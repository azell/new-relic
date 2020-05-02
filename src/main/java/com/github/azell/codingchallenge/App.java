package com.github.azell.codingchallenge;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class App {
  public static final int BATCH_SIZE = 1000;
  public static final int QUEUE_CAPACITY = 32 * 1024;
  public static final int UPDATE_INTERVAL = 10;

  public static void main(String[] args) throws IOException, InterruptedException {
    var queue = queue();

    /* Simplified dependency injection */
    try (var ingestor = ingestor(queue);
        var dst = destination();
        var consumer = consumer(queue, dst)) {
      consumer.run();
      ingestor.run();
    }
  }

  private static RecordConsumer consumer(BlockingQueue<int[]> queue, OutputStream dst) {
    return new RecordConsumer(BATCH_SIZE, queue, dst, UPDATE_INTERVAL);
  }

  private static OutputStream destination() throws IOException {
    return new FileOutputStream("numbers.log");
  }

  private static SocketIngestor ingestor(BlockingQueue<int[]> queue) throws IOException {
    return new SocketIngestor(BATCH_SIZE, queue);
  }

  private static BlockingQueue<int[]> queue() {
    return new ArrayBlockingQueue<>(QUEUE_CAPACITY);
  }
}
