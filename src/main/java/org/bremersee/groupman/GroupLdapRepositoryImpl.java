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
import java.util.HashSet;
import java.util.List;
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

  private GroupEntity mapLdapEntryToGroupEntity(@NotNull final LdapEntry ldapEntry) {
    final GroupEntity group = new GroupEntity();
    group.setCreatedAt(
        whenTimeToDate(getAttributeValue(ldapEntry, WHEN_CREATED, null)));
    group.setCreatedBy(null); // TODO
    group.setDescription(null);
    group.setId(LdapEntryUtils.getAttributeValue(ldapEntry, "sAMAccountName", null));
    group.setModifiedAt(
        whenTimeToDate(getAttributeValue(ldapEntry, WHEN_CHANGED, null)));
    group.setName(getAttributeValue(ldapEntry, "sAMAccountName", null));
    //group.setOwners(null);
    group.setSource(Source.LDAP);
    //group.setVersion(1L);
    LdapAttribute membersAttr = ldapEntry.getAttribute(properties.getGroupMemberAttr());
    if (membersAttr != null && membersAttr.getStringValues() != null) {
      group.setMembers(membersAttr.getStringValues().stream().map(member -> {
        if (properties.isMemberDn()) {
          int a = member.indexOf('=');
          int b = member.indexOf(',');
          if (a > -1 && b > a) {
            return member.substring(a + 1, b);
          }
        }
        return member;
      }).collect(Collectors.toSet()));
    }
    return group;
  }

  private Connection getConnection() throws LdapException {
    final Connection c = this.connectionFactory.getConnection();
    if (!c.isOpen()) {
      c.open();
    }
    return c;
  }

  /**
   * Close the given context and ignore any thrown exception. This is useful for typical finally
   * blocks in manual Ldap statements.
   *
   * @param connection the Ldap connection to close
   */
  private void closeConnection(final Connection connection) {
    if (connection != null && connection.isOpen()) {
      try {
        connection.close();
      } catch (final Exception ex) {
        log.warn("Closing ldap connection failed.", ex);
      }
    }
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
      final Connection conn) throws LdapException {

    if (groupNames == null || groupNames.isEmpty()) {
      return Flux.empty();
    }
    final Set<String> names = new HashSet<>(groupNames);
    final SearchFilter sf = new SearchFilter(properties.getGroupFindByNamesFilter(names.size()));
    sf.setParameters(names.toArray(new String[0]));
    final SearchResult searchResult = buildWithSearchFilter(
        sf, properties.getGroupBaseDn(), properties.getGroupSearchScope(), conn);
    final Collection<LdapEntry> entries = searchResult.getEntries();
    return entries == null ? Flux.empty() : Flux.just(entries.toArray(new LdapEntry[0]));
  }

  private Flux<LdapEntry> findGroupsByMember(
      final String member,
      final Connection conn) throws LdapException {

    if (!StringUtils.hasText(member)) {
      return Flux.empty();
    }
    final SearchFilter sf = new SearchFilter(properties.getGroupFindByMemberContainsFilter());
    sf.setParameters(new Object[]{member});
    final SearchResult searchResult = buildWithSearchFilter(
        sf, properties.getGroupBaseDn(), properties.getGroupSearchScope(), conn);
    final Collection<LdapEntry> entries = searchResult.getEntries();
    return entries == null ? Flux.empty() : Flux.just(entries.toArray(new LdapEntry[0]));
  }

  @Override
  public Mono<GroupEntity> findByName(String name) {

    log.info("msg=[Getting group by name] name=[{}]", name);
    Connection conn = null;
    try {
      conn = getConnection();
      return findGroupByName(name, conn)
          .map(this::mapLdapEntryToGroupEntity)
          .filter(
              groupEntity -> !properties.getIgnoredLdapGroups().contains(groupEntity.getName()));

    } catch (final LdapException e) {
      final ServiceException se = internalServerError(
          "Getting group by name failed.",
          e);
      log.error("msg=[Getting group by name.]", se);
      throw se;

    } finally {
      closeConnection(conn);
    }
  }

  @Override
  public Flux<GroupEntity> findByNameIn(List<String> names) {
    log.info("msg=[Getting groups by names] names=[{}]", names);
    Connection conn = null;
    try {
      conn = getConnection();
      return findGroupsByNames(names, conn)
          .map(this::mapLdapEntryToGroupEntity)
          .filter(
              groupEntity -> !properties.getIgnoredLdapGroups().contains(groupEntity.getName()));

    } catch (final LdapException e) {
      final ServiceException se = internalServerError(
          "Getting groups by names failed.",
          e);
      log.error("msg=[Getting groups by names.]", se);
      throw se;

    } finally {
      closeConnection(conn);
    }
  }

  @Override
  public Flux<GroupEntity> findByMembersIsContaining(String name) {

    log.info("msg=[Getting groups by member contains name] name=[{}]", name);
    Connection conn = null;
    try {
      conn = getConnection();
      return findGroupsByMember(name, conn)
          .map(this::mapLdapEntryToGroupEntity)
          .filter(
              groupEntity -> !properties.getIgnoredLdapGroups().contains(groupEntity.getName()));

    } catch (final LdapException e) {
      final ServiceException se = internalServerError(
          "Getting groups by names failed.",
          e);
      log.error("msg=[Getting groups by names.]", se);
      throw se;

    } finally {
      closeConnection(conn);
    }
  }

}
