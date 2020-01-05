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

package org.bremersee.groupman.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The domain controller properties test.
 *
 * @author Christian Bremer
 */
class DomainControllerPropertiesTest {

  private static DomainControllerProperties properties = new DomainControllerProperties();

  /**
   * Sets up.
   */
  @BeforeAll
  static void setUp() {
    properties.setGroupFindAllFilter("(objectClass=group)");
    properties.setGroupFindOneFilter("(&(objectClass=group)(cn={0}))");
    properties.setGroupNameAttribute("cn");
    properties.setGroupMemberAttribute("member");
  }

  /**
   * Gets group find by names filter.
   */
  @Test
  void getGroupFindByNamesFilter() {
    String expected = "(objectClass=group)";
    String actual = properties.getGroupFindByNamesFilter(0);
    assertEquals(expected, actual);

    expected = "(&(objectClass=group)(cn={0}))";
    actual = properties.getGroupFindByNamesFilter(1);
    assertEquals(expected, actual);

    expected = "(&(objectClass=group)(|(cn={0})(cn={1})))";
    actual = properties.getGroupFindByNamesFilter(2);
    assertEquals(expected, actual);
  }

  /**
   * Gets group find by member contains filter.
   */
  @Test
  void getGroupFindByMemberContainsFilter() {
    String expected = "(&(objectClass=group)(member={0}))";
    String actual = properties.getGroupFindByMemberContainsFilter();
    assertEquals(expected, actual);
  }
}