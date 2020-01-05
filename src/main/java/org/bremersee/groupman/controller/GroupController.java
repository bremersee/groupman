/*
 * Copyright 2019 the original author or authors.
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

import io.swagger.annotations.ApiParam;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.exception.ServiceException;
import org.bremersee.groupman.api.GroupWebfluxControllerApi;
import org.bremersee.groupman.model.Group;
import org.bremersee.groupman.model.Source;
import org.bremersee.groupman.repository.GroupEntity;
import org.bremersee.groupman.repository.GroupRepository;
import org.bremersee.groupman.repository.ldap.GroupLdapRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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

  /**
   * Instantiates a new group controller.
   *
   * @param groupRepository     the group repository
   * @param groupLdapRepository the group ldap repository
   * @param localRole           if a role name is given, ldap will only be called, if the user has
   *                            this role; if the role name is null or empty, ldap will always be
   *                            called
   */
  public GroupController(
      GroupRepository groupRepository,
      GroupLdapRepository groupLdapRepository,
      @Value("${bremersee.groupman.local-role:ROLE_LOCAL_USER}") String localRole) {
    super(groupRepository, groupLdapRepository, localRole);
  }

  @Override
  public Mono<Group> createGroup(Group group) {
    return oneWithCurrentUser(currentUser -> createGroup(group, currentUser).map(this::mapToGroup));
  }

  private Mono<GroupEntity> createGroup(Group group, CurrentUser currentUser) {
    group.setId(null);
    group.setCreatedAt(OffsetDateTime.now(ZoneId.of("UTC")));
    group.setModifiedAt(group.getCreatedAt());
    group.setCreatedBy(currentUser.getName());
    group.setSource(Source.INTERNAL);
    group.getOwners().add(currentUser.getName());
    return getGroupRepository().save(mapToGroupEntity(group));
  }

  @Override
  public Mono<Group> getGroupById(String groupId) {
    return super.getGroupEntityById(groupId)
        .map(this::mapToGroup);
  }

  @Override
  public Mono<Group> updateGroup(String groupId, Group group) {
    return oneWithCurrentUser(currentUser -> updateGroup(groupId, group, currentUser)
        .map(this::mapToGroup));
  }

  private Mono<GroupEntity> updateGroup(String groupId, Group group, CurrentUser currentUser) {
    return getGroupEntityById(groupId)
        .switchIfEmpty(Mono.error(() -> ServiceException.notFound("Group", groupId)))
        .filter(groupEntity -> groupEntity.getOwners().contains(currentUser.getName()))
        .switchIfEmpty(Mono.error(() -> ServiceException.forbidden("Group", groupId)))
        .flatMap(groupEntity -> getGroupRepository().save(updateGroup(group, () -> groupEntity)));
  }

  @Override
  public Mono<Group> patchGroup(String id, Group group) {
    return oneWithCurrentUser(currentUser -> patchGroup(id, group, currentUser)
        .map(this::mapToGroup));
  }

  private Mono<GroupEntity> patchGroup(String groupId, Group group, CurrentUser currentUser) {
    return getGroupEntityById(groupId)
        .switchIfEmpty(Mono.error(() -> ServiceException.notFound("Group", groupId)))
        .filter(groupEntity -> groupEntity.getOwners().contains(currentUser.getName()))
        .switchIfEmpty(Mono.error(() -> ServiceException.forbidden("Group", groupId)))
        .flatMap(groupEntity -> getGroupRepository()
            .save(patchGroup(group, () -> groupEntity, currentUser)));
  }

  @Override
  public Mono<Void> deleteGroup(String groupId) {
    return oneWithCurrentUser(currentUser -> deleteGroup(groupId, currentUser));
  }

  private Mono<Void> deleteGroup(String groupId, CurrentUser currentUser) {
    return getGroupRepository().findById(groupId)
        .switchIfEmpty(Mono.error(() -> ServiceException.notFound("Group", groupId)))
        .filter(groupEntity -> groupEntity.getOwners().contains(currentUser.getName()))
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
    return manyWithCurrentUser(currentUser -> getEditableGroups(currentUser).map(this::mapToGroup));
  }

  private Flux<GroupEntity> getEditableGroups(CurrentUser currentUser) {
    return getGroupRepository().findByOwnersIsContaining(currentUser.getName(), SORT);
  }

  @Override
  public Flux<Group> getUsableGroups() {
    return manyWithCurrentUser(currentUser -> getUsableGroups(currentUser).map(this::mapToGroup));
  }

  private Flux<GroupEntity> getUsableGroups(CurrentUser currentUser) {
    final String name = currentUser.getName();
    return getGroupRepository().findByOwnersIsContainingOrMembersIsContaining(name, name)
        .concatWith(getGroupLdapRepository().findByMembersIsContaining(name))
        .sort(COMPARATOR);
  }

  @Override
  public Flux<Group> getMembership() {
    return manyWithCurrentUser(currentUser -> getMembership(currentUser).map(this::mapToGroup));
  }

  private Flux<GroupEntity> getMembership(CurrentUser currentUser) {
    final String name = currentUser.getName();
    if (currentUser.isLocalUser()) {
      return getGroupRepository().findByMembersIsContaining(name)
          .concatWith(getGroupLdapRepository().findByMembersIsContaining(name))
          .sort(COMPARATOR);
    }
    return getGroupRepository().findByMembersIsContaining(name).sort(COMPARATOR);
  }

  @Override
  public Mono<Set<String>> getMembershipIds() {
    return oneWithCurrentUser(currentUser -> getMembership(currentUser)
        .map(GroupEntity::getId).collect(Collectors.toSet()));
  }

}
