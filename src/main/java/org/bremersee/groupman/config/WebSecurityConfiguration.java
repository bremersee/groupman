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

package org.bremersee.groupman.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.security.autoconfigure.actuate.web.reactive.EndpointRequest;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.CsrfSpec;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.oauth2.server.resource.web.server.BearerTokenServerAuthenticationEntryPoint;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter.Mode;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.util.Assert;
import org.springframework.web.cors.reactive.CorsConfigurationSource;

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
@Slf4j
class WebSecurityConfiguration {

  private final OAuth2ResourceServerProperties resourceServerProperties;

  private final CorsConfigurationSource corsConfigurationSource;

  private final Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  /**
   * Instantiates a new web security configuration.
   *
   * @param resourceServerProperties the resource server properties
   * @param corsConfigurationSource the cors configuration source
   * @param jwtAuthenticationConverter the jwt authentication converter
   */
  WebSecurityConfiguration(
      OAuth2ResourceServerProperties resourceServerProperties,
      CorsConfigurationSource corsConfigurationSource,
      Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter) {

    Assert.hasText(
        resourceServerProperties.getJwt().getJwkSetUri(),
        "jwk-set-uri must not be empty.");
    this.resourceServerProperties = resourceServerProperties;
    this.corsConfigurationSource = corsConfigurationSource;
    this.jwtAuthenticationConverter = jwtAuthenticationConverter;
  }

  /**
   * Creates security web filter chain.
   *
   * @param http the http
   * @return the security filter chain
   */
  @Bean
  SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
    return http
        .authorizeExchange(authorize -> authorize
            .pathMatchers(HttpMethod.OPTIONS, "/**")
            .permitAll()

            .matchers(
                EndpointRequest.to(InfoEndpoint.class),
                EndpointRequest.to(HealthEndpoint.class))
            .permitAll()

            .matchers(new AndServerWebExchangeMatcher(
                EndpointRequest.toAnyEndpoint(),
                new PathPatternParserServerWebExchangeMatcher("/**", HttpMethod.GET)))
            .hasAnyAuthority("ROLE_ACTUATOR", "ROLE_ACTUATOR_ADMIN", "ROLE_ADMIN")

            .matchers(EndpointRequest.toAnyEndpoint())
            .hasAnyAuthority("ROLE_ACTUATOR_ADMIN", "ROLE_ADMIN")

            .pathMatchers("/api/**")
            .authenticated()

            .pathMatchers("/v3/**").permitAll()
            .pathMatchers("/webjars/**").permitAll()
            .pathMatchers("/swagger-ui/**").permitAll()
            .pathMatchers("/swagger-ui.html").permitAll()

            .anyExchange()
            .permitAll()
        )

        .csrf(CsrfSpec::disable)

        .cors(customizer -> customizer
            .configurationSource(corsConfigurationSource))

        .headers(headerSpec -> headerSpec
            .frameOptions(fos -> fos.mode(Mode.SAMEORIGIN)))

        .oauth2ResourceServer(customizer -> customizer
            .authenticationEntryPoint(new BearerTokenServerAuthenticationEntryPoint())
            .jwt(jwtCustomizer -> jwtCustomizer
                .jwtAuthenticationConverter(new ReactiveJwtAuthenticationConverterAdapter(
                    jwtAuthenticationConverter))
                .jwkSetUri(resourceServerProperties.getJwt().getJwkSetUri())))

        .build();
  }

}
