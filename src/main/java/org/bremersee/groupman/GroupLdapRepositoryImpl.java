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

import static org.bremersee.exception.ServiceException.internalServerError;
import static org.bremersee.groupman.LdapEntryUtils.getAttributeValue;
import static org.bremersee.groupman.LdapEntryUtils.whenTimeToDate;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.exception.ServiceException;
import org.bremersee.groupman.model.Source;
import org.ldaptive.Connection;
import org.ldaptive.ConnectionFactory;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.SearchFilter;
import org.ldaptive.SearchOperation;
import org.ldaptive.SearchRequest;
import org.ldaptive.SearchResult;
import org.ldaptive.SearchScope;
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

  private static final String WHEN_CREATED = "whenCreated";

  private static final String WHEN_CHANGED = "whenChanged";

  private final DomainControllerSettings properties;

  private final ConnectionFactory connectionFactory;

  /**
   * Instantiates a new group ldap repository.
   *
   * @param properties        the properties
   * @param connectionFactory the connection factory
   */
  public GroupLdapRepositoryImpl(
      DomainControllerSettings properties,
      ConnectionFactory connectionFactory) {
    this.properties = properties;
    this.connectionFactory = connectionFactory;
  }

  private GroupEntity mapLdapEntryToGroupEntity(
      @NotNull final LdapEntry ldapEntry,
      @NotNull final Connection conn) {
    final GroupEntity group = new GroupEntity();
    group.setCreatedAt(
        whenTimeToDate(getAttributeValue(ldapEntry, WHEN_CREATED, null)));
    group.setCreatedBy(properties.getAdminName());
    group.setDescription(
        LdapEntryUtils
            .getAttributeValue(ldapEntry, properties.getGroupDescriptionAttribute(), null));
    group.setId(
        LdapEntryUtils.getAttributeValue(ldapEntry, properties.getGroupNameAttribute(), null));
    group.setModifiedAt(
        whenTimeToDate(getAttributeValue(ldapEntry, WHEN_CHANGED, null)));
    group.setName(getAttributeValue(ldapEntry, properties.getGroupNameAttribute(), null));
    group.setOwners(Collections.singleton(properties.getAdminName()));
    group.setSource(Source.LDAP);
    group.setVersion(1L);
    LdapAttribute membersAttr = ldapEntry.getAttribute(properties.getGroupMemberAttr());
    if (membersAttr != null && membersAttr.getStringValues() != null) {
      group.setMembers(
          membersAttr
              .getStringValues()
              .stream()
              .map(member -> getMemberName(member, conn))
              .filter(Objects::nonNull)
              .collect(Collectors.toSet()));
    }
    return group;
  }

  private String getMemberName(
      final String memberValue, @NotNull final Connection conn) {
    if (memberValue == null || !properties.isMemberDn()) {
      return memberValue;
    }
    if (properties.getMemberNameAttribute().equals(properties.getUserRdn())) {
      int a = memberValue.indexOf('=');
      int b = memberValue.indexOf(',');
      if (a > -1 && a < b) {
        return memberValue.substring(a + 1, b);
      }
    }
    try {
      final SearchRequest searchRequest = SearchRequest.newObjectScopeSearchRequest(
          memberValue, new String[]{properties.getMemberNameAttribute()});
      final SearchResult result = new SearchOperation(conn).execute(searchRequest).getResult();
      final LdapEntry ldapEntry = result.getEntry();
      if (ldapEntry != null) {
        return LdapEntryUtils
            .getAttributeValue(result.getEntry(), properties.getMemberNameAttribute(), null);
      }
      log.warn("msg=[Member was not found. Returning null.] member=[{}]", memberValue);
      return null;

    } catch (LdapException e) {
      final ServiceException se = internalServerError(
          "Getting member [" + memberValue + "] failed.",
          e);
      log.error("msg=[Getting member name.", se);
      throw se;
    }
  }

  private Connection getConnection() throws LdapException {
    final Connection c = this.connectionFactory.getConnection();
    if (!c.isOpen()) {
      c.open();
    }
    return c;
  }

  private SearchResult buildWithSearchFilter(
      SearchFilter sf,
      String baseDn,
      SearchScope searchScope,
      Connection conn) throws LdapException {
    final SearchRequest sr = new SearchRequest(baseDn, sf);
    sr.setSearchScope(searchScope);
    final SearchOperation so = new SearchOperation(conn);
    return so.execute(sr).getResult();
  }

  private Mono<LdapEntry> findGroupByName(
      final String groupName,
      final Connection conn) throws LdapException {

    if (!StringUtils.hasText(groupName)) {
      return Mono.empty();
    }
    final SearchFilter sf = new SearchFilter(properties.getGroupFindOneFilter());
    sf.setParameters(new Object[]{groupName});
    final SearchResult searchResult = buildWithSearchFilter(
        sf, properties.getGroupBaseDn(), properties.getGroupSearchScope(), conn);
    final LdapEntry ldapEntry = searchResult.getEntry();
    return ldapEntry == null ? Mono.empty() : Mono.just(ldapEntry);
  }

  @SuppressWarnings("Duplicates")
  private Flux<LdapEntry> findGroupsByNames(
      final Collection<String> groupNames,
      final boolean returnNoEntriesIfGroupNamesIsEmpty,
      final Connection conn) throws LdapException {

    if (returnNoEntriesIfGroupNamesIsEmpty && (groupNames == null || groupNames.isEmpty())) {
      return Flux.empty();
    }
    final Set<String> names = groupNames != null ? new HashSet<>(groupNames) : new HashSet<>();
    final SearchFilter sf = new SearchFilter(properties.getGroupFindByNamesFilter(names.size()));
    if (!names.isEmpty()) {
      sf.setParameters(names.toArray(new String[0]));
    }
    final SearchResult searchResult = buildWithSearchFilter(
        sf, properties.getGroupBaseDn(), properties.getGroupSearchScope(), conn);
    final Collection<LdapEntry> entries = searchResult.getEntries();
    return entries == null ? Flux.empty() : Flux.just(entries.toArray(new LdapEntry[0]));
  }

  @SuppressWarnings("Duplicates")
  private Flux<LdapEntry> findGroupsByMember(
      final String member,
      final Connection conn) throws LdapException {

    log.debug("msg=[Find groups by member.] member=[{}]", member);
    if (!StringUtils.hasText(member)) {
      return Flux.empty();
    }
    log.debug("msg=[Find groups by member with filter and baseDn.] filter=[{}], baseDn=[{}]",
        properties.getGroupFindByMemberContainsFilter(), properties.getGroupBaseDn());
    final SearchFilter sf = new SearchFilter(properties.getGroupFindByMemberContainsFilter());
    if (properties.isMemberDn()) {
      final String userDn = LdapEntryUtils.createDn(
          properties.getUserRdn(), member, properties.getUserBaseDn());
      log.debug("msg=[Find groups by member.] userDn=[{}]", userDn);
      sf.setParameters(new String[]{userDn});
    } else {
      sf.setParameters(new String[]{member});
    }
    final SearchResult searchResult = buildWithSearchFilter(
        sf, properties.getGroupBaseDn(), properties.getGroupSearchScope(), conn);
    final Collection<LdapEntry> entries = searchResult.getEntries();
    return entries == null ? Flux.empty() : Flux.just(entries.toArray(new LdapEntry[0]));
  }

  @Override
  public Mono<Long> count() {
    log.info("msg=[Counting groups]");
    return findAll().collect(Collectors.counting());
  }

  @Override
  public Flux<GroupEntity> findAll() {
    log.info("msg=[Getting all groups]");
    try (final Connection conn = getConnection()) {
      return findGroupsByNames(null, false, conn)
          .map(ldapEntry -> mapLdapEntryToGroupEntity(ldapEntry, conn))
          .filter(
              groupEntity -> !properties.getIgnoredLdapGroups().contains(groupEntity.getName()));

    } catch (final LdapException e) {
      final ServiceException se = internalServerError(
          "Getting all groups failed.",
          e);
      log.error("msg=[Getting all groups.]", se);
      throw se;
    }
  }

  @Override
  public Mono<GroupEntity> findByName(String name) {

    log.info("msg=[Getting group by name] name=[{}]", name);
    try (final Connection conn = getConnection()) {
      return findGroupByName(name, conn)
          .map(ldapEntry -> mapLdapEntryToGroupEntity(ldapEntry, conn))
          .filter(
              groupEntity -> !properties.getIgnoredLdapGroups().contains(groupEntity.getName()));

    } catch (final LdapException e) {
      final ServiceException se = internalServerError(
          "Getting group by name failed.",
          e);
      log.error("msg=[Getting group by name.]", se);
      throw se;
    }
  }

  @Override
  public Flux<GroupEntity> findByNameIn(List<String> names) {
    log.info("msg=[Getting groups by names] names=[{}]", names);
    try (final Connection conn = getConnection()) {
      return findGroupsByNames(names, true, conn)
          .map(ldapEntry -> mapLdapEntryToGroupEntity(ldapEntry, conn))
          .filter(
              groupEntity -> !properties.getIgnoredLdapGroups().contains(groupEntity.getName()));

    } catch (final LdapException e) {
      final ServiceException se = internalServerError(
          "Getting groups by names failed.",
          e);
      log.error("msg=[Getting groups by names.]", se);
      throw se;
    }
  }

  @Override
  public Flux<GroupEntity> findByMembersIsContaining(String name) {

    log.info("msg=[Getting groups by member contains name] name=[{}]", name);
    try (final Connection conn = getConnection()) {
      return findGroupsByMember(name, conn)
          .map(ldapEntry -> mapLdapEntryToGroupEntity(ldapEntry, conn))
          .filter(
              groupEntity -> !properties.getIgnoredLdapGroups().contains(groupEntity.getName()));

    } catch (final LdapException e) {
      final ServiceException se = internalServerError(
          "Getting groups by names failed.",
          e);
      log.error("msg=[Getting groups by names.]", se);
      throw se;
    }
  }

}
