/*
 * Copyright 2024 the original author or authors.
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

package org.bremersee.groupman.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * The user context test.
 *
 * @author Christan Bremer
 */
class UserContextTest {

  /**
   * Gets user id.
   */
  @Test
  @SuppressWarnings({"SimplifiableJUnitAssertion", "ConstantConditions"})
  void getName() {
    UserContext actual = UserContext.newInstance();
    assertNull(actual.getName());
    assertFalse(actual.isNamePresent());

    UserContext expected = UserContext.newInstance();
    assertEquals(expected.hashCode(), actual.hashCode());
    assertTrue(actual.equals(expected));
    assertFalse(actual.equals(null));
    assertFalse(actual.equals(new Object()));

    String username = UUID.randomUUID().toString();
    actual = UserContext.newInstance(username, null, null);
    assertTrue(actual.isNamePresent());
    assertEquals(username, actual.getName());
    assertEquals(username, actual.getName());

    expected = UserContext.newInstance(username, null, null);
    assertEquals(expected.hashCode(), actual.hashCode());
    assertTrue(actual.equals(expected));
    assertFalse(actual.equals(null));
    assertFalse(actual.equals(new Object()));

    assertTrue(actual.toString().contains(username));
  }

  /**
   * Gets roles.
   */
  @Test
  @SuppressWarnings({"SimplifiableJUnitAssertion", "ConstantConditions"})
  void getRoles() {
    String role = UUID.randomUUID().toString();

    UserContext actual = UserContext.newInstance();
    assertNotNull(actual.getRoles());
    assertFalse(actual.hasRole(role));
    assertFalse(actual.hasAnyRole(role));
    assertFalse(actual.hasAnyRole(Arrays.asList(role, "123")));

    String username = UUID.randomUUID().toString();
    actual = UserContext.newInstance(username, Collections.singleton(role), null);
    assertTrue(actual.getRoles().contains(role));
    assertTrue(actual.hasRole(role));
    assertTrue(actual.hasAnyRole(role));
    assertTrue(actual.hasAnyRole(Arrays.asList(role, "123")));

    UserContext expected = UserContext.newInstance(username, Collections.singleton(role), null);
    assertEquals(expected.hashCode(), actual.hashCode());
    assertTrue(actual.equals(expected));
    assertFalse(actual.equals(null));
    assertFalse(actual.equals(new Object()));

    assertTrue(actual.toString().contains(role));
  }

  /**
   * Gets groups.
   */
  @Test
  @SuppressWarnings({"SimplifiableJUnitAssertion", "ConstantConditions"})
  void getGroups() {
    String group = UUID.randomUUID().toString();

    UserContext actual = UserContext.newInstance();
    assertNotNull(actual.getGroups());
    assertFalse(actual.isInGroup(group));
    assertFalse(actual.isInAnyGroup(group));
    assertFalse(actual.isInAnyGroup(Arrays.asList(group, "123")));

    String username = UUID.randomUUID().toString();
    actual = UserContext.newInstance(username, null, Collections.singleton(group));
    assertTrue(actual.getGroups().contains(group));
    assertTrue(actual.isInGroup(group));
    assertTrue(actual.isInAnyGroup(group));
    assertTrue(actual.isInAnyGroup(Arrays.asList(group, "123")));

    UserContext expected = UserContext.newInstance(username, null, Collections.singleton(group));
    assertEquals(expected.hashCode(), actual.hashCode());
    assertTrue(actual.equals(expected));
    assertFalse(actual.equals(null));
    assertFalse(actual.equals(new Object()));

    assertTrue(actual.toString().contains(group));
  }

  /**
   * With authentication.
   */
  @Test
  void withAuthentication() {
    String username = UUID.randomUUID().toString();
    String role = UUID.randomUUID().toString();
    Collection<GrantedAuthority> roles = Collections.singleton(new SimpleGrantedAuthority(role));
    Authentication authentication = mock(Authentication.class);
    when(authentication.getName()).thenReturn(username);
    when(authentication.getAuthorities()).then(invocation -> roles);

    String group = UUID.randomUUID().toString();
    UserContext actual = UserContext.newInstance(authentication, Collections.singleton(group));
    assertTrue(actual.isNamePresent());
    assertEquals(username, actual.getName());
    assertTrue(actual.getRoles().contains(role));
    assertTrue(actual.hasRole(role));
    assertTrue(actual.hasAnyRole(role));
    assertTrue(actual.hasAnyRole(Arrays.asList("456", role)));
    assertTrue(actual.getGroups().contains(group));
    assertTrue(actual.isInGroup(group));
    assertTrue(actual.isInAnyGroup(group));
    assertTrue(actual.isInAnyGroup(Arrays.asList("456", group)));
  }

  /**
   * With lambda.
   */
  @Test
  void withLambda() {
    final String username = UUID.randomUUID().toString();
    UserContext userContext = () -> username;
    assertEquals(username, userContext.getName());
    assertTrue(userContext.getRoles().isEmpty());
    assertTrue(userContext.getGroups().isEmpty());
  }

}