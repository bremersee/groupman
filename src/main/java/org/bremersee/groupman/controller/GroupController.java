/*
 * Copyright 2019-2020 the original author or authors.
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

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.exception.ServiceException;
import org.bremersee.groupman.api.GroupWebfluxControllerApi;
import org.bremersee.groupman.model.Group;
import org.bremersee.groupman.model.Source;
import org.bremersee.groupman.model.Status;
import org.bremersee.groupman.repository.GroupEntity;
import org.bremersee.groupman.repository.GroupRepository;
import org.bremersee.groupman.repository.ldap.GroupLdapRepository;
import org.bremersee.security.core.UserContext;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The group controller.
 *
 * @author Christian Bremer
 */
@RestController
@Slf4j
public class GroupController
    extends AbstractGroupController
    implements GroupWebfluxControllerApi {

  private final Long maxOwnedGroups;

  /**
   * Instantiates a new group controller.
   *
   * @param groupRepository the group repository
   * @param groupLdapRepository the group ldap repository
   * @param modelMapper the model mapper
   * @param localRole if a role name is given, ldap will only be called, if the user has this
   *     role; if the role name is null or empty, ldap will always be called
   * @param maxOwnedGroups the max owned groups
   */
  public GroupController(
      GroupRepository groupRepository,
      GroupLdapRepository groupLdapRepository,
      ModelMapper modelMapper,
      @Value("${bremersee.groupman.local-role:ROLE_LOCAL_USER}") String localRole,
      @Value("${bremersee.groupman.max-owned-groups:-1}") Long maxOwnedGroups) {
    super(groupRepository, groupLdapRepository, modelMapper, localRole);
    this.maxOwnedGroups = maxOwnedGroups != null ? maxOwnedGroups : -1L;
  }

  @Override
  public Mono<Group> createGroup(Group group) {
    return getCaller()
        .oneWithUserContext(userContext -> createGroup(group, userContext))
        .map(this::mapToGroup);
  }

  private Mono<GroupEntity> createGroup(Group group, UserContext currentUser) {
    group.setId(null);
    group.setCreatedAt(OffsetDateTime.now(ZoneId.of("UTC")));
    group.setModifiedAt(group.getCreatedAt());
    group.setCreatedBy(currentUser.getName());
    group.setSource(Source.INTERNAL);
    group.getOwners().add(currentUser.getName());
    return Mono.just(group)
        .flatMap(newGroup -> maxOwnedGroups < 0
            ? Mono.just(newGroup)
            : getGroupRepository().countOwnedGroups(currentUser.getName())
                .flatMap(size -> size >= maxOwnedGroups
                    ? Mono.error(() -> ServiceException.badRequest(
                    "The maximum number of groups has been reached.",
                    "GRP:MAX_OWNED_GROUPS"))
                    : Mono.just(newGroup)))
        .flatMap(newGroup -> getGroupRepository().save(mapToGroupEntity(newGroup)));
  }

  @Override
  public Mono<Group> getGroupById(String groupId) {
    return super.getGroupEntityById(groupId)
        .map(this::mapToGroup);
  }

  @Override
  public Mono<Group> updateGroup(String groupId, Group group) {
    return getCaller()
        .oneWithUserContext(userContext -> updateGroup(groupId, group, userContext))
        .map(this::mapToGroup);
  }

  private Mono<GroupEntity> updateGroup(String groupId, Group group, UserContext userContext) {
    if (group.getOwners().isEmpty()) {
      group.getOwners().add(userContext.getName());
    }
    return getGroupEntityById(groupId)
        .switchIfEmpty(Mono.error(() -> ServiceException.notFound("Group", groupId)))
        .filter(groupEntity -> groupEntity.getOwners().contains(userContext.getName()))
        .switchIfEmpty(Mono.error(() -> ServiceException.forbidden("Group", groupId)))
        .flatMap(groupEntity -> getGroupRepository().save(updateGroup(group, () -> groupEntity)));
  }

  @Override
  public Mono<Void> deleteGroup(String groupId) {
    return getCaller()
        .oneWithUserContext(userContext -> deleteGroup(groupId, userContext));
  }

  private Mono<Void> deleteGroup(String groupId, UserContext userContext) {
    return getGroupRepository().findById(groupId)
        .switchIfEmpty(Mono.error(() -> ServiceException.notFound("Group", groupId)))
        .filter(groupEntity -> groupEntity.getOwners().contains(userContext.getName()))
        .switchIfEmpty(Mono.error(() -> ServiceException.forbidden("Group", groupId)))
        .flatMap(groupEntity -> getGroupRepository().delete(groupEntity));
  }

  @Override
  public Flux<Group> getGroupsByIds(List<String> ids) {
    return super.getGroupEntitiesByIds(ids)
        .map(this::mapToGroup);
  }

  @Override
  public Flux<Group> getEditableGroups() {
    return getCaller()
        .manyWithUserContext(this::getEditableGroups)
        .map(this::mapToGroup);
  }

  private Flux<GroupEntity> getEditableGroups(UserContext userContext) {
    return getGroupRepository().findByOwnersIsContaining(userContext.getName(), SORT);
  }

  @Override
  public Flux<Group> getUsableGroups() {
    return getCaller()
        .manyWithUserContext(this::getUsableGroups)
        .map(this::mapToGroup);
  }

  private Flux<GroupEntity> getUsableGroups(UserContext userContext) {
    final String name = userContext.getName();
    return getGroupRepository().findByOwnersIsContainingOrMembersIsContaining(name, name)
        .concatWith(getGroupLdapRepository().findByMembersIsContaining(name))
        .sort(COMPARATOR);
  }

  @Override
  public Flux<Group> getMembership() {
    return getCaller()
        .manyWithUserContext(this::getMembership)
        .map(this::mapToGroup);
  }

  private Flux<GroupEntity> getMembership(UserContext userContext) {
    final String name = userContext.getName();
    if (userContext.hasRole(getLocalUserRole())) {
      return getGroupRepository().findByMembersIsContaining(name)
          .concatWith(getGroupLdapRepository().findByMembersIsContaining(name))
          .sort(COMPARATOR);
    }
    return getGroupRepository().findByMembersIsContaining(name).sort(COMPARATOR);
  }

  @Override
  public Mono<Set<String>> getMembershipIds() {
    return getCaller().oneWithUserContext(userContext -> getMembership(userContext)
        .map(GroupEntity::getId).collect(Collectors.toSet()));
  }

  @Override
  public Mono<Status> getStatus() {
    return getCaller().oneWithUserContext(this::getStatus);
  }

  private Mono<Status> getStatus(UserContext userContext) {
    return getGroupRepository().countOwnedGroups(userContext.getName())
        .zipWith(getMembershipSum(userContext))
        .map(sizes -> Status.builder()
            .ownedGroupSize(sizes.getT1())
            .membershipSize(sizes.getT2())
            .maxOwnedGroups(maxOwnedGroups)
            .build());
  }

  private Mono<Long> getMembershipSum(UserContext userContext) {
    if (userContext.hasRole(getLocalUserRole())) {
      return getGroupRepository().countMembership(userContext.getName())
          .zipWith(getGroupLdapRepository().countMembership(userContext.getName()))
          .map(sizes -> sizes.getT1() + sizes.getT2());
    }
    return getGroupRepository().countMembership(userContext.getName());
  }

}
