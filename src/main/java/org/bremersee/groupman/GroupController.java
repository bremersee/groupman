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
import org.springframework.security.core.Authentication;
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
@RequestMapping(path = "/api/groups")
@Slf4j
public class GroupController extends AbstractGroupController {

  private static final ResponseEntity<Group> FORBIDDEN = new ResponseEntity<>(HttpStatus.FORBIDDEN);

  @Autowired
  public GroupController(GroupRepository groupRepository) {
    super(groupRepository);
  }

  @PostMapping(
      produces = {MediaType.APPLICATION_JSON_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE})
  public Mono<Group> createGroup(
      @Valid @RequestBody Group group,
      final Authentication authentication) {

    final String currentUserName = getCurrentUserName(authentication);
    group.setId(null);
    group.setCreatedAt(new Date());
    group.setCreatedBy(currentUserName);
    group.getOwners().add(currentUserName);
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
      @Valid @RequestBody Group group,
      final Authentication authentication) {

    final String currentUserName = getCurrentUserName(authentication);
    return groupRepository.findById(groupId)
        .flatMap(existingGroup -> {
          if (existingGroup.getOwners().contains(currentUserName)) {
            existingGroup.setDescription(group.getDescription());
            existingGroup.setMembers(group.getMembers());
            existingGroup.setName(group.getName());
            existingGroup.setOwners(group.getOwners());
            existingGroup.getOwners().add(currentUserName);
            return groupRepository.save(existingGroup);
          } else {
            return Mono.empty();
          }
        })
        .map(ResponseEntity::ok)
        .defaultIfEmpty(FORBIDDEN);
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<Void>> deleteGroup(
      @PathVariable(value = "id") String groupId,
      final Authentication authentication) {

    return groupRepository.findById(groupId)
        .flatMap(existingGroup -> {
          if (existingGroup.getOwners().contains(getCurrentUserName(authentication))) {
            return groupRepository.delete(existingGroup);
          } else {
            return Mono.empty();
          }
        })
        .map(x -> new ResponseEntity<Void>(HttpStatus.OK))
        .defaultIfEmpty(new ResponseEntity<>(HttpStatus.FORBIDDEN));
  }

  /*
  private String getCurrentUserName(final Authentication authentication) {

    WebClient webClient = WebClient
        .builder()
        .baseUrl("")
        .filter(new ExchangeFilterFunction() {
          @Override
          public Mono<ClientResponse> filter(ClientRequest clientRequest,
              ExchangeFunction exchangeFunction) {
            clientRequest.headers().add("", "");
            return exchangeFunction.exchange(clientRequest);
          }
        })
        .build();

    webClient
        .method(HttpMethod.GET)
        .uri("/api/groupman")
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToFlux(Group.class);

    webClient
        .method(HttpMethod.GET)
        .uri("/api/groupman")
        .accept(MediaType.APPLICATION_JSON)
        .exchange()
        .flatMap(clientResponse -> clientResponse.bodyToMono(Group.class));
  }
    */

  @GetMapping(path = "/f", produces = {MediaType.APPLICATION_JSON_VALUE})
  public Flux<Group> getGroupsByIds(
      @RequestParam(value = "id", required = false) List<String> ids) {
    return super.getGroupsByIds(ids);
  }

  @GetMapping(path = "/f/editable", produces = {MediaType.APPLICATION_JSON_VALUE})
  public Flux<Group> getEditableGroups(final Authentication authentication) {
    return groupRepository.findByOwnersIsContaining(getCurrentUserName(authentication), SORT);
  }

  @GetMapping(path = "/f/useable", produces = {MediaType.APPLICATION_JSON_VALUE})
  public Flux<Group> getUseableGroups(final Authentication authentication) {
    final String currentUserName = getCurrentUserName(authentication);
    return groupRepository.findByOwnersIsContainingOrMembersIsContaining(
        currentUserName, currentUserName, SORT);
  }

  @GetMapping(path = "/f/membership", produces = {MediaType.APPLICATION_JSON_VALUE})
  public Flux<Group> getMembership(final Authentication authentication) {
    return groupRepository.findByMembersIsContaining(getCurrentUserName(authentication), SORT);
  }

  @GetMapping(path = "/f/membership-ids", produces = {MediaType.APPLICATION_JSON_VALUE})
  public Mono<List<String>> getMembershipIds(final Authentication authentication) {
    return groupRepository
        .findByMembersIsContaining(getCurrentUserName(authentication))
        .map(Group::getId).collectList();
  }

}
