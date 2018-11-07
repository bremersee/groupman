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

import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Christian Bremer
 */
abstract class AbstractGroupController {

  static final ResponseEntity<Group> NOT_FOUND = new ResponseEntity<>(HttpStatus.NOT_FOUND);

  static final Sort SORT = Sort.by("name", "createdBy");

  @Getter(AccessLevel.PROTECTED)
  GroupRepository groupRepository;

  public AbstractGroupController(GroupRepository groupRepository) {
    this.groupRepository = groupRepository;
  }

  String getCurrentUserName(final Authentication authentication) {
    final String name = authentication == null ? null : authentication.getName();
    return name != null ? name : "guest";
  }

  Mono<ResponseEntity<Group>> getGroupById(final String groupId) {
    return groupRepository.findById(groupId)
        .map(ResponseEntity::ok)
        .defaultIfEmpty(NOT_FOUND);
  }

  Flux<Group> getGroupsByIds(final List<String> ids) {
    return groupRepository.findByIdIn(ids == null ? Collections.emptyList() : ids, SORT);
  }

  /*
  Mono<Group> createGroup(
      final Group group,
      final Authentication authentication) {

    final String currentUserName = getCurrentUserName(authentication);
    group.setId(null);
    group.setCreatedAt(new Date());
    group.setCreatedBy(currentUserName);
    if (!isAdmin(authentication)) {
      group.getOwners().add(currentUserName);
    }
    return groupRepository.save(group);
  }
  */
}
