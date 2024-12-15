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

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The in-memory security configuration.
 *
 * @author Christian Bremer
 */
@EnableConfigurationProperties({
    SecurityProperties.class
})
@Configuration
@Profile("in-memory")
@Slf4j
public class InMemorySecurityConfiguration {

  private final SecurityProperties securityProperties;

  /**
   * Instantiates a new in-memory security configuration.
   *
   * @param securityProperties the security properties
   */
  public InMemorySecurityConfiguration(SecurityProperties securityProperties) {
    this.securityProperties = securityProperties;
  }

  /**
   * Creates in-memory authentication manager for basic authentication.
   *
   * @return the user details repository reactive authentication manager
   */
  @Bean
  public UserDetailsRepositoryReactiveAuthenticationManager inMemoryAuthenticationManager() {
    log.info("Creating in-memory authentication manager with username '{}' and password '{}'.",
        securityProperties.getUser().getName(), securityProperties.getUser().getPassword());
    return new UserDetailsRepositoryReactiveAuthenticationManager(userDetailsService());
  }

  private ReactiveUserDetailsService userDetailsService() {
    String password = securityProperties.getUser().getPassword();
    if (!password.startsWith("{noop}")) {
      password = "{noop}" + password;
    }
    UserDetails userDetails = User.builder()
        .username(securityProperties.getUser().getName())
        .password(password)
        .roles(securityProperties.getUser().getRoles().toArray(new String[0]))
        .build();
    return new MapReactiveUserDetailsService(List.of(userDetails));
  }

}
