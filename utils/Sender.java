import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Sender {
  private static final int NTHREADS = 6;

  private final ExecutorService pool = Executors.newFixedThreadPool(NTHREADS);

  public static void main(String[] args) throws Exception {
    var sender = new Sender();

    sender.run(args[0]);
    sender.shutdownAndAwaitTermination();
  }

  private Void send(String fname) throws IOException {
    var n = 0;
    var ms = System.currentTimeMillis();

    try (var reader = reader(new FileInputStream(fname));
        var sock = new Socket("localhost", 4000);
        var output = sock.getOutputStream()) {

      for (var str = reader.readLine(); str != null; str = reader.readLine()) {
        output.write(toMessage(str));
        ++n;
      }

      return null;
    } finally {
      ms = System.currentTimeMillis() - ms;

      System.out.println(n + " messages sent in: " + ms);
    }
  }

  private Void terminate() throws IOException {
    try (var sock = new Socket("localhost", 4000);
        var output = sock.getOutputStream()) {
      output.write(toMessage("terminate"));
    }

    return null;
  }

  private byte[] toMessage(String str) {
    return (str + System.lineSeparator()).getBytes(UTF_8);
  }

  private void run(String str) throws InterruptedException {
    var list = new ArrayList<Callable<Void>>(NTHREADS);

    for (var i = 1; i <= NTHREADS; ++i) {
      var fname = String.format(Locale.US, "%s%d.txt", str, i);

      list.add(() -> send(fname));
    }

    pool.invokeAll(list);
    pool.invokeAll(List.of(this::terminate));
  }

  private void shutdownAndAwaitTermination() {
    pool.shutdown(); // Disable new tasks from being submitted

    try {
      // Wait a while for existing tasks to terminate
      if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
        pool.shutdownNow(); // Cancel currently executing tasks

        // Wait a while for tasks to respond to being cancelled
        if (!pool.awaitTermination(1, TimeUnit.SECONDS)) {
          System.err.println("Pool did not terminate");
        }
      }
    } catch (InterruptedException ie) {
      // (Re-)Cancel if current thread also interrupted
      pool.shutdownNow();

      // Preserve interrupt status
      Thread.currentThread().interrupt();
    }
  }

  private BufferedReader reader(InputStream inp) {
    return new BufferedReader(new InputStreamReader(inp, UTF_8));
  }
}
