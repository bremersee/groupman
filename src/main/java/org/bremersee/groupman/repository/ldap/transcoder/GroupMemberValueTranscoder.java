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

package org.bremersee.groupman.repository.ldap.transcoder;

import lombok.extern.slf4j.Slf4j;
import org.bremersee.data.ldaptive.LdaptiveEntryMapper;
import org.bremersee.groupman.config.DomainControllerProperties;
import org.ldaptive.io.AbstractStringValueTranscoder;
import org.springframework.util.StringUtils;

/**
 * The group member value transcoder.
 *
 * @author Christian Bremer
 */
@Slf4j
public class GroupMemberValueTranscoder extends AbstractStringValueTranscoder<String> {

  private DomainControllerProperties properties;

  /**
   * Instantiates a new group member value transcoder.
   *
   * @param properties the properties
   */
  public GroupMemberValueTranscoder(DomainControllerProperties properties) {
    this.properties = properties;
  }

  @Override
  public String decodeStringValue(String value) {
    return properties.isMemberDn() ? LdaptiveEntryMapper.getRdn(value) : value;
  }

  @Override
  public String encodeStringValue(String value) {
    if (StringUtils.hasText(value)) {
      return properties.isMemberDn()
          ? LdaptiveEntryMapper.createDn(properties.getUserRdn(), value, properties.getUserBaseDn())
          : value;
    }
    return null;
  }

  @Override
  public Class<String> getType() {
    return String.class;
  }
}
