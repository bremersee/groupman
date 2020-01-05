package org.bremersee.groupman.repository.ldap.transcoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bremersee.groupman.config.DomainControllerProperties;
import org.junit.jupiter.api.Test;

/**
 * The type Group member value transcoder test.
 */
class GroupMemberValueTranscoderTest {

  /**
   * Decode string value.
   */
  @Test
  void decodeStringValue() {
    DomainControllerProperties properties = new DomainControllerProperties();
    properties.setUserBaseDn("ou=people,dc=example,dc=org");
    properties.setUserRdn("uid");
    properties.setMemberDn(true);
    GroupMemberValueTranscoder transcoder = new GroupMemberValueTranscoder(properties);
    String actual = transcoder.decodeStringValue("uid=molly,ou=people,dc=example,dc=org");
    assertEquals("molly", actual);

    properties.setMemberDn(false);
    transcoder = new GroupMemberValueTranscoder(properties);
    assertEquals("molly", transcoder.decodeStringValue("molly"));
  }

  /**
   * Encode string value.
   */
  @Test
  void encodeStringValue() {
    DomainControllerProperties properties = new DomainControllerProperties();
    properties.setUserBaseDn("ou=people,dc=example,dc=org");
    properties.setUserRdn("uid");
    properties.setMemberDn(true);
    GroupMemberValueTranscoder transcoder = new GroupMemberValueTranscoder(properties);
    String actual = transcoder.encodeStringValue("molly");
    assertEquals("uid=molly,ou=people,dc=example,dc=org", actual);

    properties.setMemberDn(false);
    transcoder = new GroupMemberValueTranscoder(properties);
    assertEquals("molly", transcoder.encodeStringValue("molly"));
  }

  /**
   * Gets type.
   */
  @Test
  void getType() {
    DomainControllerProperties properties = new DomainControllerProperties();
    GroupMemberValueTranscoder transcoder = new GroupMemberValueTranscoder(properties);
    assertEquals(String.class, transcoder.getType());
  }
}