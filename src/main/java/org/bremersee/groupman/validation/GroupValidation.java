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

package org.bremersee.groupman.validation;

import static org.springframework.util.ObjectUtils.isEmpty;

import java.util.regex.Pattern;
import org.bremersee.exception.ServiceException;
import org.bremersee.groupman.model.GroupBase;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * The group validation.
 *
 * @author Christian Bremer
 */
@Component
public class GroupValidation {

  private static final Pattern groupNamePattern = Pattern.compile("^[^/\\n]{3,75}+$");

  private static final Pattern descriptionPattern = Pattern.compile("^[^\\n]{0,255}+$");

  /**
   * Instantiates a new group validation.
   */
  public GroupValidation() {
    super();
  }

  /**
   * Validate group.
   *
   * @param group the group
   */
  public void validateGroup(GroupBase group) {
    Assert.notNull(group, "Group is required.");
    Assert.hasText(group.getName(), "Group name is required.");
    if (!groupNamePattern.matcher(group.getName()).matches()) {
      throw ServiceException
          .badRequest("Group name is invalid.", "invalid_group_name");
    }
    if (!isEmpty(group.getDescription())
        && !descriptionPattern.matcher(group.getDescription()).matches()) {
      throw ServiceException
          .badRequest("Description is invalid.", "invalid_group_description");
    }
  }

}
