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

import static org.bremersee.security.core.AuthorityConstants.LOCAL_USER_ROLE_NAME;
import static org.bremersee.security.core.AuthorityConstants.USER_ROLE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bremersee.groupman.model.Group;
import org.bremersee.groupman.model.Source;
import org.bremersee.groupman.repository.GroupEntity;
import org.bremersee.groupman.repository.GroupRepository;
import org.bremersee.test.security.authentication.WithJwtAuthenticationToken;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

/**
 * The group controller get groups test.
 *
 * @author Christian Bremer
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "bremersee.security.authentication.enable-jwt-support=true",
    "spring.ldap.embedded.base-dn=dc=bremersee,dc=org",
    "spring.ldap.embedded.credential.username=uid=admin",
    "spring.ldap.embedded.credential.password=secret",
    "spring.ldap.embedded.ldif=classpath:schema.ldif",
    "spring.ldap.embedded.port=12391",
    "spring.ldap.embedded.validation.enabled=false",
    "bremersee.ldaptive.enabled=true",
    "bremersee.ldaptive.use-unbound-id-provider=true",
    "bremersee.ldaptive.ldap-url=ldap://localhost:12391",
    "bremersee.ldaptive.use-ssl=false",
    "bremersee.ldaptive.use-start-tls=false",
    "bremersee.ldaptive.bind-dn=uid=admin",
    "bremersee.ldaptive.bind-credential=secret",
    "bremersee.ldaptive.pooled=false",
    "bremersee.domain-controller.group-base-dn=ou=groups,dc=bremersee,dc=org",
    "bremersee.domain-controller.group-member-attribute=uniqueMember",
    "bremersee.domain-controller.member-dn=true",
    "bremersee.domain-controller.user-base-dn=ou=people,dc=bremersee,dc=org",
    "bremersee.domain-controller.user-rdn=uid",
    "bremersee.domain-controller.group-find-all-filter=(objectClass=groupOfUniqueNames)",
    "bremersee.domain-controller.group-find-one-filter=(&(objectClass=groupOfUniqueNames)(cn={0}))"
})
@ActiveProfiles({"default", "ldap"})
@TestInstance(Lifecycle.PER_CLASS) // allows us to use @BeforeAll with a non-static method
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GroupControllerGetGroupsTest {

  private static final GroupEntity group0 = GroupEntity.builder()
      .id("GCGGT0")
      .source(Source.INTERNAL)
      .name("Group0")
      .description("Group One")
      .createdAt(new Date())
      .modifiedAt(new Date())
      .createdBy("molly")
      .owners(Collections.singleton("molly"))
      .members(Stream.of("molly", "leopold").collect(Collectors.toSet()))
      .build();

  private static final GroupEntity group1 = GroupEntity.builder()
      .id("GCGGT1")
      .source(Source.INTERNAL)
      .name("Group1")
      .description("Group Two")
      .createdAt(new Date())
      .modifiedAt(new Date())
      .createdBy("molly")
      .owners(Collections.singleton("molly"))
      .build();

  private static final GroupEntity group2 = GroupEntity.builder()
      .id("GCGGT2")
      .source(Source.INTERNAL)
      .name("Group2")
      .description("Group Three")
      .createdAt(new Date())
      .modifiedAt(new Date())
      .createdBy("leopold")
      .owners(Stream.of("leopold").collect(Collectors.toSet()))
      .members(Stream.of("molly", "leopold", "stephen").collect(Collectors.toSet()))
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
        .create(groupRepository.save(group0))
        .consumeNextWith(groupEntity -> {
          assertNotNull(groupEntity.getId());
          assertEquals("GCGGT0", groupEntity.getId());
        })
        .verifyComplete();
    StepVerifier
        .create(groupRepository.save(group1))
        .consumeNextWith(groupEntity -> {
          assertNotNull(groupEntity.getId());
          assertEquals("GCGGT1", groupEntity.getId());
        })
        .verifyComplete();
    StepVerifier
        .create(groupRepository.save(group2))
        .consumeNextWith(groupEntity -> {
          assertNotNull(groupEntity.getId());
          assertEquals("GCGGT2", groupEntity.getId());
        })
        .verifyComplete();
  }

  /**
   * Gets group by id and expect ok.
   */
  @WithJwtAuthenticationToken(
      preferredUsername = "molly",
      roles = {USER_ROLE_NAME})
  @Test
  void getGroupByIdAndExpectOk() {
    webTestClient
        .get()
        .uri("/api/groups/{id}", "GCGGT0")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectBody(Group.class)
        .value((Consumer<Group>) group -> {
          assertEquals(group0.getName(), group.getName());
          assertEquals(group0.getDescription(), group.getDescription());
        });
  }

  /**
   * Gets group by id and expect not found.
   */
  @WithJwtAuthenticationToken(
      preferredUsername = "molly",
      roles = {USER_ROLE_NAME})
  @Test
  void getGroupByIdAndExpectNotFound() {
    webTestClient
        .get()
        .uri("/api/groups/{id}", UUID.randomUUID().toString())
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isNotFound();
  }

  /**
   * Gets group by ids and expect ok.
   */
  @WithJwtAuthenticationToken(
      preferredUsername = "molly",
      roles = {USER_ROLE_NAME})
  @Test
  void getGroupByIdsAndExpectOk() {
    webTestClient
        .get()
        .uri("/api/groups/f?id=GCGGT0&id=GCGGT1")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectBodyList(Group.class)
        .value(groups -> {
          assertTrue(groups.stream().anyMatch(group -> group.getName().equals(group0.getName())));
          assertTrue(groups.stream().anyMatch(group -> group.getName().equals(group1.getName())));
        });
  }

  /**
   * Gets editable groups and expect ok.
   */
  @WithJwtAuthenticationToken(
      preferredUsername = "molly",
      roles = {USER_ROLE_NAME})
  @Test
  void getEditableGroupsAndExpectOk() {
    webTestClient
        .get()
        .uri("/api/groups/f/editable")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectBodyList(Group.class)
        .value(groups -> {
          assertTrue(groups.stream().anyMatch(group -> group.getName().equals(group0.getName())));
          assertTrue(groups.stream().anyMatch(group -> group.getName().equals(group1.getName())));
          assertFalse(groups.stream().anyMatch(group -> group.getName().equals(group2.getName())));
        });
  }

  /**
   * Gets usable groups and expect ok.
   */
  @WithJwtAuthenticationToken(
      preferredUsername = "molly",
      roles = {USER_ROLE_NAME})
  @Test
  void getUsableGroupsAndExpectOk() {
    webTestClient
        .get()
        .uri("/api/groups/f/usable")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectBodyList(Group.class)
        .value(groups -> {
          assertTrue(groups.stream().anyMatch(group -> group.getName().equals(group0.getName())));
          assertTrue(groups.stream().anyMatch(group -> group.getName().equals(group1.getName())));
          assertTrue(groups.stream().anyMatch(group -> group.getName().equals(group2.getName())));
        });
  }

  /**
   * Gets membership and expect ok.
   */
  @WithJwtAuthenticationToken(
      preferredUsername = "leopold",
      roles = {USER_ROLE_NAME})
  @Test
  void getMembershipAndExpectOk() {
    webTestClient
        .get()
        .uri("/api/groups/f/membership")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectBodyList(Group.class)
        .value(groups -> {
          assertTrue(groups.stream().anyMatch(group -> group.getName().equals(group0.getName())));
          assertFalse(groups.stream().anyMatch(group -> group.getName().equals(group1.getName())));
          assertTrue(groups.stream().anyMatch(group -> group.getName().equals(group2.getName())));
        });
  }

  /**
   * Gets membership ids and expect ok.
   */
  @WithJwtAuthenticationToken(
      preferredUsername = "leopold",
      roles = {USER_ROLE_NAME, LOCAL_USER_ROLE_NAME}) // required for ldap
  @Test
  @SuppressWarnings("unchecked")
  void getMembershipIdsAndExpectOk() {
    webTestClient
        .get()
        .uri("/api/groups/f/membership-ids")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectBody(Set.class)
        .value(ids -> {
          assertTrue(ids.stream().anyMatch(id -> id.equals(group0.getId())));
          assertFalse(ids.stream().anyMatch(id -> id.equals(group1.getId())));
          assertTrue(ids.stream().anyMatch(id -> id.equals(group2.getId())));
          // from embedded ldap
          assertTrue(ids.stream().anyMatch(id -> id.equals("managers")));
        });
  }

}