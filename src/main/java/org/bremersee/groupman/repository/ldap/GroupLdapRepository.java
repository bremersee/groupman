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

import java.util.List;
import org.bremersee.groupman.repository.GroupEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The group ldap repository.
 *
 * @author Christian Bremer
 */
public interface GroupLdapRepository {

  /**
   * Count groups.
   *
   * @return the group size
   */
  default Mono<Long> count() {
    return Mono.just(0L);
  }

  /**
   * Find all groups.
   *
   * @return the flux
   */
  default Flux<GroupEntity> findAll() {
    return Flux.empty();
  }

  /**
   * Find group by name.
   *
   * @param name the name
   * @return the group
   */
  default Mono<GroupEntity> findByName(String name) {
    return Mono.empty();
  }

  /**
   * Find groups with the specified names.
   *
   * @param names the names
   * @return the groups
   */
  default Flux<GroupEntity> findByNameIn(List<String> names) {
    return Flux.empty();
  }

  /**
   * Find all groups with the specified member.
   *
   * @param name the name
   * @return the flux
   */
  default Flux<GroupEntity> findByMembersIsContaining(String name) {
    return Flux.empty();
  }

  /**
   * Count ldap membership.
   *
   * @param name the user name
   * @return the size of membership
   */
  default Mono<Long> countMembership(String name) {
    return findByMembersIsContaining(name).count();
  }

}
