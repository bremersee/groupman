/*
 * Copyright 2016 the original author or authors.
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

package org.bremersee.groupman.repository;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

/**
 * The group repository.
 *
 * @author Christian Bremer
 */
public interface GroupRepository
    extends ReactiveMongoRepository<GroupEntity, String>, GroupRepositoryCustom {

  /**
   * Find groups by owner or member.
   *
   * @param owner  the owner
   * @param member the member
   * @return the groups
   */
  Flux<GroupEntity> findByOwnersIsContainingOrMembersIsContaining(
      String owner,
      String member);

  /**
   * Find groups by owner.
   *
   * @param owner the owner
   * @param sort  the sort order
   * @return the groups
   */
  Flux<GroupEntity> findByOwnersIsContaining(String owner, Sort sort);

  /**
   * Find groups by member.
   *
   * @param member the member
   * @return the groups
   */
  Flux<GroupEntity> findByMembersIsContaining(String member);

  /**
   * Find groups with the specified IDs.
   *
   * @param ids  the ids
   * @return the groups
   */
  Flux<GroupEntity> findByIdIn(List<String> ids);

}
