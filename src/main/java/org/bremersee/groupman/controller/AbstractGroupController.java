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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.bremersee.comparator.ComparatorBuilder;
import org.bremersee.comparator.spring.ComparatorSpringUtils;
import org.bremersee.exception.ServiceException;
import org.bremersee.groupman.model.Group;
import org.bremersee.groupman.repository.GroupEntity;
import org.bremersee.groupman.repository.GroupRepository;
import org.bremersee.groupman.repository.ldap.GroupLdapRepository;
import org.modelmapper.AbstractConverter;
import org.modelmapper.ModelMapper;
import org.reactivestreams.Publisher;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The abstract group controller.
 *
 * @author Christian Bremer
 */
abstract class AbstractGroupController {

  /**
   * The default sort order.
   */
  static final Sort SORT = Sort.by("name", "createdBy");

  /**
   * The default comparator.
   */
  static final Comparator<Object> COMPARATOR = ComparatorBuilder.builder()
      .addAll(ComparatorSpringUtils.fromSort(SORT))
      .build();

  @Getter(AccessLevel.PACKAGE)
  private final ModelMapper modelMapper = new ModelMapper();

  @Getter(AccessLevel.PACKAGE)
  private GroupRepository groupRepository;

  @Getter(AccessLevel.PACKAGE)
  private GroupLdapRepository groupLdapRepository;

  private String localUserRole;

  /**
   * Instantiates a new abstract group controller.
   *
   * @param groupRepository     the group repository
   * @param groupLdapRepository the group ldap repository
   * @param localUserRole       the local user role
   */
  public AbstractGroupController(
      final GroupRepository groupRepository,
      final GroupLdapRepository groupLdapRepository,
      final String localUserRole) {

    Assert.notNull(groupRepository, "Group repository must not be null.");
    Assert.notNull(groupLdapRepository, "Group ldap repository must not be null.");
    this.groupRepository = groupRepository;
    this.groupLdapRepository = groupLdapRepository;
    this.localUserRole = localUserRole;
    modelMapper.addConverter(new AbstractConverter<Date, OffsetDateTime>() {
      @Override
      protected OffsetDateTime convert(Date date) {
        return date == null ? null : OffsetDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));
      }
    });
    modelMapper.addConverter(new AbstractConverter<OffsetDateTime, Date>() {
      @Override
      protected Date convert(OffsetDateTime offsetDateTime) {
        return offsetDateTime == null ? null : Date.from(offsetDateTime.toInstant());
      }
    });
  }

  /**
   * One with current user mono.
   *
   * @param <R>      the type parameter
   * @param function the function
   * @return the mono
   */
  <R> Mono<R> oneWithCurrentUser(Function<CurrentUser, ? extends Mono<R>> function) {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .switchIfEmpty(Mono.error(ServiceException::forbidden))
        .map(authentication -> new CurrentUser(authentication, localUserRole))
        .flatMap(function);
  }

  /**
   * Many with current user flux.
   *
   * @param <R>      the type parameter
   * @param function the function
   * @return the flux
   */
  <R> Flux<R> manyWithCurrentUser(Function<CurrentUser, ? extends Publisher<R>> function) {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .switchIfEmpty(Mono.error(ServiceException::forbidden))
        .map(authentication -> new CurrentUser(authentication, localUserRole))
        .flatMapMany(function);
  }

  /**
   * Gets group entity by id.
   *
   * @param groupId the group id
   * @return the group entity
   */
  Mono<GroupEntity> getGroupEntityById(final String groupId) {
    return groupRepository
        .findById(groupId)
        .switchIfEmpty(groupLdapRepository.findByName(groupId))
        .switchIfEmpty(Mono.error(() -> ServiceException.notFound("Group", groupId)));
  }

  /**
   * Gets group entities by ids.
   *
   * @param ids the ids
   * @return the group entities by ids
   */
  Flux<GroupEntity> getGroupEntitiesByIds(final List<String> ids) {
    return groupRepository
        .findByIdIn(ids == null ? Collections.emptyList() : ids)
        .concatWith(groupLdapRepository.findByNameIn(ids))
        .sort(COMPARATOR);
  }

  private Group prepareGroup(final Supplier<Group> groupSupplier) {
    Group group = groupSupplier.get();
    if (group.getMembers() == null) {
      group.setMembers(Collections.emptyList());
    }
    if (group.getOwners() == null) {
      group.setOwners(Collections.emptyList());
    }
    return group;
  }

  /**
   * Map group entity to group representation.
   *
   * @param source the group entity
   * @return the group representation
   */
  Group mapToGroup(final GroupEntity source) {
    Group destination = new Group();
    destination.setMembers(new ArrayList<>());
    destination.setOwners(new ArrayList<>());
    getModelMapper().map(source, destination);
    return destination;
  }

  /**
   * Map to group representation to group entity.
   *
   * @param source the group representation
   * @return the group entity
   */
  GroupEntity mapToGroupEntity(final Group source) {
    GroupEntity destination = new GroupEntity();
    getModelMapper().map(prepareGroup(() -> source), destination);
    return destination;
  }

  /**
   * Update group entity with group representation.
   *
   * @param source              the group representation
   * @param destinationSupplier the group entity supplier
   * @return the group entity
   */
  GroupEntity updateGroup(final Group source, final Supplier<GroupEntity> destinationSupplier) {
    final Group src = prepareGroup(() -> source);
    final GroupEntity destination = destinationSupplier.get();
    if (source.getVersion() != null) {
      destination.setVersion(source.getVersion());
    }
    destination.setModifiedAt(new Date());
    destination.setDescription(src.getDescription());
    destination.setMembers(new LinkedHashSet<>(src.getMembers()));
    destination.setName(src.getName());
    destination.setOwners(new LinkedHashSet<>(src.getOwners()));
    return destination;
  }

  /**
   * The type Current user.
   */
  @Getter
  @ToString
  @EqualsAndHashCode(of = "name")
  public static class CurrentUser {

    private String name;

    private Set<String> roles;

    private boolean localUser;

    /**
     * Instantiates a new Current user.
     *
     * @param authentication the authentication
     * @param localUserRole  the local user role
     */
    public CurrentUser(Authentication authentication, String localUserRole) {
      name = authentication.getName();
      if (authentication.getAuthorities() != null) {
        roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toSet());
      } else {
        roles = Collections.emptySet();
      }
      localUser = localUserRole != null && roles.contains(localUserRole);
    }
  }

}
