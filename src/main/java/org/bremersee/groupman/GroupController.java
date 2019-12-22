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

package org.bremersee.groupman;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.exception.ServiceException;
import org.bremersee.groupman.api.GroupControllerApi;
import org.bremersee.groupman.model.Group;
import org.bremersee.groupman.model.Source;
import org.bremersee.security.authentication.BremerseeAuthenticationToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

/**
 * The group controller.
 *
 * @author Christian Bremer
 */
@RestController
@RequestMapping(path = "/api/groups")
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

  @PostMapping(
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  @Override
  public Mono<Group> createGroup(
      @Valid @RequestBody Group group) {

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

  @GetMapping(path = "/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
  @Override
  public Mono<Group> getGroupById(@PathVariable(value = "id") String groupId) {
    return super.getGroupEntityById(groupId)
        .map(this::mapToGroup);
  }

  @PutMapping(path = "/{id}",
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  @Override
  public Mono<Group> updateGroup(
      @PathVariable(value = "id") String groupId,
      @Valid @RequestBody Group group) {

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

  @DeleteMapping("/{id}")
  @Override
  public Mono<Void> deleteGroup(
      @PathVariable(value = "id") String groupId) {

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

  @GetMapping(path = "/f", produces = {MediaType.APPLICATION_JSON_VALUE})
  @Override
  public Flux<Group> getGroupsByIds(
      @RequestParam(value = "id", required = false) List<String> ids) {
    return super.getGroupEntitiesByIds(ids)
        .map(this::mapToGroup);
  }

  @GetMapping(path = "/f/editable", produces = {MediaType.APPLICATION_JSON_VALUE})
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

  @GetMapping(path = "/f/usable", produces = {MediaType.APPLICATION_JSON_VALUE})
  @Override
  public Flux<Group> getUsableGroups() {
    return ReactiveSecurityContextHolder
        .getContext()
        .map(SecurityContext::getAuthentication)
        .cast(BremerseeAuthenticationToken.class)
        .map(authToken -> Tuples.of(
            authToken.getPreferredName(),
            localRole == null || authToken.getAuthorities().contains(localRole)))
        .flatMapMany(tuple -> getGroupRepository()
            .findByOwnersIsContainingOrMembersIsContaining(tuple.getT1(), tuple.getT1())
            .concatWith(
                tuple.getT2()
                    ? getGroupLdapRepository().findByMembersIsContaining(tuple.getT1())
                    : Flux.empty()))
        .sort(COMPARATOR)
        .map(this::mapToGroup);
  }

  @GetMapping(path = "/f/membership", produces = {MediaType.APPLICATION_JSON_VALUE})
  @Override
  public Flux<Group> getMembership() {
    return ReactiveSecurityContextHolder
        .getContext()
        .map(SecurityContext::getAuthentication)
        .cast(BremerseeAuthenticationToken.class)
        .map(authToken -> Tuples.of(
            authToken.getPreferredName(),
            localRole == null || authToken.getAuthorities().contains(localRole)))
        .flatMapMany(tuple -> getGroupRepository()
            .findByMembersIsContaining(tuple.getT1())
            .concatWith(
                tuple.getT2()
                    ? getGroupLdapRepository().findByMembersIsContaining(tuple.getT1())
                    : Flux.empty()))
        .sort(COMPARATOR)
        .map(this::mapToGroup);
  }

  @GetMapping(path = "/f/membership-ids", produces = {MediaType.APPLICATION_JSON_VALUE})
  @Override
  public Mono<Set<String>> getMembershipIds() {
    return ReactiveSecurityContextHolder
        .getContext()
        .map(SecurityContext::getAuthentication)
        .cast(BremerseeAuthenticationToken.class)
        .map(authToken -> Tuples.of(
            authToken.getPreferredName(),
            localRole == null || authToken.getAuthorities().contains(localRole)))
        .flatMapMany(tuple -> getGroupRepository()
            .findByMembersIsContaining(tuple.getT1())
            .concatWith(
                tuple.getT2()
                    ? getGroupLdapRepository().findByMembersIsContaining(tuple.getT1())
                    : Flux.empty()))
        .map(GroupEntity::getId).collect(Collectors.toSet());
  }

}
