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

package org.bremersee.groupman.repository.ldap;

import static org.bremersee.data.ldaptive.LdaptiveEntryMapper.createDn;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.data.ldaptive.LdaptiveTemplate;
import org.bremersee.groupman.config.DomainControllerProperties;
import org.bremersee.groupman.repository.GroupEntity;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
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

  private DomainControllerProperties properties;

  private LdaptiveTemplate ldaptiveTemplate;

  private GroupLdapMapper mapper;

  /**
   * Instantiates a new group ldap repository.
   *
   * @param properties       the properties
   * @param ldaptiveTemplate the ldap template
   */
  public GroupLdapRepositoryImpl(
      DomainControllerProperties properties,
      LdaptiveTemplate ldaptiveTemplate) {
    this.properties = properties;
    this.ldaptiveTemplate = ldaptiveTemplate;
    this.mapper = new GroupLdapMapper(properties);
  }

  @Override
  public Mono<Long> count() {
    return findAll().collect(Collectors.counting());
  }

  @Override
  public Flux<GroupEntity> findAll() {
    final SearchRequest searchRequest = new SearchRequest(
        properties.getGroupBaseDn(),
        new SearchFilter(properties.getGroupFindAllFilter()));
    searchRequest.setSearchScope(properties.getGroupSearchScope());
    return Flux.fromStream(ldaptiveTemplate.findAll(searchRequest, mapper)
        .filter(groupEntity -> !properties.getIgnoredLdapGroups().contains(groupEntity.getName())));
  }

  @Override
  public Mono<GroupEntity> findByName(String name) {
    final SearchFilter searchFilter = new SearchFilter(properties.getGroupFindOneFilter());
    searchFilter.setParameter(0, name);
    final SearchRequest searchRequest = new SearchRequest(
        properties.getGroupBaseDn(),
        searchFilter);
    searchRequest.setSearchScope(properties.getGroupSearchScope());
    return ldaptiveTemplate.findOne(searchRequest, mapper)
        .filter(groupEntity -> !properties.getIgnoredLdapGroups().contains(groupEntity.getName()))
        .map(Mono::just)
        .orElse(Mono.empty());
  }

  @Override
  public Flux<GroupEntity> findByNameIn(List<String> groupNames) {
    final Set<String> names = groupNames != null ? new HashSet<>(groupNames) : new HashSet<>();
    if (names.isEmpty()) {
      return Flux.empty();
    }
    final SearchFilter sf = new SearchFilter(properties.getGroupFindByNamesFilter(names.size()));
    if (!names.isEmpty()) {
      sf.setParameters(names.toArray(new String[0]));
    }
    final SearchRequest searchRequest = new SearchRequest(properties.getGroupBaseDn(), sf);
    searchRequest.setSearchScope(properties.getGroupSearchScope());
    return Flux.fromStream(ldaptiveTemplate.findAll(searchRequest, mapper)
        .filter(groupEntity -> !properties.getIgnoredLdapGroups().contains(groupEntity.getName())));
  }

  @Override
  public Flux<GroupEntity> findByMembersIsContaining(String name) {
    if (!StringUtils.hasText(name)) {
      return Flux.empty();
    }
    final SearchFilter sf = new SearchFilter(properties.getGroupFindByMemberContainsFilter());
    if (properties.isMemberDn()) {
      final String userDn = createDn(properties.getUserRdn(), name, properties.getUserBaseDn());
      sf.setParameters(new String[]{userDn});
    } else {
      sf.setParameters(new String[]{name});
    }
    final SearchRequest searchRequest = new SearchRequest(properties.getGroupBaseDn(), sf);
    searchRequest.setSearchScope(properties.getGroupSearchScope());
    return Flux.fromStream(ldaptiveTemplate.findAll(searchRequest, mapper)
        .filter(groupEntity -> !properties.getIgnoredLdapGroups().contains(groupEntity.getName())));
  }

}
