/*
 * Copyright 2024 the original author or authors.
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

import static java.util.Objects.requireNonNull;
import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.spring.security.ldaptive.authentication.ReactiveLdaptiveAuthenticationManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.CsrfSpec;
import org.springframework.security.config.web.server.ServerHttpSecurity.FormLoginSpec;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter.Mode;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import reactor.core.publisher.Mono;

/**
 * The web security configuration.
 *
 * @author Christian Bremer
 */
@EnableWebFluxSecurity
@EnableConfigurationProperties({
    SecurityProperties.class,
    OAuth2ResourceServerProperties.class
})
@Configuration
@Slf4j
public class WebSecurityConfiguration {

  private final OAuth2ResourceServerProperties resourceServerProperties;

  private final Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  private final ReactiveAuthenticationManager basicAuthenticationManager;

  /**
   * Instantiates a new web security configuration.
   *
   * @param securityProperties the security properties
   * @param resourceServerProperties the resource server properties
   * @param jwtConverterProvider the jwt converter provider
   * @param authenticationManagers the authentication managers
   */
  public WebSecurityConfiguration(
      SecurityProperties securityProperties,
      OAuth2ResourceServerProperties resourceServerProperties,
      ObjectProvider<Converter<Jwt, AbstractAuthenticationToken>> jwtConverterProvider,
      List<ReactiveAuthenticationManager> authenticationManagers) {
    this.resourceServerProperties = resourceServerProperties;
    this.jwtAuthenticationConverter = jwtConverterProvider
        .getIfAvailable(JwtAuthenticationConverter::new);
    this.basicAuthenticationManager = authenticationManagers.stream()
        .filter(authenticationManager -> authenticationManager
            instanceof ReactiveLdaptiveAuthenticationManager)
        .findFirst()
        .map(authenticationManager -> {
          log.info("Using ldaptive authentication manager for basic authentication.");
          return authenticationManager;
        })
        .orElseGet(() -> {
          if (!authenticationManagers.isEmpty()) {
            log.info("Using authentication managers for basic authentication: {}",
                authenticationManagers.get(0));
            return authenticationManagers.get(0);
          } else {
            return createInMemoryAuthenticationManager(securityProperties);
          }
        });
  }

  private static ReactiveAuthenticationManager createInMemoryAuthenticationManager(
      SecurityProperties securityProperties) {
    log.info("Using in-memory authentication manager with username '{}' and password '{}'.",
        securityProperties.getUser().getName(), securityProperties.getUser().getPassword());
    String password = securityProperties.getUser().getPassword();
    if (!password.startsWith("{noop}")) {
      password = "{noop}" + password;
    }
    UserDetails userDetails = User.builder()
        .username(securityProperties.getUser().getName())
        .password(password)
        .roles(securityProperties.getUser().getRoles().toArray(new String[0]))
        .build();
    ReactiveUserDetailsService userDetailsService = new MapReactiveUserDetailsService(
        List.of(userDetails));
    return new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService);

  }

  /**
   * Creates security web filter chain.
   *
   * @param http the http
   * @return the security web filter chain
   */
  @Bean
  public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    http
        .authorizeExchange(exchanges -> exchanges
            .pathMatchers(HttpMethod.OPTIONS, "/**")
            .permitAll()

            .matchers(EndpointRequest.to(InfoEndpoint.class, HealthEndpoint.class))
            .permitAll()

            .matchers(new AndServerWebExchangeMatcher(
                EndpointRequest.toAnyEndpoint(),
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/**")
            ))
            .hasAnyAuthority("ROLE_ACTUATOR", "ROLE_ACTUATOR_ADMIN")

            .matchers(EndpointRequest.toAnyEndpoint())
            .hasAuthority("ROLE_ACTUATOR_ADMIN")

            .pathMatchers("/api/admin/**")
            .hasAnyAuthority("ROLE_ADMIN", "ROLE_GROUP_ADMIN")

            .pathMatchers("/v3/**").permitAll()
            .pathMatchers("/webjars/**").permitAll()
            .pathMatchers("/swagger-ui/**").permitAll()
            .pathMatchers("/swagger-ui.html").permitAll()

            .anyExchange().authenticated())

        .csrf(CsrfSpec::disable)

        .headers(configurer -> configurer
            .frameOptions(c -> c.mode(Mode.SAMEORIGIN)))

        .httpBasic(customizer -> customizer
            .authenticationManager(basicAuthenticationManager))
        .formLogin(FormLoginSpec::disable);

    if (!isEmpty(resourceServerProperties.getJwt().getJwkSetUri())) {
      log.info("Authentication of bearer token enabled: {}",
          resourceServerProperties.getJwt().getJwkSetUri());
      http
          .oauth2ResourceServer(customizer -> customizer
              .jwt(jwtConfigurer -> jwtConfigurer

                  .jwtAuthenticationConverter(jwt -> Mono
                      .just(requireNonNull(jwtAuthenticationConverter.convert(jwt))))
                  .jwkSetUri(resourceServerProperties.getJwt().getJwkSetUri())));
    } else {
      log.info("Authentication of bearer token disabled.");
    }

    return http.build();
  }

}
