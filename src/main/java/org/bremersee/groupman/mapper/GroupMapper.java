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

package org.bremersee.groupman.mapper;

import org.bremersee.groupman.model.GroupBase;
import org.bremersee.groupman.model.Group;
import org.bremersee.keycloak.api.model.GroupRepresentation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.MappingTarget;

/**
 * The group mapper.
 *
 * @author Christian Bremer
 */
@Mapper(componentModel = ComponentModel.SPRING)
public interface GroupMapper {

  /**
   * Map representation to group.
   *
   * @param source the source
   * @return the group
   */
  Group mapToDto(GroupRepresentation source);

  /**
   * Map group into representation.
   *
   * @param source the source
   * @param target the target
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "path", ignore = true)
  @Mapping(target = "parentId", ignore = true)
  @Mapping(target = "subGroupCount", ignore = true)
  @Mapping(target = "subGroups", ignore = true)
  @Mapping(target = "attributes", ignore = true)
  @Mapping(target = "realmRoles", ignore = true)
  @Mapping(target = "clientRoles", ignore = true)
  @Mapping(target = "access", ignore = true)
  void mapInto(GroupBase source, @MappingTarget GroupRepresentation target);

}
