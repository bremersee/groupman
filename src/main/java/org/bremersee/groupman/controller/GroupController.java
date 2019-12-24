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

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.exception.ServiceException;
import org.bremersee.groupman.api.GroupControllerApi;
import org.bremersee.groupman.model.Group;
import org.bremersee.groupman.model.Source;
import org.bremersee.groupman.repository.GroupEntity;
import org.bremersee.groupman.repository.GroupRepository;
import org.bremersee.groupman.repository.ldap.GroupLdapRepository;
import org.bremersee.security.authentication.BremerseeAuthenticationToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.util.StringUtils;
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
    implements GroupControllerApi {

  private final GrantedAuthority localRole;

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
    super(groupRepository, groupLdapRepository);
    this.localRole = StringUtils.hasText(localRole)
        ? new SimpleGrantedAuthority(localRole)
        : null;
  }

  private boolean isLocalUser(BremerseeAuthenticationToken token) {
    return localRole != null && token.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch(roleName -> roleName.equalsIgnoreCase(localRole.getAuthority()));
  }

  @Override
  public Mono<Group> createGroup(Group group) {

    return ReactiveSecurityContextHolder
        .getContext()
        .map(SecurityContext::getAuthentication)
        .cast(BremerseeAuthenticationToken.class)
        .map(BremerseeAuthenticationToken::getPreferredName)
        .flatMap(currentUserName -> {
          group.setId(null);
          group.setCreatedAt(OffsetDateTime.now(ZoneId.of("UTC")));
          group.setModifiedAt(group.getCreatedAt());
          group.setCreatedBy(currentUserName);
          group.setSource(Source.INTERNAL);
          group.getOwners().add(currentUserName);
          return getGroupRepository().save(mapToGroupEntity(group));
        })
        .map(this::mapToGroup);
  }

  @Override
  public Mono<Group> getGroupById(String groupId) {
    return super.getGroupEntityById(groupId)
        .map(this::mapToGroup);
  }

  @Override
  public Mono<Group> updateGroup(String groupId, Group group) {

    return Mono.zip(
        ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .cast(BremerseeAuthenticationToken.class)
            .map(BremerseeAuthenticationToken::getPreferredName),
        getGroupRepository().findById(groupId)
            .switchIfEmpty(Mono.error(() -> ServiceException.notFound("Group", groupId))))
        .flatMap(
            userNameAndGroupEntity -> {
              String currentUserName = userNameAndGroupEntity.getT1();
              GroupEntity existingGroup = userNameAndGroupEntity.getT2();
              if (existingGroup.getOwners().contains(currentUserName)) {
                return getGroupRepository().save(updateGroup(group, () -> existingGroup));
              } else {
                return Mono.empty();
              }
            })
        .switchIfEmpty(Mono.error(() -> ServiceException.forbidden("Group", groupId)))
        .map(this::mapToGroup);
  }

  @Override
  public Mono<Void> deleteGroup(String groupId) {

    return Mono.zip(
        ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .cast(BremerseeAuthenticationToken.class)
            .map(BremerseeAuthenticationToken::getPreferredName),
        getGroupRepository().findById(groupId)
            .switchIfEmpty(Mono.error(() -> ServiceException.notFound("Group", groupId))))
        .flatMap(
            userNameAndGroupEntity -> {
              String currentUserName = userNameAndGroupEntity.getT1();
              GroupEntity existingGroup = userNameAndGroupEntity.getT2();
              if (existingGroup.getOwners().contains(currentUserName)) {
                return getGroupRepository().delete(existingGroup);
              } else {
                return Mono.error(() -> ServiceException.forbidden("Group", groupId));
              }
            });
  }

  @Override
  public Flux<Group> getGroupsByIds(List<String> ids) {
    return super.getGroupEntitiesByIds(ids)
        .map(this::mapToGroup);
  }

  @Override
  public Flux<Group> getEditableGroups() {
    return ReactiveSecurityContextHolder
        .getContext()
        .map(SecurityContext::getAuthentication)
        .cast(BremerseeAuthenticationToken.class)
        .map(BremerseeAuthenticationToken::getPreferredName)
        .flatMapMany(currentUserName -> getGroupRepository()
            .findByOwnersIsContaining(currentUserName, SORT))
        .map(this::mapToGroup);
  }

  @Override
  public Flux<Group> getUsableGroups() {
    return ReactiveSecurityContextHolder
        .getContext()
        .map(SecurityContext::getAuthentication)
        .cast(BremerseeAuthenticationToken.class)
        .flatMapMany(this::getUsableGroups)
        .sort(COMPARATOR)
        .map(this::mapToGroup);
  }

  private Flux<GroupEntity> getUsableGroups(BremerseeAuthenticationToken token) {
    if (isLocalUser(token)) {
      return getGroupRepository().findByOwnersIsContainingOrMembersIsContaining(
          token.getPreferredName(), token.getPreferredName())
          .concatWith(getGroupLdapRepository().findByMembersIsContaining(token.getPreferredName()));
    }
    return getGroupRepository().findByOwnersIsContainingOrMembersIsContaining(
        token.getPreferredName(), token.getPreferredName());
  }

  @Override
  public Flux<Group> getMembership() {
    return ReactiveSecurityContextHolder
        .getContext()
        .map(SecurityContext::getAuthentication)
        .cast(BremerseeAuthenticationToken.class)
        .flatMapMany(this::getMembership)
        .sort(COMPARATOR)
        .map(this::mapToGroup);
  }

  private Flux<GroupEntity> getMembership(BremerseeAuthenticationToken token) {
    if (isLocalUser(token)) {
      return getGroupRepository().findByMembersIsContaining(token.getPreferredName())
          .concatWith(getGroupLdapRepository().findByMembersIsContaining(token.getPreferredName()));
    }
    return getGroupRepository().findByMembersIsContaining(token.getPreferredName());
  }

  @Override
  public Mono<Set<String>> getMembershipIds() {
    return ReactiveSecurityContextHolder
        .getContext()
        .map(SecurityContext::getAuthentication)
        .cast(BremerseeAuthenticationToken.class)
        .flatMapMany(this::getMembership)
        .map(GroupEntity::getId).collect(Collectors.toSet());
  }

}
