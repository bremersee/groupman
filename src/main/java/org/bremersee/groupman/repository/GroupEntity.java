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

package org.bremersee.groupman.repository;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.bremersee.groupman.model.Source;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.validation.annotation.Validated;

/**
 * The group entity.
 *
 * @author Christian Bremer
 */
@Document(collection = "groups")
@TypeAlias("group")
@CompoundIndexes({
    @CompoundIndex(name = "creator_group",
        def = "{'createdBy': 1, 'name': 1}",
        unique = true)
})
@Getter
@Setter
@ToString
@NoArgsConstructor
@Validated
public class GroupEntity implements Comparable<GroupEntity> {

  @Id
  private String id;

  @Version
  private Long version;

  @Indexed
  private String createdBy;

  private Date createdAt = new Date();

  @Indexed
  private Date modifiedAt = new Date();

  @Indexed
  private Source source = Source.INTERNAL;

  @NotBlank
  @Size(min = 3, max = 75)
  @Indexed
  private String name;

  @Size(max = 255)
  private String description;

  @Indexed
  private Set<String> members = new LinkedHashSet<>();

  @Indexed
  private Set<String> owners = new LinkedHashSet<>();

  /**
   * Instantiates a new Group entity.
   *
   * @param id          the id
   * @param version     the version
   * @param createdBy   the created by
   * @param createdAt   the created at
   * @param modifiedAt  the modified at
   * @param source      the source
   * @param name        the name
   * @param description the description
   * @param members     the members
   * @param owners      the owners
   */
  @Builder
  public GroupEntity(
      String id,
      Long version,
      String createdBy,
      Date createdAt,
      Date modifiedAt,
      Source source,
      @NotBlank @Size(min = 3, max = 75) String name,
      @Size(max = 255) String description,
      Set<String> members,
      Set<String> owners) {
    this.id = id;
    this.version = version;
    this.createdBy = createdBy;
    this.createdAt = createdAt;
    this.modifiedAt = modifiedAt;
    this.source = source;
    this.name = name;
    this.description = description;
    this.members = members;
    this.owners = owners;
  }

  @Override
  public int compareTo(GroupEntity o) {
    String s1 = getName() == null ? "" : getName();
    String s2 = o == null || o.getName() == null ? "" : o.getName();
    int c = s1.compareToIgnoreCase(s2);
    if (c != 0) {
      return c;
    }
    s1 = getCreatedBy() == null ? "" : getCreatedBy();
    s2 = o == null || o.getCreatedBy() == null ? "" : o.getCreatedBy();
    return s1.compareToIgnoreCase(s2);
  }
}
