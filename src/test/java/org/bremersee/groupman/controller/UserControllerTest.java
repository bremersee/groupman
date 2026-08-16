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

package org.bremersee.groupman.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.util.List;
import org.bremersee.groupman.model.User;
import org.bremersee.groupman.service.GroupService;
import org.bremersee.groupman.service.UserService;
import org.bremersee.spring.security.test.context.support.WithNormalizedUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

/**
 * The user controller test.
 *
 * @author Christian Bremer
 */
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@ActiveProfiles({"test"})
class UserControllerTest {

  @MockitoBean
  private GroupService groupService;

  @MockitoBean
  private UserService userService;

  @Autowired
  private WebTestClient webClient;

  /**
   * Find users.
   */
  @WithNormalizedUser
  @Test
  void findUsers() {
    User expected = User.builder()
        .id("u1")
        .username("junit")
        .firstName("Test")
        .lastName("User")
        .build();
    doReturn(Flux.just(expected))
        .when(userService)
        .findUsers(any(), any(), any());
    webClient
        .get()
        .uri("/api/users?search={}", "junit")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectBodyList(User.class)
        .value(users -> assertThat(users).isEqualTo(List.of(expected)));
  }

}