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

package org.bremersee.groupman;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.springframework.util.StringUtils;

/**
 * The ldap entry utilities.
 *
 * @author Christian Bremer
 */
@Slf4j
abstract class LdapEntryUtils {

  private static final String WHEN_DATE_PATTERN = "yyyyMMddHHmmss";

  private LdapEntryUtils() {
  }

  /**
   * Create dn string.
   *
   * @param rdn      the rdn
   * @param rdnValue the rdn value
   * @param baseDn   the base dn
   * @return the string
   */
  static String createDn(
      @NotNull final String rdn,
      @NotNull final String rdnValue,
      @NotNull final String baseDn) {
    return rdn + "=" + rdnValue + "," + baseDn;
  }

  /**
   * Gets attribute value.
   *
   * @param ldapEntry     the ldap entry
   * @param attributeName the attribute name
   * @param defaultValue  the default value
   * @return the attribute value
   */
  static String getAttributeValue(
      @NotNull final LdapEntry ldapEntry,
      @NotNull final String attributeName,
      final String defaultValue) {

    final LdapAttribute attr = ldapEntry.getAttribute(attributeName);
    if (attr == null || !StringUtils.hasText(attr.getStringValue())) {
      return defaultValue;
    }
    return attr.getStringValue();
  }

  /**
   * When time to date.
   *
   * @param value the value (e. g. '20190527181601.0Z')
   * @return the offset date time
   */
  static Date whenTimeToDate(final String value) {
    if (!StringUtils.hasText(value) || value.length() < WHEN_DATE_PATTERN.length()) {
      return null;
    }
    final SimpleDateFormat sdf = new SimpleDateFormat(WHEN_DATE_PATTERN);
    sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
    try {
      return sdf.parse(value.substring(0, WHEN_DATE_PATTERN.length()));

    } catch (final Exception e) {
      log.error("Parsing when time [{}] failed. Returning null.", value, e);
      return null;
    }
  }

}
