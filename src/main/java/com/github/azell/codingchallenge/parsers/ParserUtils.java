package com.github.azell.codingchallenge.parsers;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.charset.Charset;
import java.util.Arrays;

/** Singleton for immutable data and stateless functions. */
public enum ParserUtils {
  INSTANCE;

  public static final Charset CHARSET = UTF_8;
  public static final int INTEGER_RECORD_SEQUENCE = 9;

  /* Sadly, arrays are mutable. Trust callers not to modify them. */
  public static final String DELIMITER = System.lineSeparator();
  public static final byte[] DELIMITER_BYTES = DELIMITER.getBytes(CHARSET);

  public static final String TERMINATE_RECORD = "terminate" + DELIMITER;
  public static final byte[] TERMINATE_RECORD_BYTES = TERMINATE_RECORD.getBytes(CHARSET);

  /** Check if the complete record is the special terminate record. */
  public boolean terminateRecord(byte[] buffer, RecordParser parser) {
    var len = parser.nbytes();

    return parser.eol()
        && len == TERMINATE_RECORD_BYTES.length
        && Arrays.equals(buffer, 0, len, TERMINATE_RECORD_BYTES, 0, len);
  }

  /** Decode a complete integer record, or -1 on failure. */
  public int decodeIntegerRecord(byte[] buffer, RecordParser parser) {
    var num = -1;

    if (parser.eol() && parser.nbytes() == INTEGER_RECORD_SEQUENCE + DELIMITER_BYTES.length) {
      /* index into the buffer */
      var idx = 0;
      /* running value of the record */
      var curr = 0;

      while (idx < INTEGER_RECORD_SEQUENCE) {
        var b = buffer[idx++];

        if (b >= '0' && b <= '9') {
          curr = (curr * 10) + (b - '0');
        } else {
          curr = -1;
          break;
        }
      }

      num = curr;
    }

    return num;
  }

  /** Serialize a complete integer record, returning the number of bytes written to the buffer. */
  public int writeIntegerRecord(byte[] buffer, int bufferIdx, int num, byte[] scratch) {
    /* reset the scratch area */
    Arrays.fill(scratch, 0, INTEGER_RECORD_SEQUENCE, (byte) '0');

    /* reverse index into the scratch area */
    var idx = INTEGER_RECORD_SEQUENCE - 1;

    for (var curr = num; curr > 0; curr /= 10) {
      scratch[idx--] = (byte) ('0' + (curr % 10));
    }

    System.arraycopy(scratch, 0, buffer, bufferIdx, INTEGER_RECORD_SEQUENCE);
    System.arraycopy(
        DELIMITER_BYTES, 0, buffer, bufferIdx + INTEGER_RECORD_SEQUENCE, DELIMITER_BYTES.length);

    return INTEGER_RECORD_SEQUENCE + DELIMITER_BYTES.length;
  }

  /** Calculate the maximum size of a complete record, in bytes. */
  public int maxRecordSize() {
    return Math.max(
        INTEGER_RECORD_SEQUENCE + DELIMITER_BYTES.length, TERMINATE_RECORD_BYTES.length);
  }
}
