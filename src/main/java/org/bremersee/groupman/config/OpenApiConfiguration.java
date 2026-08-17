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

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * The open api configuration.
 *
 * @author Christian Bremer
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Group Manager API",
        description = "RESTful service for group manager application.",
        version = "v1"
    )
)
@SecurityScheme(
    name = "bearer-jwt",
    type = SecuritySchemeType.OAUTH2,
    flows = @OAuthFlows(
        authorizationCode = @OAuthFlow(
            authorizationUrl = "${spring.security.oauth2.client.provider.swagger-ui.authorization-uri:}",
            tokenUrl = "${spring.security.oauth2.client.provider.swagger-ui.token-uri:}",
            scopes = {
                @OAuthScope(name = "openid")
            }
        )
    )
)
class OpenApiConfiguration {

  /**
   * Instantiates a new open api configuration.
   */
  OpenApiConfiguration() {
    super();
  }

}
