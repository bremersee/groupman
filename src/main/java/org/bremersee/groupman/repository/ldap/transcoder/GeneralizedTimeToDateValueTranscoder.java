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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;
import org.ldaptive.io.AbstractStringValueTranscoder;
import org.ldaptive.io.GeneralizedTimeValueTranscoder;
import org.springframework.util.StringUtils;

/**
 * The generalized time value transcoder.
 *
 * @author Christian Bremer
 */
public class GeneralizedTimeToDateValueTranscoder
    extends AbstractStringValueTranscoder<Date> {

  private static final GeneralizedTimeValueTranscoder transcoder
      = new GeneralizedTimeValueTranscoder();

  @Override
  public Date decodeStringValue(String value) {
    return Optional.ofNullable(value)
        .filter(StringUtils::hasText)
        .map(transcoder::decodeStringValue)
        .map(ZonedDateTime::toInstant)
        .map(Date::from)
        .orElse(null);
  }

  @Override
  public String encodeStringValue(Date value) {
    return Optional.ofNullable(value)
        .map(date -> ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC))
        .map(transcoder::encodeStringValue)
        .orElse(null);
  }

  @Override
  public Class<Date> getType() {
    return Date.class;
  }
}
