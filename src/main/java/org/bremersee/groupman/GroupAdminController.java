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

import java.util.Date;
import java.util.List;
import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Christian Bremer
 */
@RestController
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequestMapping(path = "/api/admin/groups")
@Slf4j
public class GroupAdminController extends AbstractGroupController {

  private static final ResponseEntity<Group> FORBIDDEN = new ResponseEntity<>(HttpStatus.FORBIDDEN);

  @Autowired
  public GroupAdminController(GroupRepository groupRepository) {
    super(groupRepository);
  }

  @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
  public Flux<Group> getGroups() {
    return groupRepository.findAll(SORT);
  }

  @PostMapping(
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  public Mono<Group> createGroup(
      @Valid @RequestBody Group group,
      final Authentication authentication) {

    group.setId(null);
    group.setCreatedAt(new Date());
    if (!StringUtils.hasText(group.getCreatedBy())) {
      group.setCreatedBy(getCurrentUserName(authentication));
    }
    return groupRepository.save(group);
  }

  @GetMapping(path = "/{id}", produces = {MediaType.APPLICATION_JSON_VALUE})
  public Mono<ResponseEntity<Group>> getGroupById(@PathVariable(value = "id") String groupId) {
    return super.getGroupById(groupId);
  }

  @PutMapping(path = "/{id}",
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  public Mono<ResponseEntity<Group>> updateGroup(
      @PathVariable(value = "id") String groupId,
      @Valid @RequestBody Group group) {

    return groupRepository.findById(groupId)
        .flatMap(existingGroup -> {
          existingGroup.setDescription(group.getDescription());
          existingGroup.setMembers(group.getMembers());
          existingGroup.setName(group.getName());
          existingGroup.setOwners(group.getOwners());
          if (StringUtils.hasText(group.getCreatedBy())) {
            existingGroup.setCreatedBy(group.getCreatedBy());
          }
          return groupRepository.save(existingGroup);
        })
        .map(ResponseEntity::ok)
        .defaultIfEmpty(FORBIDDEN);
  }

  @DeleteMapping("/{id}")
  public Mono<Void> deleteGroup(
      @PathVariable(value = "id") String groupId) {

    return groupRepository.deleteById(groupId);
  }

  @GetMapping(path = "/f", produces = {MediaType.APPLICATION_JSON_VALUE})
  public Flux<Group> getGroupsByIds(
      @RequestParam(value = "id", required = false) List<String> ids) {
    return super.getGroupsByIds(ids);
  }

}
