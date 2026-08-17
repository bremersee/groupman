/*
 * Copyright 2026 the original author or authors.
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

package org.bremersee.groupman.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.bremersee.exception.model.RestApiException;
import org.bremersee.groupman.model.Group;
import org.bremersee.groupman.model.GroupCreate;
import org.bremersee.groupman.model.GroupUpdate;
import org.bremersee.groupman.model.User;
import org.bremersee.groupman.service.GroupService;
import org.bremersee.spring.security.core.ReactiveAuthenticationOperations;
import org.bremersee.spring.security.core.ReactiveAuthenticationTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
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
 * The group controller.
 *
 * @author Christian Bremer
 */
@RestController
@RequestMapping(path = "/api/groups")
@Validated
public class GroupController {

  private final ReactiveAuthenticationOperations<Authentication> authTemplate;

  private final GroupService groupService;

  /**
   * Instantiates a new group controller.
   *
   * @param groupService the group service
   */
  public GroupController(GroupService groupService) {

    Assert.notNull(groupService, "Group service must not be null.");
    this.authTemplate = new ReactiveAuthenticationTemplate<>();
    this.groupService = groupService;
  }

  /**
   * Create group.
   *
   * @param group the group
   * @return the mono
   */
  @Operation(
      description = "Create group.",
      security = {
          @SecurityRequirement(name = "bearer-jwt")
      }
  )
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "200", description = "OK"),
          @ApiResponse(responseCode = "400", description = "Bad request", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "401", description = "Unauthorized", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "403", description = "Forbidden", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "500", description = "Internal server error", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          })
      }
  )
  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<Group> createGroup(@Valid @RequestBody GroupCreate group) {
    return authTemplate.oneWithAuthentication(auth -> groupService
        .createGroup(auth.getName(), group));
  }

  /**
   * Gets groups.
   *
   * @param search the search
   * @return the groups
   */
  @Operation(
      description = "Get groups.",
      security = {
          @SecurityRequirement(name = "bearer-jwt")
      }
  )
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "200", description = "OK"),
          @ApiResponse(responseCode = "400", description = "Bad request", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "401", description = "Unauthorized", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "403", description = "Forbidden", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "500", description = "Internal server error", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          })
      }
  )
  @GetMapping(
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Flux<Group> getGroups(
      @Parameter(name = "search", description = "A String contained in name or description.")
      @RequestParam(name = "search", required = false) String search,

      @Parameter(name = "first", description = "Pagination offset.")
      @RequestParam(name = "first", required = false) Integer first,

      @Parameter(name = "max", description = "Maximum results size (defaults to 100).")
      @RequestParam(name = "max", required = false) Integer max) {

    return authTemplate.manyWithAuthentication(auth -> groupService
        .getGroups(auth.getName(), search, first, max));
  }

  /**
   * Gets group.
   *
   * @param groupId the group id
   * @return the group
   */
  @Operation(
      description = "Get group.",
      security = {
          @SecurityRequirement(name = "bearer-jwt")
      }
  )
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "200", description = "OK"),
          @ApiResponse(responseCode = "400", description = "Bad request", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "401", description = "Unauthorized", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "403", description = "Forbidden", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "404", description = "Not found", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "500", description = "Internal server error", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          })
      }
  )
  @GetMapping(
      path = "/{groupId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<Group> getGroup(
      @Parameter(name = "groupId", description = "The ID of the group.", required = true)
      @PathVariable String groupId) {

    return authTemplate.oneWithAuthentication(auth -> groupService
        .getGroup(auth.getName(), groupId));
  }

  /**
   * Gets members.
   *
   * @param groupId the group id
   * @param first the first
   * @param max the max
   * @return the members
   */
  @Operation(
      description = "Get members.",
      security = {
          @SecurityRequirement(name = "bearer-jwt")
      }
  )
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "200", description = "OK"),
          @ApiResponse(responseCode = "400", description = "Bad request", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "401", description = "Unauthorized", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "403", description = "Forbidden", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "404", description = "Not found", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "500", description = "Internal server error", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          })
      }
  )
  @GetMapping(
      path = "/{groupId}/members",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Flux<User> getMembers(
      @Parameter(name = "groupId", description = "The ID of the group.", required = true)
      @PathVariable String groupId,

      @Parameter(name = "first", description = "Pagination offset.")
      @RequestParam(name = "first", required = false) Integer first,

      @Parameter(name = "max", description = "Maximum results size (defaults to 100).")
      @RequestParam(name = "max", required = false) Integer max) {

    return authTemplate.manyWithAuthentication(auth -> groupService
        .getMembers(auth.getName(), groupId, first, max));
  }

  /**
   * Add member.
   *
   * @param groupId the group id
   * @param userId the user id
   * @return the mono
   */
  @Operation(
      description = "Add member.",
      security = {
          @SecurityRequirement(name = "bearer-jwt")
      }
  )
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "200", description = "OK"),
          @ApiResponse(responseCode = "400", description = "Bad request", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "401", description = "Unauthorized", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "403", description = "Forbidden", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "404", description = "Not found", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "500", description = "Internal server error", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          })
      }
  )
  @PutMapping(
      path = "/{groupId}/members/{userId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<Void> addMember(
      @Parameter(name = "groupId", description = "The ID of the group.", required = true)
      @PathVariable String groupId,

      @Parameter(name = "userId", description = "The ID of the user (member).", required = true)
      @PathVariable String userId) {

    return authTemplate.oneWithAuthentication(auth -> groupService
        .addMember(auth.getName(), groupId, userId));
  }

  /**
   * Remove member.
   *
   * @param groupId the group id
   * @param userId the user id
   * @return the mono
   */
  @Operation(
      description = "Remove member.",
      security = {
          @SecurityRequirement(name = "bearer-jwt")
      }
  )
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "200", description = "OK"),
          @ApiResponse(responseCode = "400", description = "Bad request", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "401", description = "Unauthorized", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "403", description = "Forbidden", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "404", description = "Not found", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "500", description = "Internal server error", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          })
      }
  )
  @DeleteMapping(
      path = "/{groupId}/members/{userId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<Void> removeMember(
      @Parameter(name = "groupId", description = "The ID of the group.", required = true)
      @PathVariable String groupId,

      @Parameter(name = "userId", description = "The ID of the user (member).", required = true)
      @PathVariable String userId) {

    return authTemplate.oneWithAuthentication(auth -> groupService
        .removeMember(auth.getName(), groupId, userId));
  }

  /**
   * Update group.
   *
   * @param groupId the group id
   * @param group the group
   * @return the mono
   */
  @Operation(
      description = "Update group.",
      security = {
          @SecurityRequirement(name = "bearer-jwt")
      }
  )
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "200", description = "OK"),
          @ApiResponse(responseCode = "400", description = "Bad request", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "401", description = "Unauthorized", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "403", description = "Forbidden", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "404", description = "Not found", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "500", description = "Internal server error", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          })
      }
  )
  @PutMapping(
      path = "/{groupId}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<Group> updateGroup(
      @Parameter(name = "groupId", description = "The ID of the group.", required = true)
      @PathVariable String groupId,

      @Valid @RequestBody GroupUpdate group) {

    return authTemplate.oneWithAuthentication(auth -> groupService
        .updateGroup(auth.getName(), groupId, group));
  }

  /**
   * Delete group.
   *
   * @param groupId the group id
   * @return the mono
   */
  @Operation(
      description = "Delete group.",
      security = {
          @SecurityRequirement(name = "bearer-jwt")
      }
  )
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "200", description = "OK"),
          @ApiResponse(responseCode = "400", description = "Bad request", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "401", description = "Unauthorized", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "403", description = "Forbidden", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "404", description = "Not found", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          }),
          @ApiResponse(responseCode = "500", description = "Internal server error", content = {
              @Content(schema = @Schema(implementation = RestApiException.class))
          })
      }
  )
  @DeleteMapping(
      path = "/{groupId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<Void> deleteGroup(
      @Parameter(name = "groupId", description = "The ID of the group.", required = true)
      @PathVariable String groupId) {

    return authTemplate.oneWithAuthentication(auth -> groupService
        .deleteGroup(auth.getName(), groupId));
  }

}
