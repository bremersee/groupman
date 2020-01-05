package org.bremersee.groupman.controller;

import static org.bremersee.security.core.AuthorityConstants.ADMIN_ROLE_NAME;
import static org.bremersee.security.core.AuthorityConstants.USER_ROLE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.UUID;
import java.util.function.Consumer;
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
 * The group admin controller find groups tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "bremersee.security.authentication.enable-jwt-support=true",
    "spring.ldap.embedded.base-dn=dc=bremersee,dc=org",
    "spring.ldap.embedded.credential.username=uid=admin",
    "spring.ldap.embedded.credential.password=secret",
    "spring.ldap.embedded.ldif=classpath:schema.ldif",
    "spring.ldap.embedded.port=12390",
    "spring.ldap.embedded.validation.enabled=false",
    "bremersee.ldaptive.enabled=true",
    "bremersee.ldaptive.use-unbound-id-provider=true",
    "bremersee.ldaptive.ldap-url=ldap://localhost:12390",
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
class GroupAdminControllerFindGroupsTest {

  private static final GroupEntity group0 = GroupEntity.builder()
      .id("GACFGT0")
      .source(Source.INTERNAL)
      .name("Group0")
      .description("Group One")
      .createdAt(new Date())
      .modifiedAt(new Date())
      .createdBy("someone")
      .build();

  private static final GroupEntity group1 = GroupEntity.builder()
      .id("GACFGT1")
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
          assertEquals("GACFGT0", groupEntity.getId());
        })
        .verifyComplete();
    StepVerifier
        .create(groupRepository.save(group1))
        .consumeNextWith(groupEntity -> {
          assertNotNull(groupEntity.getId());
          assertEquals("GACFGT1", groupEntity.getId());
        })
        .verifyComplete();
  }

  /**
   * Find groups and expect forbidden.
   */
  @WithJwtAuthenticationToken(roles = {USER_ROLE_NAME})
  @Test
  void findGroupsAndExpectForbidden() {
    webTestClient
        .get()
        .uri("/api/admin/groups")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isForbidden();
  }

  /**
   * Find groups and expect ok.
   */
  @WithJwtAuthenticationToken(roles = {ADMIN_ROLE_NAME})
  @Test
  void findGroupsAndExpectOk() {
    webTestClient
        .get()
        .uri("/api/admin/groups")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectBodyList(Group.class)
        .value(groups -> {
          assertTrue(groups.stream().anyMatch(group -> group.getName().equals(group0.getName())));
          assertTrue(groups.stream().anyMatch(group -> group.getName().equals(group1.getName())));
          // from embedded ldap:
          assertTrue(groups.stream().anyMatch(group -> group.getName().equals("developers")));
          assertTrue(groups.stream().anyMatch(group -> group.getName().equals("managers")));
        });
  }

  /**
   * Find group by id and expect ok.
   */
  @WithJwtAuthenticationToken(roles = {ADMIN_ROLE_NAME})
  @Test
  void findGroupByIdAndExpectOk() {
    webTestClient
        .get()
        .uri("/api/admin/groups/{id}", "GACFGT0")
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
   * Find ldap group by id and expect ok.
   */
  @WithJwtAuthenticationToken(roles = {ADMIN_ROLE_NAME})
  @Test
  void findLdapGroupByIdAndExpectOk() {
    webTestClient
        .get()
        .uri("/api/admin/groups/{id}", "developers")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectBody(Group.class)
        .value((Consumer<Group>) group -> {
          assertEquals("developers", group.getName());
          assertTrue(group.getMembers().contains("anna"));
          assertTrue(group.getMembers().contains("hans"));
        });
  }

  /**
   * Find group by ids and expect ok.
   */
  @WithJwtAuthenticationToken(roles = {ADMIN_ROLE_NAME})
  @Test
  void findGroupByIdsAndExpectOk() {
    webTestClient
        .get()
        .uri("/api/admin/groups/f?id=GACFGT0&id=GACFGT1&id=managers")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isOk()
        .expectBodyList(Group.class)
        .value(groups -> {
          assertTrue(groups.stream().anyMatch(group -> group.getName().equals(group0.getName())));
          assertTrue(groups.stream().anyMatch(group -> group.getName().equals(group1.getName())));
          assertTrue(groups.stream().anyMatch(group -> group.getName().equals("managers")));
          assertFalse(groups.stream().anyMatch(group -> group.getName().equals("developers")));
        });
  }

  /**
   * Find group by id and expect not found.
   */
  @WithJwtAuthenticationToken(roles = {ADMIN_ROLE_NAME})
  @Test
  void findGroupByIdAndExpectNotFound() {
    webTestClient
        .get()
        .uri("/api/admin/groups/{id}", UUID.randomUUID().toString())
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .expectStatus().isNotFound();
  }

}