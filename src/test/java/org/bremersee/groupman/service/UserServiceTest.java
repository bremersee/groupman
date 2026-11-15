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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.bremersee.groupman.mapper.UserMapper;
import org.bremersee.groupman.model.User;
import org.bremersee.keycloak.api.model.UserRepresentation;
import org.bremersee.keycloak.api.webflux.KeycloakAdminClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * The user service test.
 *
 * @author Christian Bremer
 */
class UserServiceTest {

  private KeycloakAdminClient keycloakAdminClient;

  private UserService target;

  /**
   * Sets up.
   */
  @BeforeEach
  void setUp() {
    keycloakAdminClient = mock(KeycloakAdminClient.class);
    target = new UserService(keycloakAdminClient, Mappers.getMapper(UserMapper.class), "junit");
  }

  /**
   * Find users.
   */
  @Test
  void findUsers() {
    UserRepresentation representation = new UserRepresentation();
    representation.setId("1");
    representation.setUsername("junit");
    representation.setFirstName("Test");
    representation.setLastName("User");
    doReturn(Flux.just(representation))
        .when(keycloakAdminClient)
        .getUsers(eq("junit"), any());
    User expected = User.builder()
        .id("1")
        .username("junit")
        .firstName("Test")
        .lastName("User")
        .build();
    List<User> actual = target.findUsers("Test", null, null)
        .collectList().block();
    assertThat(actual).isEqualTo(List.of(expected));
  }

  /**
   * Find no users when search too short.
   */
  @Test
  void findNoUsersWhenSearchTooShort() {
    StepVerifier.create(target.findUsers("", null, null))
        .verifyComplete();
    verify(keycloakAdminClient, never()).getUsers(any(), any());
  }

}