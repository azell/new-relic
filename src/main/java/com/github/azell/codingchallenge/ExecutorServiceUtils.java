package com.github.azell.codingchallenge;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public enum ExecutorServiceUtils {
  INSTANCE;

  /** Modified from the ExecutorService javadoc. */
  public void shutdown(
      ExecutorService pool, long timeout, TimeUnit unit, Consumer<ExecutorService> zombie) {
    /* Disable new tasks from being submitted */
    pool.shutdown();

    try {
      /* Wait a while for existing tasks to terminate */
      if (!pool.awaitTermination(timeout, unit)) {
        /* Cancel currently executing tasks */
        pool.shutdownNow();
        /* Wait a while for tasks to respond to being cancelled */
        if (!pool.awaitTermination(timeout, unit)) {
          zombie.accept(pool);
        }
      }
    } catch (InterruptedException e) {
      /* (Re-)Cancel if current thread also interrupted */
      pool.shutdownNow();
      /* Preserve interrupt status */
      Thread.currentThread().interrupt();
    }
  }
}
