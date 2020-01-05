package org.bremersee.groupman.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bremersee.groupman.model.Source;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = WebEnvironment.NONE, properties = {
    "bremersee.security.authentication.enable-jwt-support=false",
})
@ActiveProfiles({"basic-auth"})
@TestInstance(Lifecycle.PER_CLASS) // allows us to use @BeforeAll with a non-static method
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GroupRepositoryTest {

  private static final GroupEntity group0 = GroupEntity.builder()
      .source(Source.INTERNAL)
      .name("Group0")
      .description("Group One")
      .createdAt(new Date())
      .modifiedAt(new Date())
      .createdBy("gustav")
      .owners(Collections.singleton("gustav"))
      .members(Stream.of("gustav", "leopold").collect(Collectors.toSet()))
      .build();

  private static final GroupEntity group1 = GroupEntity.builder()
      .source(Source.INTERNAL)
      .name("Group1")
      .description("Group Two")
      .createdAt(new Date())
      .modifiedAt(new Date())
      .createdBy("gustav")
      .owners(Stream.of("leopold", "gustav").collect(Collectors.toSet()))
      .build();

  private static final GroupEntity group2 = GroupEntity.builder()
      .source(Source.INTERNAL)
      .name("Group2")
      .description("Group Three")
      .createdAt(new Date())
      .modifiedAt(new Date())
      .createdBy("leopold")
      .owners(Collections.singleton("leopold"))
      .members(Stream.of("gustav", "leopold", "stephen").collect(Collectors.toSet()))
      .build();

  @Autowired
  private GroupRepository groupRepository;

  @BeforeAll
  void setUpData() {
    StepVerifier
        .create(groupRepository.save(group0))
        .assertNext(groupEntity -> {
          assertNotNull(groupEntity.getId());
        })
        .expectComplete()
        .verify();
    StepVerifier
        .create(groupRepository.save(group1))
        .assertNext(groupEntity -> {
          assertNotNull(groupEntity.getId());
        })
        .expectComplete()
        .verify();
    StepVerifier
        .create(groupRepository.save(group2))
        .assertNext(groupEntity -> {
          assertNotNull(groupEntity.getId());
        })
        .expectComplete()
        .verify();
  }

  @Test
  void countOwnedGroups() {
    StepVerifier
        .create(groupRepository.countOwnedGroups("gustav"))
        .assertNext(size -> assertEquals(2L, size))
        .expectComplete()
        .verify();
  }
}