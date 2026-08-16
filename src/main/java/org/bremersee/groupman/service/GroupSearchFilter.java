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

package org.bremersee.groupman.service;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.Optional;
import java.util.function.Predicate;
import org.bremersee.groupman.model.Group;

/**
 * The group search filter.
 *
 * @author Christian Bremer
 */
public record GroupSearchFilter(String search) implements Predicate<Group> {

  @Override
  public boolean test(Group groupDto) {
    if (isEmpty(search)) {
      return true;
    }
    if (isEmpty(groupDto)) {
      return false;
    }
    String lowerSearch = search.toLowerCase();
    String name = Optional.ofNullable(groupDto.getName())
        .map(String::toLowerCase)
        .orElse("");
    String description = Optional.ofNullable(groupDto.getDescription())
        .map(String::toLowerCase)
        .orElse("");
    return name.contains(lowerSearch) || description.contains(lowerSearch);
  }
}
