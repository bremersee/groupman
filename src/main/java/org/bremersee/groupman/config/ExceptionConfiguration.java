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

import org.bremersee.exception.RestApiExceptionMapper;
import org.bremersee.exception.RestApiExceptionMapperImpl;
import org.bremersee.exception.RestApiExceptionMapperProperties;
import org.bremersee.web.reactive.ApiExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;

/**
 * The exception configuration.
 *
 * @author Christian Bremer
 */
@Configuration
@EnableConfigurationProperties({RestApiExceptionMapperProperties.class})
public class ExceptionConfiguration {

  private final String applicationName;

  private final RestApiExceptionMapperProperties apiExceptionMapperProperties;

  /**
   * Instantiates a new exception configuration.
   *
   * @param applicationName              the application name
   * @param apiExceptionMapperProperties the api exception mapper properties
   */
  @Autowired
  public ExceptionConfiguration(
      @Value("${spring.application.name:groupman}") String applicationName,
      RestApiExceptionMapperProperties apiExceptionMapperProperties) {
    this.applicationName = applicationName;
    this.apiExceptionMapperProperties = apiExceptionMapperProperties;
  }

  /**
   * Builds the rest api exception mapper bean.
   *
   * @return the rest api exception mapper bean
   */
  @Bean
  public RestApiExceptionMapper restApiExceptionMapper() {
    return new RestApiExceptionMapperImpl(apiExceptionMapperProperties, applicationName);
  }

  /**
   * Builds api exception handler bean.
   *
   * @param errorAttributes        the error attributes
   * @param resourceProperties     the resource properties
   * @param applicationContext     the application context
   * @param serverCodecConfigurer  the server codec configurer
   * @param restApiExceptionMapper the rest api exception mapper
   * @return the api exception handler bean
   */
  @Bean
  @Order(-2)
  public ApiExceptionHandler apiExceptionHandler(
      ErrorAttributes errorAttributes,
      ResourceProperties resourceProperties,
      ApplicationContext applicationContext,
      ServerCodecConfigurer serverCodecConfigurer,
      RestApiExceptionMapper restApiExceptionMapper) {

    return new ApiExceptionHandler(
        errorAttributes,
        resourceProperties,
        applicationContext,
        serverCodecConfigurer,
        restApiExceptionMapper);
  }

}
