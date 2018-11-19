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

package org.bremersee.groupman;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

/**
 * @author Christian Bremer
 */
public interface GroupRepository extends ReactiveMongoRepository<GroupEntity, String> {

  Flux<GroupEntity> findByOwnersIsContaining(String owner, Sort sort);

  Flux<GroupEntity> findByOwnersIsContainingOrMembersIsContaining(
      String owner,
      String member,
      Sort sort);

  Flux<GroupEntity> findByMembersIsContaining(String member, Sort sort);

  Flux<GroupEntity> findByIdIn(List<String> ids, Sort sort);

}
