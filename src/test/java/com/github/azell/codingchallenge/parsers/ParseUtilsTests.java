package com.github.azell.codingchallenge.parsers;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class ParserUtilsTests {
  private final ParserUtils utils = ParserUtils.INSTANCE;

  @Test
  void shouldMatchTerminateRecord() throws IOException {
    var input = new ByteArrayInputStream(ParserUtils.TERMINATE_RECORD_BYTES);
    var buffer = new byte[input.available()];
    var parser = new RecordParser();

    /* Pass a complete termination record */
    parser.parse(input, buffer, ParserUtils.DELIMITER_BYTES);

    assertThat(utils.terminateRecord(buffer, parser)).isTrue();
  }

  @Test
  void shouldMatchIntegerRecord() throws IOException {
    var input = asInputStream("012345678" + ParserUtils.DELIMITER);
    var buffer = new byte[input.available()];
    var parser = new RecordParser();

    /* Pass a complete integer record */
    parser.parse(input, buffer, ParserUtils.DELIMITER_BYTES);

    assertThat(utils.decodeIntegerRecord(buffer, parser)).isEqualTo(12345678);
  }

  @Test
  void shouldNotMatchIntegerRecord() throws IOException {
    var input = asInputStream("12345678X" + ParserUtils.DELIMITER);
    var buffer = new byte[input.available()];
    var parser = new RecordParser();

    /* Pass a complete record wth a non-integer byte */
    parser.parse(input, buffer, ParserUtils.DELIMITER_BYTES);

    assertThat(utils.decodeIntegerRecord(buffer, parser)).isEqualTo(-1);
  }

  @Test
  void shouldWriteIntegerRecord() {
    var buffer = new byte[utils.maxRecordSize()];
    var scratch = new byte[ParserUtils.INTEGER_RECORD_SEQUENCE];
    var nbytes = utils.writeIntegerRecord(buffer, 0, 7007009, scratch);

    assertThat(nbytes).isLessThanOrEqualTo(buffer.length);
    assertThat(asString(buffer, nbytes)).isEqualTo("007007009" + ParserUtils.DELIMITER);
  }

  private InputStream asInputStream(String str) {
    return new ByteArrayInputStream(str.getBytes(ParserUtils.CHARSET));
  }

  private String asString(byte[] buffer, int nbytes) {
    return new String(buffer, 0, nbytes, ParserUtils.CHARSET);
  }
}
