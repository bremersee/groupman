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

package org.bremersee.groupman.config;

import org.bremersee.security.authentication.KeycloakReactiveJwtConverter;
import org.bremersee.security.authentication.PasswordFlowReactiveAuthenticationManager;
import org.bremersee.security.core.AuthorityConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;

/**
 * The security configuration.
 *
 * @author Christian Bremer
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

  private KeycloakReactiveJwtConverter keycloakJwtConverter;

  private PasswordFlowReactiveAuthenticationManager passwordFlowReactiveAuthenticationManager;

  /**
   * Instantiates a new security configuration.
   *
   * @param keycloakJwtConverter                      the keycloak jwt converter
   * @param passwordFlowReactiveAuthenticationManager the password flow reactive authentication
   *                                                  manager
   */
  @Autowired
  public SecurityConfiguration(
      KeycloakReactiveJwtConverter keycloakJwtConverter,
      PasswordFlowReactiveAuthenticationManager passwordFlowReactiveAuthenticationManager) {
    this.keycloakJwtConverter = keycloakJwtConverter;
    this.passwordFlowReactiveAuthenticationManager = passwordFlowReactiveAuthenticationManager;
  }

  /**
   * Builds the OAuth2 resource server filter chain.
   *
   * @param http the http
   * @return the security web filter chain
   */
  @Bean
  @Order(51)
  public SecurityWebFilterChain oauth2ResourceServerFilterChain(
      ServerHttpSecurity http) {

    http
        .securityMatcher(new NegatedServerWebExchangeMatcher(EndpointRequest.toAnyEndpoint()))
        .oauth2ResourceServer()
        .jwt()
        .jwtAuthenticationConverter(keycloakJwtConverter);

    http
        .authorizeExchange()
        .anyExchange().authenticated();

    return http.build();
  }

  /**
   * Builds the actuator filter chain.
   *
   * @param http the http security configuration object
   * @return the security web filter chain
   */
  @Bean
  @Order(52)
  public SecurityWebFilterChain actuatorFilterChain(
      ServerHttpSecurity http) {

    http
        .securityMatcher(EndpointRequest.toAnyEndpoint())
        .cors().disable()
        .csrf().disable()
        .httpBasic()
        .authenticationManager(passwordFlowReactiveAuthenticationManager);

    http
        .authorizeExchange()
        .matchers(EndpointRequest.to(HealthEndpoint.class)).permitAll()
        .matchers(EndpointRequest.to(InfoEndpoint.class)).permitAll()
        .anyExchange().hasAuthority(AuthorityConstants.ACTUATOR_ROLE_NAME);

    return http.build();
  }

}
