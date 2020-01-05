package org.bremersee.groupman.controller;

import static org.bremersee.security.core.AuthorityConstants.ADMIN_ROLE_NAME;
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
 * The group admin controller add group tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "bremersee.security.authentication.enable-jwt-support=true"
})
@ActiveProfiles({"default"})
@TestInstance(Lifecycle.PER_CLASS) // allows us to use @BeforeAll with a non-static method
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GroupAdminControllerAddGroupTest {

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
   * Add group and expect forbidden.
   */
  @WithJwtAuthenticationToken(roles = {USER_ROLE_NAME})
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
  @WithJwtAuthenticationToken(roles = {ADMIN_ROLE_NAME})
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
  @WithJwtAuthenticationToken(roles = {ADMIN_ROLE_NAME})
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
        .value((Consumer<RestApiException>) restApiException -> {
          assertEquals("GRP:1002", restApiException.getErrorCode()); // see application.yml
        });
  }

  /**
   * Add group and expect no owners and members.
   */
  @WithJwtAuthenticationToken(
      preferredUsername = "admin",
      roles = {ADMIN_ROLE_NAME})
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
        .value((Consumer<Group>) group -> {
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
  @WithJwtAuthenticationToken(
      preferredUsername = "admin",
      roles = {ADMIN_ROLE_NAME})
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
        .value((Consumer<Group>) group -> {
          assertNotNull(group);
          assertTrue(group.getOwners().containsAll(Arrays.asList("anna", "stephen")));
          assertTrue(group.getMembers().containsAll(Arrays.asList("anna", "leopold", "molly")));
        });
  }

  /**
   * Add group and expect already exists.
   */
  @WithJwtAuthenticationToken(
      preferredUsername = "admin",
      roles = {ADMIN_ROLE_NAME})
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
        .value((Consumer<RestApiException>) restApiException -> {
          assertEquals("GRP:1000", restApiException.getErrorCode()); // see application.yml
        });
  }

}