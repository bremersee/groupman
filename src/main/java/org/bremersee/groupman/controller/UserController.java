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
import org.bremersee.exception.model.RestApiException;
import org.bremersee.groupman.model.User;
import org.bremersee.groupman.service.UserService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * The user controller.
 *
 * @author Christian Bremer
 */
@RestController
@RequestMapping(path = "/api/users")
@Validated
public class UserController {

  private final UserService userService;

  /**
   * Instantiates a new user controller.
   *
   * @param userService the user service
   */
  public UserController(UserService userService) {
    Assert.notNull(userService, "User service must not be null.");
    this.userService = userService;
  }

  /**
   * Find users.
   *
   * @param search A String contained in username, first or last name, or email. Default search
   *     behavior is prefix-based (e.g., foo or foo*). Use *foo* for infix search and "foo" for
   *     exact search.
   * @param first pagination offset
   * @param max maximum results size (defaults to 100)
   * @return the flux
   */
  @Operation(
      description = "Find users.",
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
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public Flux<User> findUsers(
      @Parameter(name = "search", description = "A String contained in username, first or last "
          + "name, or email. Default search behavior is prefix-based (e.g., foo or foo*). Use "
          + "*foo* for infix search and \"foo\" for exact search.")
      @RequestParam(name = "search") String search,

      @Parameter(name = "first", description = "Pagination offset.")
      @RequestParam(name = "first", required = false) Integer first,

      @Parameter(name = "max", description = "Maximum results size (defaults to 100).")
      @RequestParam(name = "max", required = false) Integer max) {

    return userService.findUsers(search, first, max);
  }

}
