package com.github.azell.codingchallenge;

import com.github.azell.codingchallenge.parsers.ParserUtils;
import com.github.azell.codingchallenge.parsers.RecordParser;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketIngestor implements Closeable, AutoCloseable {
  /** Port to bind to on localhost. */
  public static final int PORT = 4000;
  /** Maximum concurrent clients. */
  public static final int MAX_AVAILABLE = 5;

  private static final Logger LOGGER =
      LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /* Used to enforce client limit */
  private final Semaphore available = new Semaphore(MAX_AVAILABLE);
  /* Thread pool management */
  private final ExecutorService pool = Executors.newFixedThreadPool(MAX_AVAILABLE);

  private final ServerSocket serverSocket;
  private final int batchSize;
  private final BlockingQueue<int[]> queue;

  public SocketIngestor(int batchSize, BlockingQueue<int[]> queue) throws IOException {
    this.serverSocket = new ServerSocket(PORT);
    this.batchSize = batchSize;
    this.queue = queue;
  }

  public void run() throws IOException, InterruptedException {
    try {
      while (!Thread.interrupted()) {
        /* block if clients are all busy */
        available.acquire();

        var client = serverSocket.accept();

        pool.execute(() -> ingest(client));
      }
    } catch (IOException e) {
      /* don't propagate the exception if it is due to termination */
      if (!serverSocket.isClosed()) {
        throw e;
      }
    }
  }

  @Override
  public void close() throws IOException {
    LOGGER.info("Closing ingestor");

    serverSocket.close();
    ExecutorServiceUtils.INSTANCE.shutdown(
        pool, 500L, TimeUnit.MILLISECONDS, p -> LOGGER.warn("Pool did not terminate"));
  }

  private void ingest(Socket client) {
    /* Batch operations */
    var batch = new int[batchSize];
    var batchIdx = 0;

    try (var src = new BufferedInputStream(client.getInputStream())) {
      var utils = ParserUtils.INSTANCE;

      /* Reuse objects as much as possible */
      var parser = new RecordParser();
      var buffer = new byte[utils.maxRecordSize()];

      while (!Thread.interrupted()) {
        parser.parse(src, buffer, ParserUtils.DELIMITER_BYTES);

        if (utils.terminateRecord(buffer, parser)) {
          terminate();
          break;
        }

        var num = utils.decodeIntegerRecord(buffer, parser);

        if (num < 0) {
          break;
        }

        batch[batchIdx++] = num;

        /* If the batch is full, send it off */
        if (batchIdx == batch.length) {
          publishBatch(batch, batchIdx);
          batchIdx = 0;
        }
      }

      /* Keep track of leftover records */
      publishBatch(batch, batchIdx);
    } catch (IOException e) {
      /* Not much we can do here, other than log */
      LOGGER.error(e.getMessage(), e);

      /* Keep track of leftover records */
      publishBatch(batch, batchIdx);
    } finally {
      /* Let another client run */
      available.release();
    }
  }

  private void publishBatch(int[] batch, int batchIdx) {
    if (batchIdx == 0) {
      return;
    }

    /* make a copy for the consumer */
    var payload = Arrays.copyOf(batch, batchIdx);

    try {
      queue.put(payload);
    } catch (InterruptedException e) {
      /* Preserve interrupt status */
      Thread.currentThread().interrupt();
    }
  }

  private void terminate() throws IOException {
    LOGGER.info("Terminating ingestor");

    /* Force the main thread out of a blocking accept call, if necessary */
    serverSocket.close();
  }
}
