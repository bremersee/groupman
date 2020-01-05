package org.bremersee.groupman.repository.ldap.transcoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

/**
 * The generalized time to date value transcoder test.
 */
class GeneralizedTimeToDateValueTranscoderTest {

  private static final GeneralizedTimeToDateValueTranscoder transcoder
      = new GeneralizedTimeToDateValueTranscoder();

  private static final String ldapValue = "20191226154554.000Z";

  /**
   * Decode and encode.
   */
  @Test
  void decodeAndEncode() {
    Date date = transcoder.decodeStringValue(ldapValue);
    assertNotNull(date);
    OffsetDateTime dateTime = OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    Assert.assertEquals(Month.DECEMBER, dateTime.getMonth());
    Assert.assertEquals(26, dateTime.getDayOfMonth());
    Assert.assertEquals(15, dateTime.getHour());
    Assert.assertEquals(45, dateTime.getMinute());
    Assert.assertEquals(54, dateTime.getSecond());

    Assert.assertEquals(ldapValue, transcoder.encodeStringValue(date));
  }

  /**
   * Gets type.
   */
  @Test
  void getType() {
    assertEquals(Date.class, transcoder.getType());
  }
}