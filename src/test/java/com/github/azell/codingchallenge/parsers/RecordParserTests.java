package com.github.azell.codingchallenge.parsers;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class RecordParserTests {
  @Test
  void shouldParseRecords() throws IOException {
    var input = new ByteArrayInputStream(asBytes("0123456789\r\n"));
    var buffer = new byte[8];
    var parser = new RecordParser();

    /* First case: incomplete record */
    parser.parse(input, buffer, asBytes("\r\n"));

    assertThat(parser.nbytes()).isEqualTo(buffer.length);
    assertThat(parser.eol()).isFalse();
    assertThat(asString(buffer, parser)).isEqualTo("01234567");

    /* Second case: complete record with delimiter */
    parser.parse(input, buffer, asBytes("\r\n"));

    assertThat(parser.nbytes()).isEqualTo(4);
    assertThat(parser.eol()).isTrue();
    assertThat(asString(buffer, parser)).isEqualTo("89\r\n");

    /* Third case: end of file */
    parser.parse(input, buffer, asBytes("\r\n"));

    assertThat(parser.nbytes()).isEqualTo(0);
    assertThat(parser.eol()).isFalse();
    assertThat(asString(buffer, parser)).isEmpty();
  }

  private byte[] asBytes(String str) {
    return str.getBytes(UTF_8);
  }

  private String asString(byte[] bytes, RecordParser parser) {
    return new String(bytes, 0, parser.nbytes(), UTF_8);
  }
}
