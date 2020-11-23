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

package org.bremersee.groupman.repository.ldap;

import static org.bremersee.data.ldaptive.LdaptiveEntryMapper.createDn;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.data.ldaptive.reactive.ReactiveLdaptiveTemplate;
import org.bremersee.groupman.config.DomainControllerProperties;
import org.bremersee.groupman.repository.GroupEntity;
import org.ldaptive.FilterTemplate;
import org.ldaptive.SearchRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The group ldap repository implementation.
 *
 * @author Christian Bremer
 */
@Profile("ldap")
@Component
@Slf4j
public class GroupLdapRepositoryImpl implements GroupLdapRepository {

  private final DomainControllerProperties properties;

  private final ReactiveLdaptiveTemplate ldaptiveTemplate;

  private final GroupLdapMapper mapper;

  /**
   * Instantiates a new group ldap repository.
   *
   * @param properties the properties
   * @param ldaptiveTemplate the ldap template
   */
  public GroupLdapRepositoryImpl(
      DomainControllerProperties properties,
      ObjectProvider<ReactiveLdaptiveTemplate> ldaptiveTemplate) {
    this.properties = properties;
    this.ldaptiveTemplate = ldaptiveTemplate.getIfAvailable();
    this.mapper = new GroupLdapMapper(properties);
    Assert.notNull(this.ldaptiveTemplate, "Ldaptive template must be present.");
  }

  @Override
  public Mono<Long> count() {
    return findAll().collect(Collectors.counting());
  }

  @Override
  public Flux<GroupEntity> findAll() {
    return ldaptiveTemplate.
        findAll(
            SearchRequest.builder()
                .dn(properties.getGroupBaseDn())
                .filter(properties.getGroupFindAllFilter())
                .scope(properties.getGroupSearchScope())
                .build(),
            mapper)
        .filter(groupEntity -> !properties.getIgnoredLdapGroups().contains(groupEntity.getName()));
  }

  @Override
  public Mono<GroupEntity> findByName(String name) {
    return ldaptiveTemplate
        .findOne(
            SearchRequest.builder()
                .dn(properties.getGroupBaseDn())
                .filter(FilterTemplate.builder()
                    .filter(properties.getGroupFindOneFilter())
                    .parameters(name)
                    .build())
                .scope(properties.getGroupSearchScope())
                .build(),
            mapper)
        .filter(groupEntity -> !properties.getIgnoredLdapGroups().contains(groupEntity.getName()));
  }

  @Override
  public Flux<GroupEntity> findByNameIn(List<String> groupNames) {
    final Set<String> names = groupNames != null ? new HashSet<>(groupNames) : new HashSet<>();
    if (names.isEmpty()) {
      return Flux.empty();
    }
    FilterTemplate filterTemplate = new FilterTemplate();
    filterTemplate.setFilter(properties.getGroupFindByNamesFilter(names.size()));
    int i = 0;
    for (String name : names) {
      filterTemplate.setParameter(i, name);
      i++;
    }
    return ldaptiveTemplate
        .findAll(
            SearchRequest.builder()
                .dn(properties.getGroupBaseDn())
                .filter(filterTemplate)
                .scope(properties.getGroupSearchScope())
                .build(),
            mapper)
        .filter(groupEntity -> !properties.getIgnoredLdapGroups().contains(groupEntity.getName()));
  }

  @Override
  public Flux<GroupEntity> findByMembersIsContaining(String name) {
    if (!StringUtils.hasText(name)) {
      return Flux.empty();
    }
    return ldaptiveTemplate
        .findAll(
            SearchRequest.builder()
                .dn(properties.getGroupBaseDn())
                .filter(FilterTemplate.builder()
                    .filter(properties.getGroupFindByMemberContainsFilter())
                    .parameters(properties.isMemberDn()
                        ? createDn(properties.getUserRdn(), name, properties.getUserBaseDn())
                        : name)
                    .build())
                .scope(properties.getGroupSearchScope())
                .build(),
            mapper)
        .filter(groupEntity -> !properties.getIgnoredLdapGroups().contains(groupEntity.getName()));
  }

}
