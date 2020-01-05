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

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.AlternateTypeRule;
import springfox.documentation.schema.AlternateTypeRules;
import springfox.documentation.schema.WildcardType;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebFlux;

/**
 * The swagger configuration.
 *
 * @author Christian Bremer
 */
@ConditionalOnWebApplication
@Configuration
@EnableConfigurationProperties(SwaggerProperties.class)
@EnableSwagger2WebFlux
public class SwaggerConfiguration {

  private SwaggerProperties swaggerProperties;

  private TypeResolver resolver;

  /**
   * Instantiates a new swagger configuration.
   *
   * @param swaggerProperties the swagger properties
   * @param resolver          the resolver
   */
  public SwaggerConfiguration(SwaggerProperties swaggerProperties, TypeResolver resolver) {
    this.swaggerProperties = swaggerProperties;
    this.resolver = resolver;
  }

  /**
   * Returns the swagger docket. The swagger definition will be available under {@code
   * http://localhost:8090/v2/api-docs}**.
   *
   * @return the swagger docket
   */
  @Bean
  public Docket api() {
    return new Docket(DocumentationType.SWAGGER_2)
        .select()
        .apis(RequestHandlerSelectors.any())
        .paths(PathSelectors.ant("/api/**"))
        .build()
        .pathMapping(swaggerProperties.getPathMapping())
        .apiInfo(apiInfo())

        .alternateTypeRules(new RecursiveAlternateTypeRule(resolver,
            Arrays.asList(
                AlternateTypeRules.newRule(
                    resolver.resolve(Mono.class, WildcardType.class),
                    resolver.resolve(WildcardType.class)),
                AlternateTypeRules.newRule(
                    resolver.resolve(ResponseEntity.class, WildcardType.class),
                    resolver.resolve(WildcardType.class))
            )))
        .alternateTypeRules(new RecursiveAlternateTypeRule(resolver,
            Arrays.asList(
                AlternateTypeRules.newRule(
                    resolver.resolve(Flux.class, WildcardType.class),
                    resolver.resolve(List.class, WildcardType.class)),
                AlternateTypeRules.newRule(
                    resolver.resolve(ResponseEntity.class, WildcardType.class),
                    resolver.resolve(WildcardType.class))
            )));

  }

  private ApiInfo apiInfo() {
    final Contact contact;
    if (StringUtils.hasText(swaggerProperties.getContactName())
        || StringUtils.hasText(swaggerProperties.getContactUrl())
        || StringUtils.hasText(swaggerProperties.getContactEmail())) {
      contact = new Contact(
          swaggerProperties.getContactName(),
          swaggerProperties.getContactUrl(),
          swaggerProperties.getContactEmail());
    } else {
      contact = null;
    }
    return new ApiInfo(
        swaggerProperties.getTitle(),
        swaggerProperties.getDescription(),
        swaggerProperties.getVersion(),
        swaggerProperties.getTermsOfServiceUrl(),
        contact,
        swaggerProperties.getLicense(),
        swaggerProperties.getLicenseUrl(),
        Collections.emptyList());
  }

  /**
   * The type Recursive alternate type rule.
   */
  public static class RecursiveAlternateTypeRule extends AlternateTypeRule {

    private List<AlternateTypeRule> rules;

    /**
     * Instantiates a new Recursive alternate type rule.
     *
     * @param typeResolver the type resolver
     * @param rules        the rules
     */
    public RecursiveAlternateTypeRule(TypeResolver typeResolver, List<AlternateTypeRule> rules) {
      // Unused but cannot be null
      super(typeResolver.resolve(Object.class), typeResolver.resolve(Object.class));
      this.rules = rules;
    }

    @Override
    public ResolvedType alternateFor(ResolvedType type) {
      Stream<ResolvedType> rStream = rules.stream()
          .flatMap(rule -> Stream.of(rule.alternateFor(type)));
      ResolvedType newType = rStream
          .filter(alternateType -> alternateType != type).findFirst().orElse(type);

      if (appliesTo(newType)) {
        // Recursion happens here
        return alternateFor(newType);
      }

      return newType;
    }

    @Override
    public boolean appliesTo(ResolvedType type) {
      return rules.stream().anyMatch(rule -> rule.appliesTo(type));
    }
  }
}
