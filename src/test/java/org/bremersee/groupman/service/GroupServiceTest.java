/*
 * Copyright 2026 the original author or authors.
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

package org.bremersee.groupman.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.bremersee.exception.ServiceException;
import org.bremersee.groupman.mapper.GroupMapper;
import org.bremersee.groupman.mapper.UserMapper;
import org.bremersee.groupman.model.Group;
import org.bremersee.groupman.model.GroupCreate;
import org.bremersee.groupman.model.GroupUpdate;
import org.bremersee.groupman.model.User;
import org.bremersee.groupman.validation.GroupValidation;
import org.bremersee.keycloak.api.model.GroupRepresentation;
import org.bremersee.keycloak.api.model.UserRepresentation;
import org.bremersee.keycloak.api.webflux.KeycloakAdminClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * The group service test.
 *
 * @author Christian Bremer
 */
class GroupServiceTest {

  private static final String REALM = "junit";

  private static final String MAIN_GROUP_NAME = "groupman";

  private static final String USER_ID = "user-id";

  private static final String USER_MAIN_GROUP_ID = "user-main-group-id";

  private static final String USER_GROUP_ID = "user-group-id";

  private KeycloakAdminClient keycloakAdminClient;

  private GroupService target;

  /**
   * Sets up.
   */
  @BeforeEach
  void setUp() {
    keycloakAdminClient = mock(KeycloakAdminClient.class);
    target = new GroupService(
        keycloakAdminClient,
        new GroupValidation(),
        Mappers.getMapper(GroupMapper.class),
        Mappers.getMapper(UserMapper.class),
        REALM,
        20,
        MAIN_GROUP_NAME);
    init();
  }

  private void init() {
    GroupRepresentation mainGroup = new GroupRepresentation();
    mainGroup.setId(MAIN_GROUP_NAME + "_id");
    mainGroup.setName(MAIN_GROUP_NAME);
    doReturn(Mono.just(mainGroup))
        .when(keycloakAdminClient)
        .getGroupByPath(REALM, "/" + MAIN_GROUP_NAME);
    target.init();
  }

  private void mockGetUserMainGroup(long subGroupCount) {
    GroupRepresentation userMainGroup = new GroupRepresentation();
    userMainGroup.setId(USER_MAIN_GROUP_ID);
    userMainGroup.setName(USER_ID);
    userMainGroup.setPath("/" + MAIN_GROUP_NAME + "/" + USER_ID);
    userMainGroup.setParentId(MAIN_GROUP_NAME + "_id");
    userMainGroup.setSubGroupCount(subGroupCount);
    doReturn(Mono.just(userMainGroup))
        .when(keycloakAdminClient)
        .getGroupByPath(REALM, "/" + MAIN_GROUP_NAME + "/" + USER_ID);
  }

  private void mockCreateUserMainGroup() {
    doReturn(Mono.empty())
        .when(keycloakAdminClient)
        .getGroupByPath(REALM, "/" + MAIN_GROUP_NAME + "/" + USER_ID);

    UserRepresentation user = new UserRepresentation();
    user.setId(USER_ID);
    doReturn(Mono.just(user))
        .when(keycloakAdminClient)
        .getUserById(eq(REALM), eq(USER_ID), any());

    GroupRepresentation userMainGroup = new GroupRepresentation();
    userMainGroup.setId(USER_MAIN_GROUP_ID);
    userMainGroup.setName(USER_ID);
    userMainGroup.setPath("/" + MAIN_GROUP_NAME + "/" + USER_ID);
    userMainGroup.setParentId(MAIN_GROUP_NAME + "_id");
    userMainGroup.setSubGroupCount(1L);
    doReturn(Mono.just(userMainGroup))
        .when(keycloakAdminClient)
        .createSubGroup(eq(REALM), eq(MAIN_GROUP_NAME + "_id"), any());
  }

  private GroupRepresentation getUsersGroup() {
    GroupRepresentation subGroup = new GroupRepresentation();
    subGroup.setId(USER_GROUP_ID);
    subGroup.setName("sub-group");
    subGroup.setDescription("foo bar");
    subGroup.setParentId(USER_MAIN_GROUP_ID);
    subGroup.setPath("/" + MAIN_GROUP_NAME + "/" + USER_ID + "/sub-group");
    return subGroup;
  }

  /**
   * Create group.
   */
  @Test
  void createGroup() {
    mockGetUserMainGroup(13L);
    GroupRepresentation groupRepresentation = new GroupRepresentation();
    groupRepresentation.setId("my-group-id");
    groupRepresentation.setName("my-group");
    doReturn(Mono.just(groupRepresentation))
        .when(keycloakAdminClient)
        .createSubGroup(eq(REALM), eq(USER_MAIN_GROUP_ID), any());

    Group expected = Group.builder()
        .id("my-group-id")
        .name("my-group")
        .build();
    GroupCreate groupCreate = GroupCreate.builder()
        .name("my-group")
        .build();
    Group actual = target.createGroup(USER_ID, groupCreate).block();
    assertThat(actual)
        .isEqualTo(expected);
  }

  /**
   * Create group failed because of too many groups.
   */
  @Test
  void createGroupFailedBecauseOfTooManyGroups() {
    mockGetUserMainGroup(1000L);
    GroupRepresentation groupRepresentation = new GroupRepresentation();
    groupRepresentation.setId("my-group-id");
    groupRepresentation.setName("my-group");
    doReturn(Mono.just(groupRepresentation))
        .when(keycloakAdminClient)
        .createSubGroup(eq(REALM), eq(USER_MAIN_GROUP_ID), any());

    GroupCreate groupCreate = GroupCreate.builder()
        .name("my-group")
        .build();
    StepVerifier.create(target.createGroup(USER_ID, groupCreate))
        .expectError(ServiceException.class)
        .verify();
  }

  /**
   * Gets groups.
   */
  @Test
  void getGroups() {
    mockCreateUserMainGroup();
    GroupRepresentation groupRepresentation = getUsersGroup();
    doReturn(Flux.fromIterable(List.of(groupRepresentation)))
        .when(keycloakAdminClient)
        .getSubGroups(eq(REALM), eq(USER_MAIN_GROUP_ID), any());
    List<Group> expected = List.of(Group.builder()
        .id(USER_GROUP_ID)
        .name("sub-group")
        .description("foo bar")
        .build());
    List<Group> actual = target.getGroups(USER_ID, null, null, null)
        .collectList().block();
    assertThat(actual)
        .isEqualTo(expected);
  }

  /**
   * Gets group.
   */
  @Test
  void getGroup() {
    mockCreateUserMainGroup();
    doReturn(Mono.just(getUsersGroup()))
        .when(keycloakAdminClient)
        .getGroupById(REALM, USER_GROUP_ID);

    Group expected = Group.builder()
        .id(USER_GROUP_ID)
        .name("sub-group")
        .description("foo bar")
        .build();
    Group actual = target.getGroup(USER_ID, USER_GROUP_ID).block();
    assertThat(actual)
        .isEqualTo(expected);
  }

  /**
   * Gets members.
   */
  @Test
  void getMembers() {
    mockCreateUserMainGroup();
    doReturn(Mono.just(getUsersGroup()))
        .when(keycloakAdminClient)
        .getGroupById(REALM, USER_GROUP_ID);
    UserRepresentation userRepresentation = new UserRepresentation();
    userRepresentation.setId("a-user-id");
    userRepresentation.setUsername("group-member");
    userRepresentation.setFirstName("first");
    userRepresentation.setLastName("last");
    doReturn(Flux.fromIterable(List.of(userRepresentation)))
        .when(keycloakAdminClient)
        .getGroupMembers(eq(REALM), eq(USER_GROUP_ID), any(), any(), any());
    List<User> expected = List.of(User.builder()
        .id("a-user-id")
        .username("group-member")
        .firstName("first")
        .lastName("last")
        .build());
    List<User> actual = target.getMembers(USER_ID, USER_GROUP_ID, null, null)
        .collectList()
        .block();
    assertThat(actual)
        .isEqualTo(expected);
  }

  /**
   * Add member.
   */
  @Test
  void addMember() {
    mockCreateUserMainGroup();
    doReturn(Mono.just(getUsersGroup()))
        .when(keycloakAdminClient)
        .getGroupById(REALM, USER_GROUP_ID);
    doReturn(Mono.empty())
        .when(keycloakAdminClient)
        .addUserToGroup(REALM, "a-user-id", USER_GROUP_ID);
    StepVerifier
        .create(target.addMember(USER_ID, USER_GROUP_ID, "a-user-id"))
        .verifyComplete();
  }

  /**
   * Remove member.
   */
  @Test
  void removeMember() {
    mockCreateUserMainGroup();
    doReturn(Mono.just(getUsersGroup()))
        .when(keycloakAdminClient)
        .getGroupById(REALM, USER_GROUP_ID);
    doReturn(Mono.empty())
        .when(keycloakAdminClient)
        .removeUserFromGroup(REALM, "a-user-id", USER_GROUP_ID);
    StepVerifier
        .create(target.removeMember(USER_ID, USER_GROUP_ID, "a-user-id"))
        .verifyComplete();
  }

  /**
   * Update group.
   */
  @Test
  void updateGroup() {
    mockCreateUserMainGroup();
    doReturn(Mono.just(getUsersGroup()))
        .when(keycloakAdminClient)
        .getGroupById(REALM, USER_GROUP_ID);
    GroupRepresentation groupRepresentation = getUsersGroup();
    groupRepresentation.setName("new name");
    groupRepresentation.setDescription("new description");
    doReturn(Mono.just(groupRepresentation))
        .when(keycloakAdminClient)
        .saveGroup(eq(REALM), any());
    GroupUpdate groupUpdate = GroupUpdate.builder()
        .name("new name")
        .description("new description")
        .build();
    Group expected = Group.builder()
        .id(USER_GROUP_ID)
        .name("new name")
        .description("new description")
        .build();
    Group actual = target.updateGroup(USER_ID, USER_GROUP_ID, groupUpdate).block();
    assertThat(actual)
        .isEqualTo(expected);
  }

  /**
   * Delete group.
   */
  @Test
  void deleteGroup() {
    mockCreateUserMainGroup();
    doReturn(Mono.just(getUsersGroup()))
        .when(keycloakAdminClient)
        .getGroupById(REALM, USER_GROUP_ID);
    doReturn(Mono.empty())
        .when(keycloakAdminClient)
        .deleteGroup(REALM, USER_GROUP_ID);
    StepVerifier
        .create(target.deleteGroup(USER_ID, USER_GROUP_ID))
        .verifyComplete();
  }
}