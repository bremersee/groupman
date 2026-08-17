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

import static java.util.Objects.requireNonNullElse;
import static org.springframework.util.ObjectUtils.isEmpty;

import lombok.extern.slf4j.Slf4j;
import org.bremersee.exception.ServiceException;
import org.bremersee.groupman.mapper.GroupMapper;
import org.bremersee.groupman.mapper.UserMapper;
import org.bremersee.groupman.model.Group;
import org.bremersee.groupman.model.GroupCreate;
import org.bremersee.groupman.model.GroupUpdate;
import org.bremersee.groupman.model.User;
import org.bremersee.groupman.validation.GroupValidation;
import org.bremersee.keycloak.api.GetGroupsParameters;
import org.bremersee.keycloak.api.model.GroupRepresentation;
import org.bremersee.keycloak.api.webflux.KeycloakAdminClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The group service.
 *
 * @author Christian Bremer
 */
@Service
@Slf4j
public class GroupService {

  private final KeycloakAdminClient keycloakAdminClient;

  private final GroupValidation groupValidation;

  private final GroupMapper groupMapper;

  private final UserMapper userMapper;

  private final String realm;

  private final int maxGroups;

  private final String mainGroupName;

  private String mainGroupId;

  /**
   * Instantiates a new group service.
   *
   * @param keycloakAdminClient the keycloak admin client
   * @param groupValidation the group validation
   * @param groupMapper the group mapper
   * @param userMapper the user mapper
   * @param realm the realm
   * @param maxGroups the max groups (-1 == infinitive)
   * @param mainGroupName the main group name
   */
  public GroupService(
      KeycloakAdminClient keycloakAdminClient,
      GroupValidation groupValidation,
      GroupMapper groupMapper,
      UserMapper userMapper,
      @Value("${bremersee.keycloak.realm:master}") String realm,
      @Value("${bremersee.groupman.max-groups:100}") int maxGroups,
      @Value("${bremersee.groupman.main-group-name:groupman}") String mainGroupName) {

    Assert.notNull(keycloakAdminClient, "Keycloak admin client must not be null.");
    Assert.notNull(groupValidation, "Group validation must not be null.");
    Assert.notNull(groupMapper, "Group mapper must not be null.");
    Assert.notNull(userMapper, "User mapper must not be null.");
    Assert.hasText(realm, "Realm is required.");
    this.keycloakAdminClient = keycloakAdminClient;
    this.groupValidation = groupValidation;
    this.groupMapper = groupMapper;
    this.userMapper = userMapper;
    this.realm = isEmpty(realm) ? "master" : realm;
    this.maxGroups = maxGroups;
    this.mainGroupName = isEmpty(mainGroupName) ? "groupman" : mainGroupName;
  }

  /**
   * Init.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void init() {
    GroupRepresentation mainGroup = keycloakAdminClient
        .getGroupByPath(realm, "/" + mainGroupName)
        .singleOptional()
        .flatMap(optMainGroup -> optMainGroup
            .map(Mono::just)
            .orElseGet(() -> keycloakAdminClient.saveGroup(realm, createNewMainGroup())))
        .block();
    if (isEmpty(mainGroup)) {
      log.warn("Main group with name '{}' could not be created. Is keycloak admin api a MOCK?",
          mainGroupName);
    } else {
      mainGroupId = mainGroup.getId();
    }
  }

  /**
   * Create group.
   *
   * @param userId the user id
   * @param groupCreateRequest the group dto
   * @return the mono
   */
  public Mono<Group> createGroup(String userId, GroupCreate groupCreateRequest) {
    groupValidation.validateGroup(groupCreateRequest);
    GroupRepresentation group = new GroupRepresentation();
    groupMapper.mapInto(groupCreateRequest, group);
    return getUserMainGroup(userId)
        .filter(g -> maxGroups < 0
            || maxGroups > requireNonNullElse(g.getSubGroupCount(), 0L))
        .switchIfEmpty(Mono
            .error(ServiceException.badRequest("Too many groups.", "too_many_groups")))
        .mapNotNull(GroupRepresentation::getId)
        .flatMap(id -> keycloakAdminClient.createSubGroup(realm, id, group))
        .map(groupMapper::mapToDto);
  }

  /**
   * Gets groups.
   *
   * @param userId the user id
   * @param search the search
   * @param first the first
   * @param max the max
   * @return the groups
   */
  public Flux<Group> getGroups(String userId, String search, Integer first, Integer max) {
    var paramBuilder = GetGroupsParameters.builder()
        .first(first != null && first >= 0 ? first : null)
        .max(max != null && max > 0 ? max : null);
    if (!isEmpty(search)) {
      paramBuilder.search(search);
    }
    return getUserMainGroup(userId)
        .mapNotNull(GroupRepresentation::getId)
        .flatMapMany(groupId -> keycloakAdminClient
            .getSubGroups(realm, groupId, paramBuilder.build()))
        .map(groupMapper::mapToDto);
  }

  /**
   * Gets group.
   *
   * @param userId the user id
   * @param groupId the group id
   * @return the group
   */
  public Mono<Group> getGroup(String userId, String groupId) {
    return getOwnedGroup(userId, groupId)
        .map(groupMapper::mapToDto);
  }

  /**
   * Gets members.
   *
   * @param userId the user id
   * @param groupId the group id
   * @param first the first
   * @param max the max
   * @return the members
   */
  public Flux<User> getMembers(
      String userId,
      String groupId,
      Integer first,
      Integer max) {

    return Flux.from(getOwnedGroup(userId, groupId))
        .flatMap(group -> keycloakAdminClient
            .getGroupMembers(realm, groupId, null, first, max))
        .map(userMapper::mapToDto);
  }

  /**
   * Add member.
   *
   * @param userId the user id
   * @param groupId the group id
   * @param memberId the member id
   * @return the mono
   */
  public Mono<Void> addMember(
      String userId,
      String groupId,
      String memberId) {

    return getOwnedGroup(userId, groupId)
        .flatMap(group -> keycloakAdminClient.addUserToGroup(realm, memberId, groupId));
  }

  /**
   * Remove member.
   *
   * @param userId the user id
   * @param groupId the group id
   * @param memberId the member id
   * @return the mono
   */
  public Mono<Void> removeMember(
      String userId,
      String groupId,
      String memberId) {

    return getOwnedGroup(userId, groupId)
        .flatMap(group -> keycloakAdminClient.removeUserFromGroup(realm, memberId, groupId));
  }

  /**
   * Update group.
   *
   * @param userId the user id
   * @param groupId the group id
   * @param groupUpdateRequest the group update request
   * @return the mono
   */
  public Mono<Group> updateGroup(
      String userId,
      String groupId,
      GroupUpdate groupUpdateRequest) {

    groupValidation.validateGroup(groupUpdateRequest);
    return getOwnedGroup(userId, groupId)
        .flatMap(group -> {
          groupMapper.mapInto(groupUpdateRequest, group);
          return keycloakAdminClient.saveGroup(realm, group);
        })
        .map(groupMapper::mapToDto);
  }

  /**
   * Delete group.
   *
   * @param userId the user id
   * @param groupId the group id
   * @return the mono
   */
  public Mono<Void> deleteGroup(String userId, String groupId) {
    return getOwnedGroup(userId, groupId)
        .flatMap(group -> keycloakAdminClient.deleteGroup(realm, groupId));
  }

  private GroupRepresentation createNewMainGroup() {
    GroupRepresentation mainGroup = new GroupRepresentation();
    mainGroup.setName(mainGroupName);
    mainGroup.setDescription("Main group for groupman");
    return mainGroup;
  }

  /**
   * Creates a new user main group representation without persisting.
   *
   * <p>The name of the group is the given user ID.
   *
   * @param userId the user ID
   * @return the new user main group representation
   */
  private Mono<GroupRepresentation> newUserMainGroupRepresentation(String userId) {
    return keycloakAdminClient.getUserById(realm, userId, null)
        .map(user -> {
          GroupRepresentation mainGroup = new GroupRepresentation();
          mainGroup.setName(userId);
          StringBuilder nameBuilder = new StringBuilder();
          if (!isEmpty(user.getFirstName())) {
            nameBuilder.append(user.getFirstName());
          }
          if (!isEmpty(user.getLastName())) {
            if (!isEmpty(user.getFirstName())) {
              nameBuilder.append(' ');
            }
            nameBuilder.append(user.getLastName());
          }
          if (!nameBuilder.isEmpty()) {
            mainGroup.setDescription("Main group of " + nameBuilder);
          }
          return mainGroup;
        })
        .switchIfEmpty(Mono.error(ServiceException.forbidden("User", userId)));
  }

  private Mono<GroupRepresentation> getUserMainGroup(String userId) {
    if (isEmpty(mainGroupId)) {
      return Mono.error(ServiceException
          .internalServerError("Main group is not initialized.", "main_group_is_missing"));
    }
    return keycloakAdminClient.getGroupByPath(realm, "/" + mainGroupName + "/" + userId)
        .singleOptional()
        .flatMap(optUserMainGroup -> optUserMainGroup
            .map(Mono::just)
            .orElseGet(() -> newUserMainGroupRepresentation(userId)
                .flatMap(g -> keycloakAdminClient.createSubGroup(realm, mainGroupId, g))));
  }

  private Mono<GroupRepresentation> getOwnedGroup(String userId, String groupId) {
    return getUserMainGroup(userId)
        .mapNotNull(GroupRepresentation::getId)
        .flatMap(userMainGroupId -> keycloakAdminClient.getGroupById(realm, groupId)
            .filter(group -> userMainGroupId.equals(group.getParentId())))
        .switchIfEmpty(Mono.error(ServiceException
            .notFoundWithErrorCode("Group", groupId, "group_not_found")));
  }

}
