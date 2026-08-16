/*
 * Copyright 2026 the original author or authors.
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

package org.bremersee.groupman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.bremersee.groupman.model.Group;
import org.junit.jupiter.api.Test;

/**
 * The group search filter test.
 *
 * @author Christian Bremer
 */
class GroupSearchFilterTest {

  /**
   * Test expect true when search is empty.
   */
  @Test
  void testExpectTrueWhenSearchIsEmpty() {
    GroupSearchFilter target = new GroupSearchFilter("");
    boolean actual = target.test(mock(Group.class));
    assertThat(actual).isTrue();
  }

  /**
   * Test expect false when group is empty.
   */
  @Test
  void testExpectFalseWhenGroupIsEmpty() {
    GroupSearchFilter target = new GroupSearchFilter("junit");
    boolean actual = target.test(null);
    assertThat(actual).isFalse();
  }

  /**
   * Test expect true when search matches.
   */
  @Test
  void testExpectTrueWhenSearchMatches() {
    GroupSearchFilter target = new GroupSearchFilter("junit");
    Group group = Group.builder()
        .id("1")
        .name("junit")
        .build();
    boolean actual = target.test(group);
    assertThat(actual).isTrue();
  }

  /**
   * Test expect false when search not matches.
   */
  @Test
  void testExpectFalseWhenSearchNotMatches() {
    GroupSearchFilter target = new GroupSearchFilter("foo");
    Group group = Group.builder()
        .id("1")
        .name("junit")
        .build();
    boolean actual = target.test(group);
    assertThat(actual).isFalse();
  }

}