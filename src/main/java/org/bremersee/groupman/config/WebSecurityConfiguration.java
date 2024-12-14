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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.CsrfSpec;
import org.springframework.security.config.web.server.ServerHttpSecurity.FormLoginSpec;
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
    OAuth2ResourceServerProperties.class
})
@Configuration
public class WebSecurityConfiguration {

  private final OAuth2ResourceServerProperties resourceServerProperties;

  //private final CorsConfigurationSource corsConfigurationSource;

  private final Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  /**
   * Instantiates a new Web security configuration.
   *
   * @param resourceServerProperties the resource server properties
   * @param jwtConverterProvider the jwt converter provider
   */
  public WebSecurityConfiguration(
      OAuth2ResourceServerProperties resourceServerProperties,
      //CorsConfigurationSource corsConfigurationSource,
      ObjectProvider<Converter<Jwt, AbstractAuthenticationToken>> jwtConverterProvider) {
    this.resourceServerProperties = resourceServerProperties;
    //this.corsConfigurationSource = corsConfigurationSource;
    jwtAuthenticationConverter = jwtConverterProvider
        .getIfAvailable(JwtAuthenticationConverter::new);
  }

  /**
   * Security web filter chain.
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

            .matchers(
                org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest.to(
                    InfoEndpoint.class, HealthEndpoint.class))
            .permitAll()

            .matchers(new AndServerWebExchangeMatcher(
                org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest.toAnyEndpoint(),
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, "/**")
            ))
            .hasAnyAuthority("ROLE_ACTUATOR", "ROLE_ACTUATOR_ADMIN")

            .matchers(
                org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest.toAnyEndpoint())
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

        .formLogin(FormLoginSpec::disable)
    ;
    if (!isEmpty(resourceServerProperties.getJwt().getJwkSetUri())) {
      http
          .oauth2ResourceServer(customizer -> customizer
              .jwt(jwtConfigurer -> jwtConfigurer
                  .jwtAuthenticationConverter(jwt -> Mono
                      .just(requireNonNull(jwtAuthenticationConverter.convert(jwt))))
                  .jwkSetUri(resourceServerProperties.getJwt().getJwkSetUri())));
    }
    return http.build();
  }

}
