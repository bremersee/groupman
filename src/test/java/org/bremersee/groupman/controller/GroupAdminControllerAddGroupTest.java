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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.bremersee.exception.model.RestApiException;
import org.bremersee.groupman.model.Group;
import org.bremersee.groupman.model.Source;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

/**
 * The group admin controller add group tests.
 *
 * @author Christian Bremer
 */
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@AutoConfigureDataMongo
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GroupAdminControllerAddGroupTest {

  /**
   * The web test client.
   */
  @Autowired
  WebTestClient webTestClient;

  /**
   * Add group and expect forbidden.
   */
  @WithMockUser(username = "junit", authorities = {"ROLE_USER"})
  @Test
  void addGroupAndExpectForbidden() {
    webTestClient
        .post()
        .uri("/api/admin/groups")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(Group.builder()
            .name("TestGroup1")
            .description("A test group")
            .build()))
        .exchange()
        .expectStatus().isForbidden();
  }

  /**
   * Add group and expect bad request.
   */
  @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
  @Test
  void addGroupAndExpectBadRequest() {
    webTestClient
        .post()
        .uri("/api/admin/groups")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(Group.builder()
            .description("Name is missing")
            .build()))
        .exchange()
        .expectStatus().isBadRequest();
  }

  /**
   * Add group with source ldap and expect bad request.
   */
  @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
  @Test
  void addGroupWithSourceLdapAndExpectBadRequest() {
    webTestClient
        .post()
        .uri("/api/admin/groups")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(Group.builder()
            .name("LdapGroup")
            .description("A ldap group")
            .source(Source.LDAP)
            .build()))
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody(RestApiException.class)
        .value(restApiException -> {
          assertEquals("GRP:1002", restApiException.getErrorCode()); // see application.yml
        });
  }

  /**
   * Add group and expect no owners and members.
   */
  @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
  @Test
  void addGroupAndExpectNoOwnersAndMembers() {
    webTestClient
        .post()
        .uri("/api/admin/groups")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(Group.builder()
            .name("TestGroup1")
            .description("A test group")
            .build()))
        .exchange()
        .expectBody(Group.class)
        .value(group -> {
          assertNotNull(group);
          assertNotNull(group.getId());
          assertEquals(0L, group.getVersion());
          assertNotNull(group.getCreatedAt());
          assertNotNull(group.getModifiedAt());
          assertEquals("admin", group.getCreatedBy());
          assertEquals(Source.INTERNAL, group.getSource());
          assertEquals("TestGroup1", group.getName());
          assertEquals("A test group", group.getDescription());
          assertTrue(group.getOwners().isEmpty());
          assertTrue(group.getMembers().isEmpty());
        });
  }

  /**
   * Add group and expect with owners and members.
   */
  @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
  @Test
  void addGroupAndExpectWithOwnersAndMembers() {
    webTestClient
        .post()
        .uri("/api/admin/groups")
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
        .value(group -> {
          assertNotNull(group);
          assertTrue(group.getOwners().containsAll(Arrays.asList("anna", "stephen")));
          assertTrue(group.getMembers().containsAll(Arrays.asList("anna", "leopold", "molly")));
        });
  }

  /**
   * Add group and expect already exists.
   */
  @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
  @Test
  void addGroupAndExpectAlreadyExists() {
    webTestClient
        .post()
        .uri("/api/admin/groups")
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
        .uri("/api/admin/groups")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(Group.builder()
            .name("CreatorAndNameAreUnique")
            .description("Persistence should fail.")
            .build()))
        .exchange()
        .expectStatus().is4xxClientError()
        .expectBody(RestApiException.class)
        .value(restApiException -> {
          assertEquals("GRP:1000", restApiException.getErrorCode()); // see application.yml
        });
  }

}