package com.github.azell.codingchallenge.parsers;

import java.io.IOException;
import java.io.InputStream;

public class RecordParser {
  private int nbytes;
  private boolean eol;

  /** The number of bytes parsed. */
  public int nbytes() {
    return nbytes;
  }

  /** Whether the record delimiter was found. */
  public boolean eol() {
    return eol;
  }

  /**
   * Read the next record into the given buffer until the buffer is full, or the record delimiter is
   * found.
   */
  public void parse(InputStream src, byte[] buffer, byte[] delimiter) throws IOException {
    reset();

    /* index into the buffer */
    var idx = 0;
    /* index into the delimiter */
    var delimIdx = 0;

    while (idx < buffer.length) {
      var curr = src.read();

      /* check for EOF */
      if (curr < 0) {
        break;
      }

      var b = (byte) curr;

      buffer[idx++] = b;

      /* check if we have matched the record delimiter */
      if (delimiter[delimIdx] != b) {
        delimIdx = (delimiter[0] != b) ? 0 : 1;
      } else if (++delimIdx == delimiter.length) {
        this.eol = true;
        break;
      }
    }

    this.nbytes = idx;
  }

  void reset() {
    nbytes = 0;
    eol = false;
  }
}
