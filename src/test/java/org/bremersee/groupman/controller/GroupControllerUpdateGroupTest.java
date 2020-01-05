package org.bremersee.groupman.controller;

import static org.bremersee.security.core.AuthorityConstants.USER_ROLE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.test.StepVerifier;

/**
 * The group controller update group test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "bremersee.security.authentication.enable-jwt-support=true"
})
@ActiveProfiles({"default"})
@TestInstance(Lifecycle.PER_CLASS) // allows us to use @BeforeAll with a non-static method
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GroupControllerUpdateGroupTest {

  private static final GroupEntity group0 = GroupEntity.builder()
      .id("GCUGT0")
      .source(Source.INTERNAL)
      .name("Group0")
      .description("Group One")
      .createdAt(new Date())
      .modifiedAt(new Date())
      .createdBy("molly")
      .owners(Collections.singleton("molly"))
      .build();

  private static final GroupEntity group1 = GroupEntity.builder()
      .id("GCUGT1")
      .source(Source.INTERNAL)
      .name("Group1")
      .description("Group Two")
      .createdAt(new Date())
      .modifiedAt(new Date())
      .createdBy("someone else")
      .owners(Collections.singleton("someone else"))
      .build();

  private static final GroupEntity group2 = GroupEntity.builder()
      .id("GCUGT2")
      .source(Source.INTERNAL)
      .name("Group2")
      .description("Group Three")
      .createdAt(new Date())
      .modifiedAt(new Date())
      .createdBy("molly")
      .owners(Collections.singleton("molly"))
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
          assertEquals("GCUGT0", groupEntity.getId());
        })
        .verifyComplete();
    StepVerifier
        .create(groupRepository.save(group1))
        .consumeNextWith(groupEntity -> {
          assertNotNull(groupEntity.getId());
          assertEquals("GCUGT1", groupEntity.getId());
        })
        .verifyComplete();
    StepVerifier
        .create(groupRepository.save(group2))
        .consumeNextWith(groupEntity -> {
          assertNotNull(groupEntity.getId());
          assertEquals("GCUGT2", groupEntity.getId());
        })
        .verifyComplete();
  }

  /**
   * Update group and expect forbidden.
   */
  @WithJwtAuthenticationToken(
      preferredUsername = "molly",
      roles = {USER_ROLE_NAME})
  @Test
  @Order(10)
  void updateGroupAndExpectForbidden() {
    webTestClient
        .put()
        .uri("/api/groups/{id}", "GCUGT1")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(Group.builder()
            .name("group")
            .build()))
        .exchange()
        .expectStatus().isForbidden();
  }

  /**
   * Update group and expect not found.
   */
  @WithJwtAuthenticationToken(
      preferredUsername = "molly",
      roles = {USER_ROLE_NAME})
  @Test
  @Order(11)
  void updateGroupAndExpectNotFound() {
    webTestClient
        .put()
        .uri("/api/groups/{id}", UUID.randomUUID().toString())
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(Group.builder()
            .name("group")
            .build()))
        .exchange()
        .expectStatus().isNotFound();
  }

  /**
   * Update group and expect bad request.
   */
  @WithJwtAuthenticationToken(
      preferredUsername = "molly",
      roles = {USER_ROLE_NAME})
  @Test
  @Order(50)
  void updateGroupAndExpectBadRequest() {
    webTestClient
        .put()
        .uri("/api/groups/{id}", "GCUGT0")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(Group.builder()
            .build()))
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody(RestApiException.class)
        .value((Consumer<RestApiException>) restApiException -> {
          assertNotNull(restApiException.getMessage());
          assertEquals("/api/groups/GCUGT0", restApiException.getPath());
          // System.out.println("RestApiException of Validation: " + restApiException);
        });
  }

  /**
   * Patch group.
   */
  @WithJwtAuthenticationToken(
      preferredUsername = "molly",
      roles = {USER_ROLE_NAME})
  @Test
  @Order(90)
  void patchGroup() {
    webTestClient
        .patch()
        .uri("/api/groups/{id}", "GCUGT0")
        .accept(MediaType.APPLICATION_JSON)
        .contentType(MediaType.APPLICATION_JSON)
        .body(BodyInserters.fromValue(Group.builder()
            .name(group0.getName())
            .description("")
            .owners(Arrays.asList("anna", "stephen"))
            .members(Arrays.asList("anna", "leopold", "molly"))
            .build()))
        .exchange()
        .expectBody(Group.class)
        .value((Consumer<Group>) group -> {
          assertNotNull(group);
          assertEquals(group0.getName(), group.getName());
          assertNull(group.getDescription());
          assertTrue(group.getOwners().containsAll(Arrays.asList("anna", "stephen", "molly")));
          assertTrue(group.getMembers().containsAll(Arrays.asList("anna", "leopold", "molly")));
        });
  }

  /**
   * Update group.
   */
  @WithJwtAuthenticationToken(
      preferredUsername = "molly",
      roles = {USER_ROLE_NAME})
  @Test
  @Order(100)
  void updateGroup() {
    webTestClient
        .put()
        .uri("/api/groups/{id}", "GCUGT0")
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
  @WithJwtAuthenticationToken(
      preferredUsername = "molly",
      roles = {USER_ROLE_NAME})
  @Test
  @Order(110)
  void deleteGroup() {
    webTestClient
        .delete()
        .uri("/api/groups/{id}", "GCUGT2")
        .exchange()
        .expectStatus().isOk();

    webTestClient
        .get()
        .uri("/api/groups/{id}", "GCUGT2")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isNotFound();
  }

  /**
   * Delete group and expect forbidden.
   */
  @WithJwtAuthenticationToken(
      preferredUsername = "molly",
      roles = {USER_ROLE_NAME})
  @Test
  @Order(111)
  void deleteGroupAndExpectForbidden() {
    webTestClient
        .delete()
        .uri("/api/groups/{id}", "GCUGT1")
        .exchange()
        .expectStatus().isForbidden();
  }

}