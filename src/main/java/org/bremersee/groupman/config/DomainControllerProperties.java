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

import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.ldaptive.SearchScope;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * The domain controller settings.
 *
 * @author Christian Bremer
 */
@ConfigurationProperties(prefix = "bremersee.domain-controller")
@Component
@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class DomainControllerProperties {

  private String groupBaseDn;

  private String groupRdn = "cn";

  // Must be the same as in keycloak (User Federation > Ldap > LDAP Mappers > Group mapper)
  private String groupNameAttribute = "cn";

  private String groupDescriptionAttribute = "description";

  private String groupMemberAttribute = "member";

  private boolean memberDn = true;

  private String userBaseDn;

  private String userRdn = "cn";

  private String groupFindAllFilter = "(objectClass=group)";

  private SearchScope groupFindAllSearchScope = SearchScope.ONELEVEL;

  private String groupFindOneFilter = "(&(objectClass=group)(cn={0}))";

  private SearchScope groupSearchScope = SearchScope.ONELEVEL;

  private String adminName = "Administrator";

  private List<String> ignoredLdapGroups = new ArrayList<>();

  /**
   * Gets the group find by names filter.
   *
   * @param size the size
   * @return the group find by names filter
   */
  @SuppressWarnings("WeakerAccess")
  public String getGroupFindByNamesFilter(int size) {
    if (size <= 0) {
      return groupFindAllFilter;
    } else if (size == 1) {
      return groupFindOneFilter;
    }
    StringBuilder sb = new StringBuilder();
    sb.append("(&");
    sb.append(groupFindAllFilter);
    sb.append("(|");
    for (int i = 0; i < size; i++) {
      sb.append("(").append(groupNameAttribute).append("={").append(i).append("})");
    }
    sb.append("))");
    return sb.toString();
  }

  /**
   * Gets the group find by member contains filter.
   *
   * @return the group find by member contains filter
   */
  public String getGroupFindByMemberContainsFilter() {
    return "(&" + groupFindAllFilter + "(" + groupMemberAttribute + "={0}))";
  }
}
