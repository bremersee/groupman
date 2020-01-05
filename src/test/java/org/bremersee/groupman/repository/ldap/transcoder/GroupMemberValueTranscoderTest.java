/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bremersee.groupman.repository.ldap.transcoder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bremersee.groupman.config.DomainControllerProperties;
import org.junit.jupiter.api.Test;

/**
 * The type Group member value transcoder test.
 *
 * @author Christian Bremer
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