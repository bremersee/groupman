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

package org.bremersee.groupman.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * The user.
 *
 * @author Christian Bremer
 */
@Value.Style(
    visibility = Value.Style.ImplementationVisibility.PACKAGE,
    overshadowImplementation = true,
    depluralize = true,
    jdk9Collections = true,
    get = {"get*", "is*"},
    withUnaryOperator = "with*")
@Value.Immutable
@JsonSerialize(as = ImmutableUser.class)
@JsonDeserialize(as = ImmutableUser.class)
public interface User {

  /**
   * Gets id.
   *
   * @return the id
   */
  @JsonProperty(value = "id", required = true)
  String getId();

  /**
   * Gets username.
   *
   * @return the username
   */
  @JsonProperty(value = "username", required = true)
  String getUsername();

  /**
   * Gets first name.
   *
   * @return the first name
   */
  @Nullable
  String getFirstName();

  /**
   * Gets last name.
   *
   * @return the last name
   */
  @Nullable
  String getLastName();

  /**
   * Creates builder.
   *
   * @return the builder
   */
  static Builder builder() {
    return new Builder();
  }

  /**
   * The builder.
   */
  class Builder extends ImmutableUser.Builder {

  }

}
