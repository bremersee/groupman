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

import static org.bremersee.security.core.AuthorityConstants.ADMIN_ROLE_NAME;
import static org.bremersee.security.core.AuthorityConstants.USER_ROLE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;
import org.bremersee.exception.model.RestApiException;
import org.bremersee.groupman.model.Group;
import org.bremersee.groupman.model.Source;
import org.bremersee.groupman.repository.GroupEntity;
import org.bremersee.groupman.repository.GroupRepository;
import org.bremersee.test.security.authentication.WithJwtAuthenticationToken;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.test.StepVerifier;

/**
 * The group admin controller modify group tests.
 *
 * @author Christian Bremer
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost/jwk"
})
@TestInstance(Lifecycle.PER_CLASS) // allows us to use @BeforeAll with a non-static method
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GroupAdminControllerModifyGroupTest {

  private static final GroupEntity group0 = GroupEntity.builder()
      .id("GACMGT0")
      .source(Source.INTERNAL)
      .name("Group0")
      .description("Group One")
      .createdAt(new Date())
      .modifiedAt(new Date())
      .createdBy("someone")
      .build();

  private static final GroupEntity group1 = GroupEntity.builder()
      .id("GACMGT1")
      .source(Source.INTERNAL)
      .name("Group1")
      .description("Group Two")
      .createdAt(new Date())
      .modifiedAt(new Date())
      .createdBy("someone else")
      .build();

  /**
   * The application context.
   */
  @Autowired
  ApplicationContext context;

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
   * Setup tests.
   */
  @BeforeAll
  void setUp() {
    // https://docs.spring.io/spring-security/site/docs/current/reference/html/test-webflux.html
    WebTestClient
        .bindToApplicationContext(this.context)
        .configureClient()
        .build();
  }

  /**
   * Sets up data.
   */
  @BeforeAll
  void setUpData() {
    StepVerifier
        .create(groupRepository.deleteAll())
        .expectNextCount(0L)
        .verifyComplete();
    StepVerifier
        .create(groupRepository.save(group0))
        .consumeNextWith(groupEntity -> {
          assertNotNull(groupEntity.getId());
          assertEquals("GACMGT0", groupEntity.getId());
        })
        .verifyComplete();
    StepVerifier
        .create(groupRepository.save(group1))
        .consumeNextWith(groupEntity -> {
          assertNotNull(groupEntity.getId());
          assertEquals("GACMGT1", groupEntity.getId());
        })
        .verifyComplete();
  }

  /**
   * Modify group and expect forbidden.
   */
  @WithJwtAuthenticationToken(roles = {USER_ROLE_NAME})
  @Test
  @Order(10)
  void modifyGroupAndExpectForbidden() {
    webTestClient
        .put()
        .uri("/api/admin/groups/{id}", "GACMGT0")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(Group.builder()
            .name("group")
            .build()))
        .exchange()
        .expectStatus().isForbidden();
  }

  /**
   * Modify group and expect not found.
   */
  @WithJwtAuthenticationToken(roles = {ADMIN_ROLE_NAME})
  @Test
  @Order(11)
  void modifyGroupAndExpectNotFound() {
    webTestClient
        .put()
        .uri("/api/admin/groups/{id}", UUID.randomUUID().toString())
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(Group.builder()
            .name("group")
            .build()))
        .exchange()
        .expectStatus().isNotFound();
  }

  /**
   * Modify group and expect bad request.
   */
  @WithJwtAuthenticationToken(roles = {ADMIN_ROLE_NAME})
  @Test
  @Order(50)
  void modifyGroupAndExpectBadRequest() {
    webTestClient
        .put()
        .uri("/api/admin/groups/{id}", "GACMGT1")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(Group.builder()
            .build()))
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody(RestApiException.class)
        .value((Consumer<RestApiException>) restApiException -> {
          assertNotNull(restApiException.getMessage());
          assertEquals("/api/admin/groups/GACMGT1", restApiException.getPath());
          // System.out.println("RestApiException of Validation: " + restApiException);
        });

    webTestClient
        .put()
        .uri("/api/admin/groups/{id}", "GACMGT1")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(Group.builder()
            .name("group")
            .source(Source.LDAP)
            .build()))
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody(RestApiException.class)
        .value((Consumer<RestApiException>) restApiException -> {
          assertEquals("GRP:1002", restApiException.getErrorCode()); // see application.yml
        });
  }

  /**
   * Modify group.
   */
  @WithJwtAuthenticationToken(roles = {ADMIN_ROLE_NAME})
  @Test
  @Order(100)
  void modifyGroup() {
    webTestClient
        .put()
        .uri("/api/admin/groups/{id}", "GACMGT0")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(Group.builder()
            .createdBy("molly")
            .name("Modified")
            .description("A modified group.")
            .owners(Arrays.asList("anna", "stephen"))
            .members(Arrays.asList("anna", "leopold", "molly"))
            .build()))
        .exchange()
        .expectBody(Group.class)
        .value((Consumer<Group>) group -> {
          assertNotNull(group);
          assertEquals("molly", group.getCreatedBy());
          assertEquals("Modified", group.getName());
          assertEquals("A modified group.", group.getDescription());
          assertTrue(group.getOwners().containsAll(Arrays.asList("anna", "stephen")));
          assertTrue(group.getMembers().containsAll(Arrays.asList("anna", "leopold", "molly")));
        });
  }

  /**
   * Delete group.
   */
  @WithJwtAuthenticationToken(roles = {ADMIN_ROLE_NAME})
  @Test
  @Order(110)
  void deleteGroup() {
    webTestClient
        .delete()
        .uri("/api/admin/groups/{id}", "GACMGT0")
        .exchange()
        .expectStatus().isOk();

    webTestClient
        .get()
        .uri("/api/admin/groups/{id}", "GACMGT0")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isNotFound();
  }

}