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

package org.bremersee.groupman.service;

import static org.springframework.util.ObjectUtils.isEmpty;

import org.bremersee.groupman.mapper.UserMapper;
import org.bremersee.groupman.model.User;
import org.bremersee.keycloak.api.GetUsersParameters;
import org.bremersee.keycloak.api.webflux.KeycloakAdminClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;

/**
 * The user service.
 *
 * @author Christian Bremer
 */
@Service
public class UserService {

  private final KeycloakAdminClient keycloakAdminClient;

  private final UserMapper userMapper;

  private final String realm;

  /**
   * Instantiates a new user service.
   *
   * @param keycloakAdminClient the keycloak admin client
   * @param userMapper the user mapper
   * @param realm the realm
   */
  public UserService(
      KeycloakAdminClient keycloakAdminClient,
      UserMapper userMapper,
      @Value("${bremersee.keycloak.realm:master}") String realm) {

    Assert.notNull(keycloakAdminClient, "Keycloak admin client must not be null.");
    Assert.notNull(userMapper, "User mapper must not be null.");
    Assert.hasText(realm, "Realm is required.");
    this.keycloakAdminClient = keycloakAdminClient;
    this.userMapper = userMapper;
    this.realm = realm;
  }

  /**
   * Find users.
   *
   * @param search the search
   * @param first the first
   * @param max the max
   * @return the flux
   */
  public Flux<User> findUsers(
      String search,
      Integer first,
      Integer max) {

    if (isEmpty(search) || search.length() < 3) {
      return Flux.empty();
    }
    return keycloakAdminClient
        .getUsers(realm, GetUsersParameters.builder()
            .search(search)
            .first(first)
            .max(max)
            .build())
        .map(userMapper::mapToDto);
  }

}
