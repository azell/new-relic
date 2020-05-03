import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.BitSet;

public class Verify {
  public static void main(String[] args) throws Exception {
    var bitset = new BitSet(Integer.MAX_VALUE);

    try (var reader = reader(System.in)) {
      for (var str = reader.readLine(); str != null; str = reader.readLine()) {
        if ("terminate".equals(str)) {
          break;
        }

        bitset.set(toInt(str));
      }
    }

    try (var reader = reader(new FileInputStream(args[0]))) {
      for (var str = reader.readLine(); str != null; str = reader.readLine()) {
        bitset.flip(toInt(str));
      }
    }

    System.out.println("Difference: " + bitset);
  }

  static BufferedReader reader(InputStream inp) {
    return new BufferedReader(new InputStreamReader(inp, UTF_8));
  }

  static int toInt(String str) {
    return Integer.parseInt(str, 10);
  }
}
