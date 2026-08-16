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

import org.bremersee.spring.boot.autoconfigure.keycloak.DecodeNotFoundKeycloakAdminApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The keycloak admin api configuration.
 *
 * @author Christian Bremer
 */
@Configuration
public class KeycloakAdminApiConfiguration {

  /**
   * Instantiates a new keycloak admin api configuration.
   */
  public KeycloakAdminApiConfiguration() {
    super();
  }

  /**
   * Creates a decoding not found keycloak admin api customizer.
   *
   * @return the decoding not found keycloak admin api customizer
   */
  @Bean
  public DecodeNotFoundKeycloakAdminApiCustomizer decodeNotFoundKeycloakAdminApiCustomizer() {
    return new DecodeNotFoundKeycloakAdminApiCustomizer();
  }

}
