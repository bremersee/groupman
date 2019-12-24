package org.bremersee.groupman.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.IndexResolver;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@Slf4j
public class GroupRepositoryImpl implements GroupRepositoryCustom {
  
  private MongoTemplate mongoTemplate;

  private MongoMappingContext mongoMappingContext;

  public GroupRepositoryImpl(MongoTemplate mongoTemplate,
      MongoMappingContext mongoMappingContext) {
    this.mongoTemplate = mongoTemplate;
    this.mongoMappingContext = mongoMappingContext;
  }
  
	@EventListener(ApplicationReadyEvent.class)
	public void initIndicesAfterStartup() {
	    log.info("Creating mongo index operations.");
	    IndexOperations indexOps = mongoTemplate.indexOps(GroupEntity.class);
	    IndexResolver resolver = new MongoPersistentEntityIndexResolver(mongoMappingContext);
	    resolver.resolveIndexFor(GroupEntity.class).forEach(indexOps::ensureIndex);
	}
  
}
