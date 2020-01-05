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

package org.bremersee.groupman.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Mono;

/**
 * The custom group repository implementation.
 *
 * @author Christian Bremer
 */
@Slf4j
public class GroupRepositoryImpl implements GroupRepositoryCustom {

  private ReactiveMongoTemplate mongoTemplate;

  /**
   * Instantiates a new custom group repository.
   *
   * @param mongoTemplate the mongo template
   */
  public GroupRepositoryImpl(ReactiveMongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  @Override
  public Mono<Long> countOwnedGroups(String userName) {
    return mongoTemplate
        .count(Query.query(Criteria.where("owners").is(userName)), GroupEntity.class);
  }

  @Override
  public Mono<Long> countMembership(String userName) {
    return mongoTemplate
        .count(Query.query(Criteria.where("members").is(userName)), GroupEntity.class);
  }

}
