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
import org.bremersee.groupman.model.Group;
import org.bremersee.groupman.model.GroupCreate;
import org.bremersee.groupman.model.GroupUpdate;
import org.bremersee.groupman.model.User;
import org.bremersee.groupman.service.GroupService;
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
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The group controller test.
 *
 * @author Christian Bremer
 */
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@ActiveProfiles({"test"})
class GroupControllerTest {

  @MockitoBean
  private GroupService groupService;

  @Autowired
  private WebTestClient webClient;

  /**
   * Create group.
   */
  @WithNormalizedUser
  @Test
  void createGroup() {
    GroupCreate groupCreate = GroupCreate.builder()
        .name("Test Group")
        .description("Junit test group")
        .build();
    Group expected = Group.builder()
        .from(groupCreate)
        .id("1")
        .build();
    doReturn(Mono.just(expected))
        .when(groupService)
        .createGroup(any(), any());
    webClient
        .post()
        .uri("/api/groups")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(groupCreate))
        .exchange()
        .expectStatus().isOk()
        .expectBody(Group.class)
        .value(group -> assertThat(group).isEqualTo(expected));
  }

  /**
   * Gets groups.
   */
  @WithNormalizedUser
  @Test
  void getGroups() {
    Group expected = Group.builder()
        .name("Test Group")
        .description("Junit test group")
        .id("1")
        .build();
    doReturn(Flux.just(expected))
        .when(groupService)
        .getGroups(any(), any(), any(), any());
    webClient
        .get()
        .uri("/api/groups")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectBodyList(Group.class)
        .value(groups -> assertThat(groups).isEqualTo(List.of(expected)));
  }

  /**
   * Gets group.
   */
  @WithNormalizedUser
  @Test
  void getGroup() {
    Group expected = Group.builder()
        .name("Test Group")
        .description("Junit test group")
        .id("1")
        .build();
    doReturn(Mono.just(expected))
        .when(groupService)
        .getGroup(any(), any());
    webClient
        .get()
        .uri("/api/groups/{id}", "1")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectBody(Group.class)
        .value(group -> assertThat(group).isEqualTo(expected));
  }

  /**
   * Gets members.
   */
  @WithNormalizedUser
  @Test
  void getMembers() {
    User expected = User.builder()
        .id("u1")
        .username("junit")
        .firstName("Test")
        .lastName("User")
        .build();
    doReturn(Flux.just(expected))
        .when(groupService)
        .getMembers(any(), any(), any(), any());
    webClient
        .get()
        .uri("/api/groups/{id}/members", "1")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectBodyList(User.class)
        .value(users -> assertThat(users).isEqualTo(List.of(expected)));
  }

  /**
   * Add member.
   */
  @WithNormalizedUser
  @Test
  void addMember() {
    doReturn(Mono.empty())
        .when(groupService)
        .addMember(any(), any(), any());
    webClient
        .put()
        .uri("/api/groups/{groupId}/members/{userId}", "1", "u1")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk();
  }

  /**
   * Remove member.
   */
  @WithNormalizedUser
  @Test
  void removeMember() {
    doReturn(Mono.empty())
        .when(groupService)
        .removeMember(any(), any(), any());
    webClient
        .delete()
        .uri("/api/groups/{groupId}/members/{userId}", "1", "u1")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk();
  }

  /**
   * Update group.
   */
  @WithNormalizedUser
  @Test
  void updateGroup() {
    GroupUpdate groupUpdate = GroupUpdate.builder()
        .name("Test Group")
        .description("Junit test group")
        .build();
    Group expected = Group.builder()
        .from(groupUpdate)
        .id("1")
        .build();
    doReturn(Mono.just(expected))
        .when(groupService)
        .updateGroup(any(), any(), any());
    webClient
        .put()
        .uri("/api/groups/{id}", "1")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(groupUpdate))
        .exchange()
        .expectStatus().isOk()
        .expectBody(Group.class)
        .value(group -> assertThat(group).isEqualTo(expected));
  }

  /**
   * Delete group.
   */
  @WithNormalizedUser
  @Test
  void deleteGroup() {
    doReturn(Mono.empty())
        .when(groupService)
        .deleteGroup(any(), any());
    webClient
        .delete()
        .uri("/api/groups/{groupId}", "1")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk();
  }
}