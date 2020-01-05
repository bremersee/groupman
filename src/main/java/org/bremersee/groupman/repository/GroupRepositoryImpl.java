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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

/**
 * The custom group repository implementation.
 *
 * @author Christian Bremer
 */
@Slf4j
public class GroupRepositoryImpl implements GroupRepositoryCustom {
  
  private MongoTemplate mongoTemplate;

  private MongoMappingContext mongoMappingContext;

	/**
	 * Instantiates a new custom group repository.
	 *
	 * @param mongoTemplate       the mongo template
	 * @param mongoMappingContext the mongo mapping context
	 */
	public GroupRepositoryImpl(MongoTemplate mongoTemplate,
      MongoMappingContext mongoMappingContext) {
    this.mongoTemplate = mongoTemplate;
    this.mongoMappingContext = mongoMappingContext;
  }

	/**
	 * Init indices after startup.
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void initIndicesAfterStartup() {
	    log.info("Creating mongo index operations.");
	    IndexOperations indexOps = mongoTemplate.indexOps(GroupEntity.class);
	    IndexResolver resolver = new MongoPersistentEntityIndexResolver(mongoMappingContext);
	    resolver.resolveIndexFor(GroupEntity.class).forEach(indexOps::ensureIndex);
	}
  
}
