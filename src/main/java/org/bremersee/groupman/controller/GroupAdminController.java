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
import lombok.extern.slf4j.Slf4j;
import org.bremersee.exception.ServiceException;
import org.bremersee.groupman.api.GroupAdminControllerApi;
import org.bremersee.groupman.model.Group;
import org.bremersee.groupman.model.Source;
import org.bremersee.groupman.repository.GroupRepository;
import org.bremersee.groupman.repository.ldap.GroupLdapRepository;
import org.bremersee.security.authentication.BremerseeAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The group controller for administration purposes.
 *
 * @author Christian Bremer
 */
@RestController
@Slf4j
public class GroupAdminController
    extends AbstractGroupController
    implements GroupAdminControllerApi {

  /**
   * Instantiates a new group admin controller.
   *
   * @param groupRepository     the group repository
   * @param groupLdapRepository the group ldap repository
   */
  public GroupAdminController(
      GroupRepository groupRepository,
      GroupLdapRepository groupLdapRepository) {
    super(groupRepository, groupLdapRepository);
  }

  @Override
  public Flux<Group> findGroups() {
    return getGroupRepository()
        .findAll()
        .concatWith(getGroupLdapRepository().findAll())
        .sort(COMPARATOR)
        .map(this::mapToGroup);
  }

  @Override
  public Mono<Group> addGroup(Group group) {

    group.setId(null);
    group.setCreatedAt(OffsetDateTime.now(ZoneId.of("UTC")));
    group.setModifiedAt(group.getCreatedAt());
    if (group.getSource() == null) {
      group.setSource(Source.INTERNAL);
    } else if (Source.LDAP.equals(group.getSource())) {
      UnsupportedOperationException e = new UnsupportedOperationException(
          "Creating a group with source 'LDAP' is not supported.");
      log.error("Creating group [" + group.getName() + "] failed.", e);
      throw e;
    }

    return ReactiveSecurityContextHolder
        .getContext()
        .map(SecurityContext::getAuthentication)
        .cast(BremerseeAuthenticationToken.class)
        .map(BremerseeAuthenticationToken::getPreferredName)
        .flatMap(currentUserName -> {
          if (!StringUtils.hasText(group.getCreatedBy())) {
            group.setCreatedBy(currentUserName);
          }
          return getGroupRepository().save(mapToGroupEntity(group));
        })
        .map(this::mapToGroup);
  }

  @Override
  public Mono<Group> findGroupById(String groupId) {
    return super.getGroupEntityById(groupId)
        .map(this::mapToGroup);
  }

  @Override
  public Mono<Group> modifyGroup(String groupId, Group group) {

    if (Source.LDAP.equals(group.getSource())) {
      UnsupportedOperationException e = new UnsupportedOperationException(
          "Updating a group with source 'LDAP' is not supported.");
      log.error("Updating group [" + group.getName() + "] failed.", e);
      throw e;
    }
    return getGroupRepository().findById(groupId)
        .switchIfEmpty(Mono.error(() -> ServiceException.notFound("Group", groupId)))
        .map(existingGroup -> updateGroup(group, () -> existingGroup))
        .map(existingGroup -> {
          if (StringUtils.hasText(group.getCreatedBy())) {
            existingGroup.setCreatedBy(group.getCreatedBy());
          }
          return existingGroup;
        })
        .flatMap(existingGroup -> getGroupRepository().save(existingGroup))
        .map(this::mapToGroup);
  }

  @Override
  public Mono<Void> removeGroup(String groupId) {

    return getGroupRepository().deleteById(groupId);
  }

  @Override
  public Flux<Group> findGroupsByIds(List<String> ids) {
    return super.getGroupEntitiesByIds(ids)
        .map(this::mapToGroup);
  }

}