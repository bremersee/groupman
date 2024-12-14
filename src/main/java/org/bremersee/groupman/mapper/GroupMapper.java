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

package org.bremersee.groupman.mapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import org.bremersee.groupman.model.Group;
import org.bremersee.groupman.repository.GroupEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants.ComponentModel;

/**
 * The group mapper.
 *
 * @author Christian Bremer
 */
@Mapper(componentModel = ComponentModel.SPRING)
public interface GroupMapper {

  /**
   * Map group entity to group.
   *
   * @param groupEntity the group entity
   * @return the group
   */
  Group mapEntity(GroupEntity groupEntity);

  /**
   * Map group to group entity.
   *
   * @param group the group
   * @return the group entity
   */
  GroupEntity mapDto(Group group);

  /**
   * Map date to offset date time.
   *
   * @param date the date
   * @return the offset date time
   */
  default OffsetDateTime mapDate(Date date) {
    if (date == null) {
      return null;
    }
    return OffsetDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
  }

  /**
   * Map offset date time to date.
   *
   * @param offsetDateTime the offset date time
   * @return the date
   */
  default Date mapOffsetDateTime(OffsetDateTime offsetDateTime) {
    if (offsetDateTime == null) {
      return null;
    }
    return Date.from(offsetDateTime.toInstant());
  }

}
