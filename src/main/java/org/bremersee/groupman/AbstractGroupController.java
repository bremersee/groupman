/*
 * Copyright 2017 the original author or authors.
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

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import org.bremersee.exception.ServiceException;
import org.bremersee.groupman.model.Group;
import org.modelmapper.AbstractConverter;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Christian Bremer
 */
abstract class AbstractGroupController {

  static final Sort SORT = Sort.by("name", "createdBy");

  @Getter(AccessLevel.PACKAGE)
  private final ModelMapper modelMapper = new ModelMapper();

  @Getter(AccessLevel.PACKAGE)
  private GroupRepository groupRepository;

  public AbstractGroupController(final GroupRepository groupRepository) {
    this.groupRepository = groupRepository;
    modelMapper.addConverter(new AbstractConverter<Date, OffsetDateTime>() {
      @Override
      protected OffsetDateTime convert(Date date) {
        return date == null ? null : OffsetDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));
      }
    });
    modelMapper.addConverter(new AbstractConverter<OffsetDateTime, Date>() {
      @Override
      protected Date convert(OffsetDateTime offsetDateTime) {
        return offsetDateTime == null ? null : Date.from(offsetDateTime.toInstant());
      }
    });
  }

  Mono<GroupEntity> getGroupEntityById(final String groupId) {
    return groupRepository
        .findById(groupId)
        .switchIfEmpty(Mono.error(ServiceException.notFound("Group", groupId)));
  }

  Flux<GroupEntity> getGroupEntitiesByIds(final List<String> ids) {
    return groupRepository.findByIdIn(ids == null ? Collections.emptyList() : ids, SORT);
  }

  Group mapToGroup(final GroupEntity source) {
    Group destination = new Group();
    destination.setMembers(new ArrayList<>());
    destination.setOwners(new ArrayList<>());
    getModelMapper().map(source, destination);
    return destination;
  }

  GroupEntity mapToGroupEntity(final Group source) {
    if (source.getMembers() == null) {
      source.setMembers(Collections.emptyList());
    }
    if (source.getOwners() == null) {
      source.setOwners(Collections.emptyList());
    }
    GroupEntity destination = new GroupEntity();
    getModelMapper().map(source, destination);
    return destination;
  }

}
