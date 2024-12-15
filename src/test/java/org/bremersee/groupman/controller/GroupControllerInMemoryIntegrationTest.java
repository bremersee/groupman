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

import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bremersee.groupman.model.Group;
import org.bremersee.groupman.model.Source;
import org.bremersee.groupman.repository.GroupEntity;
import org.bremersee.groupman.repository.GroupRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

/**
 * The group controller test with in-memory authentication.
 *
 * @author Christian Bremer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
    "spring.security.user.name=leopold",
    "spring.security.user.password=secret",
    "spring.security.user.roles[0]=USER"
})
@AutoConfigureDataMongo
@ActiveProfiles({"in-memory"})
@TestInstance(Lifecycle.PER_CLASS) // allows us to use @BeforeAll with a non-static method
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GroupControllerInMemoryIntegrationTest {

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

  @LocalServerPort
  int port;

  /**
   * The Group repository.
   */
  @Autowired
  GroupRepository groupRepository;

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
  }

  @Test
  void getGroupById() {
    WebClient webClient = WebClient.builder()
        .baseUrl("http://localhost:" + port)
        .defaultHeader(HttpHeaders.AUTHORIZATION,
            "Basic " + Base64.getEncoder().encodeToString("leopold:secret".getBytes()))
        .build();

    StepVerifier
        .create(webClient
            .get()
            .uri("/api/groups/{id}", "GCGGT0")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(Group.class))
        .assertNext(group -> assertEquals("GCGGT0", group.getId()))
        .verifyComplete();
  }

}