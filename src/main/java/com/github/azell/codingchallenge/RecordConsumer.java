package com.github.azell.codingchallenge;

import com.github.azell.codingchallenge.parsers.ParserUtils;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RecordConsumer implements Closeable, AutoCloseable {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /* Thread pool management */
  private final ScheduledExecutorService pool = Executors.newScheduledThreadPool(2);
  /* Lock to protect shared state */
  private final Lock lock = new ReentrantLock();
  /* Shared statistics data */
  private final RecordStats sharedStats = new RecordStats();
  /* Short-hand for singleton */
  private final ParserUtils utils = ParserUtils.INSTANCE;

  private final int batchSize;
  private final BlockingQueue<int[]> queue;
  private final OutputStream dst;
  private final int interval;

  public RecordConsumer(int batchSize, BlockingQueue<int[]> queue, OutputStream dst, int interval) {
    this.batchSize = batchSize;
    this.queue = queue;
    this.dst = dst;
    this.interval = interval;
  }

  public void run() {
    pool.submit(this::consumer);
    pool.scheduleWithFixedDelay(this::updater, interval, interval, TimeUnit.SECONDS);
  }

  @Override
  public void close() {
    LOGGER.info("Closing consumer");

    /* We did not open the destination stream, so we will not close it. */
    ExecutorServiceUtils.INSTANCE.shutdown(
        pool, 500L, TimeUnit.MILLISECONDS, p -> LOGGER.warn("Pool did not terminate"));
  }

  private void updater() {
    var prev = new RecordStats();

    /* Grab the old data, and start a new time window */
    lock.lock();
    try {
      sharedStats.startTimeWindow(prev);
    } finally {
      lock.unlock();
    }

    System.out.println(
        String.format(
            Locale.US,
            "Received %d unique numbers, %d duplicates. Unique total: %d",
            prev.uniques(),
            prev.duplicates(),
            prev.totalUniques()));
  }

  private void consumer() {
    /* Batch operations */
    var batch = new ArrayList<int[]>(batchSize);
    /* Reusable buffers */
    var buffer = new byte[1024 * 1024];
    var scratch = new byte[ParserUtils.INTEGER_RECORD_SEQUENCE];
    /* Total uniques */
    var bitset = new BitSet(Integer.MAX_VALUE);

    try {
      while (!Thread.interrupted()) {
        /* buffer state */
        var bufferIdx = 0;
        var bufferPadding = utils.maxRecordSize();

        /* time window state */
        var uniques = 0;
        var duplicates = 0;

        drainQueue(batch);

        /* Walk the list of arrays */
        for (var payload : batch) {
          for (var num : payload) {
            if (bitset.get(num)) {
              ++duplicates;
            } else {
              ++uniques;
              bitset.set(num);

              bufferIdx += utils.writeIntegerRecord(buffer, bufferIdx, num, scratch);

              /* Flush the buffer if not enough space is left for another record */
              if ((buffer.length - bufferIdx) < bufferPadding) {
                flushBuffer(buffer, bufferIdx);
                bufferIdx = 0;
              }
            }
          }
        }

        /* Keep track of leftover records */
        flushBuffer(buffer, bufferIdx);

        updateTimeWindow(uniques, duplicates);
      }
    } catch (IOException e) {
      /* Not much we can do here, other than log */
      LOGGER.error(e.getMessage(), e);
    }
  }

  private void drainQueue(Collection<int[]> c) {
    c.clear();

    try {
      if (queue.drainTo(c, batchSize) == 0) {
        c.add(queue.take());
      }
    } catch (InterruptedException e) {
      /* Preserve interrupt status */
      Thread.currentThread().interrupt();
    }
  }

  private void updateTimeWindow(long uniques, long duplicates) {
    lock.lock();
    try {
      sharedStats.updateTimeWindow(uniques, duplicates);
    } finally {
      lock.unlock();
    }
  }

  private void flushBuffer(byte[] buffer, int bufferIdx) throws IOException {
    dst.write(buffer, 0, bufferIdx);
  }
}
