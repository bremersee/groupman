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

package org.bremersee.groupman.metrics;

import static org.bremersee.security.core.AuthorityConstants.ACTUATOR_ADMIN_ROLE_NAME;
import static org.bremersee.security.core.AuthorityConstants.ACTUATOR_ROLE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bremersee.groupman.model.Source;
import org.bremersee.groupman.repository.GroupEntity;
import org.bremersee.groupman.repository.GroupRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

/**
 * The metrics test.
 *
 * @author Christian Bremer
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.ldap.embedded.base-dn=dc=bremersee,dc=org",
    "spring.ldap.embedded.credential.username=uid=admin",
    "spring.ldap.embedded.credential.password=secret",
    "spring.ldap.embedded.ldif=classpath:schema.ldif",
    "spring.ldap.embedded.port=12392",
    "spring.ldap.embedded.validation.enabled=false",
    "bremersee.ldaptive.enabled=true",
    "bremersee.ldaptive.use-unbound-id-provider=true",
    "bremersee.ldaptive.ldap-url=ldap://localhost:12392",
    "bremersee.ldaptive.use-ssl=false",
    "bremersee.ldaptive.use-start-tls=false",
    "bremersee.ldaptive.bind-dn=uid=admin",
    "bremersee.ldaptive.bind-credentials=secret",
    "bremersee.ldaptive.pooled=false",
    "bremersee.domain-controller.group-base-dn=ou=groups,dc=bremersee,dc=org",
    "bremersee.domain-controller.group-member-attribute=uniqueMember",
    "bremersee.domain-controller.member-dn=true",
    "bremersee.domain-controller.user-base-dn=ou=people,dc=bremersee,dc=org",
    "bremersee.domain-controller.user-rdn=uid",
    "bremersee.domain-controller.group-find-all-filter=(objectClass=groupOfUniqueNames)",
    "bremersee.domain-controller.group-find-one-filter=(&(objectClass=groupOfUniqueNames)(cn={0}))"
})
@ActiveProfiles({"in-memory", "ldap"})
@TestInstance(Lifecycle.PER_CLASS) // allows us to use @BeforeAll with a non-static method
public class MetricsTest {

  private static final GroupEntity group0 = GroupEntity.builder()
      .id("MT0")
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
      .id("MT1")
      .source(Source.INTERNAL)
      .name("Group1")
      .description("Group Two")
      .createdAt(new Date())
      .modifiedAt(new Date())
      .createdBy("molly")
      .owners(Collections.singleton("molly"))
      .build();

  private static final GroupEntity group2 = GroupEntity.builder()
      .id("MT2")
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
          assertEquals("MT0", groupEntity.getId());
        })
        .verifyComplete();
    StepVerifier
        .create(groupRepository.save(group1))
        .consumeNextWith(groupEntity -> {
          assertNotNull(groupEntity.getId());
          assertEquals("MT1", groupEntity.getId());
        })
        .verifyComplete();
    StepVerifier
        .create(groupRepository.save(group2))
        .consumeNextWith(groupEntity -> {
          assertNotNull(groupEntity.getId());
          assertEquals("MT2", groupEntity.getId());
        })
        .verifyComplete();
  }

  /**
   * Call prometheus endpoint.
   */
  @WithMockUser(
      username = "actuator",
      password = "actuator",
      authorities = {ACTUATOR_ROLE_NAME, ACTUATOR_ADMIN_ROLE_NAME})
  @Test
  void callPrometheusEndpoint() {
    webTestClient
        .get()
        .uri("/actuator/prometheus")
        .exchange()
        .expectStatus().isOk()
        .expectBody(String.class)
        .value(body -> {
          System.out.println("Prometheus data:\n" + body);
          assertTrue(body.contains("groups_size{storage=\"ldap\",} 2.0"));
          assertTrue(body.contains("groups_size{storage=\"mongodb\",} 3.0"));
        });
  }

}
