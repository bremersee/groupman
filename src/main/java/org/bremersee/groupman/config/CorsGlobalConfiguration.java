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

import org.bremersee.security.CorsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * The global CORS configuration.
 *
 * @author Christian Bremer
 */
@EnableConfigurationProperties(CorsProperties.class)
@Configuration
public class CorsGlobalConfiguration implements WebFluxConfigurer {

  private CorsProperties corsProperties;

  /**
   * Instantiates a new global CORS configuration.
   *
   * @param corsProperties the cors properties
   */
  public CorsGlobalConfiguration(CorsProperties corsProperties) {
    this.corsProperties = corsProperties;
  }

  @Override
  public void addCorsMappings(CorsRegistry corsRegistry) {
    for (CorsProperties.CorsConfiguration config : corsProperties.getConfigs()) {
      corsRegistry.addMapping(config.getPathPattern())
          .allowedOrigins(config.getAllowedOrigins().toArray(new String[0]))
          .allowedMethods(config.getAllowedMethods().toArray(new String[0]))
          .allowedHeaders(config.getAllowedHeaders().toArray(new String[0]))
          .maxAge(config.getMaxAge())
          .allowCredentials(config.isAllowCredentials());
    }
  }
}
