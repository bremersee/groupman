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

package org.bremersee.groupman.controller;

import static org.bremersee.security.core.AuthorityConstants.USER_ROLE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.function.Consumer;
import org.bremersee.exception.model.RestApiException;
import org.bremersee.groupman.model.Group;
import org.bremersee.groupman.model.Source;
import org.bremersee.groupman.repository.GroupRepository;
import org.bremersee.test.security.authentication.WithJwtAuthenticationToken;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

/**
 * The group controller create group tests.
 *
 * @author Christian Bremer
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwk",
    "bremersee.groupman.max-owned-groups=3"
})
@TestInstance(Lifecycle.PER_CLASS) // allows us to use @BeforeAll with a non-static method
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GroupControllerCreateGroupTest {

  /**
   * The web test client.
   */
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  WebTestClient webTestClient;

  /**
   * The Group repository.
   */
  @Autowired
  GroupRepository groupRepository;

  /**
   * Create group and expect bad request.
   */
  @WithJwtAuthenticationToken(
      preferredUsername = "molly",
      roles = {USER_ROLE_NAME})
  @Test
  void createGroupAndExpectBadRequest() {
    webTestClient
        .post()
        .uri("/api/groups")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(Group.builder()
            .description("Name is missing")
            .build()))
        .exchange()
        .expectStatus().isBadRequest();
  }

  /**
   * Create group and expect no members.
   */
  @WithJwtAuthenticationToken(
      preferredUsername = "molly",
      roles = {USER_ROLE_NAME})
  @Test
  void createGroupAndExpectNoMembers() {
    webTestClient
        .post()
        .uri("/api/groups")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(Group.builder()
            .name("TestGroup1")
            .description("A test group")
            .build()))
        .exchange()
        .expectBody(Group.class)
        .value((Consumer<Group>) group -> {
          assertNotNull(group);
          assertNotNull(group.getId());
          assertEquals(0L, group.getVersion());
          assertNotNull(group.getCreatedAt());
          assertNotNull(group.getModifiedAt());
          assertEquals("molly", group.getCreatedBy());
          assertEquals(Source.INTERNAL, group.getSource());
          assertEquals("TestGroup1", group.getName());
          assertEquals("A test group", group.getDescription());
          assertTrue(group.getOwners().contains("molly"));
          assertTrue(group.getMembers().isEmpty());
          System.out.println(group);
        });
  }

  /**
   * Create group and expect with owners and members.
   */
  @WithJwtAuthenticationToken(
      preferredUsername = "molly",
      roles = {USER_ROLE_NAME})
  @Test
  void createGroupAndExpectWithOwnersAndMembers() {
    webTestClient
        .post()
        .uri("/api/groups")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(Group.builder()
            .name("TestGroup2")
            .description("Another test group")
            .owners(Arrays.asList("anna", "stephen"))
            .members(Arrays.asList("anna", "leopold", "molly"))
            .build()))
        .exchange()
        .expectBody(Group.class)
        .value((Consumer<Group>) group -> {
          assertNotNull(group);
          assertTrue(group.getOwners().containsAll(Arrays.asList("anna", "stephen", "molly")));
          assertTrue(group.getMembers().containsAll(Arrays.asList("anna", "leopold", "molly")));
        });
  }

  /**
   * Create group and expect already exists.
   */
  @WithJwtAuthenticationToken(
      preferredUsername = "leopold",
      roles = {USER_ROLE_NAME})
  @Test
  void createGroupAndExpectAlreadyExists() {
    webTestClient
        .post()
        .uri("/api/groups")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(Group.builder()
            .name("CreatorAndNameAreUnique")
            .description("This group will be persisted.")
            .build()))
        .exchange()
        .expectStatus().isOk();

    webTestClient
        .post()
        .uri("/api/groups")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(Group.builder()
            .name("CreatorAndNameAreUnique")
            .description("Persistence should fail.")
            .build()))
        .exchange()
        .expectStatus().is4xxClientError()
        .expectBody(RestApiException.class)
        .value((Consumer<RestApiException>) restApiException -> {
          assertEquals("GRP:1000", restApiException.getErrorCode()); // see application.yml
        });
  }

  /**
   * Create groups and expect max number of owned groups is reached.
   */
  @WithJwtAuthenticationToken(
      preferredUsername = "anna-livia",
      roles = {USER_ROLE_NAME})
  @Test
  void createGroupsAndExpectMaxNumberOfOwnedGroupsIsReached() {
    webTestClient
        .post()
        .uri("/api/groups")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(Group.builder()
            .name("Group1")
            .build()))
        .exchange()
        .expectStatus().isOk();

    webTestClient
        .post()
        .uri("/api/groups")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(Group.builder()
            .name("Group2")
            .build()))
        .exchange()
        .expectStatus().isOk();

    webTestClient
        .post()
        .uri("/api/groups")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(Group.builder()
            .name("Group3")
            .build()))
        .exchange()
        .expectStatus().isOk();

    webTestClient
        .post()
        .uri("/api/groups")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(Group.builder()
            .name("Group4")
            .build()))
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody(RestApiException.class)
        .value((Consumer<RestApiException>) restApiException -> {
          assertEquals("GRP:MAX_OWNED_GROUPS", restApiException.getErrorCode());
          assertEquals("/api/groups", restApiException.getPath());
        });
  }

}