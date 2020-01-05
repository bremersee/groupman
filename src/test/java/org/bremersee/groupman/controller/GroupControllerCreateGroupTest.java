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
import org.springframework.web.reactive.function.BodyInserters;

/**
 * The group controller create group tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "bremersee.security.authentication.enable-jwt-support=true"
})
@ActiveProfiles({"default"})
@TestInstance(Lifecycle.PER_CLASS) // allows us to use @BeforeAll with a non-static method
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GroupControllerCreateGroupTest {

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
      preferredUsername = "molly",
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

}