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

package org.bremersee.groupman.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.bremersee.groupman.repository.GroupRepository;
import org.bremersee.groupman.repository.ldap.GroupLdapRepository;
import org.springframework.stereotype.Component;

/**
 * The gauge metrics.
 *
 * @author Christian Bremer
 */
@Component
@Slf4j
public class GaugeMetrics {

  /**
   * Instantiates new gauge metrics.
   *
   * @param meterRegistry       the meter registry
   * @param groupRepository     the group repository
   * @param groupLdapRepository the group ldap repository
   */
  public GaugeMetrics(
      MeterRegistry meterRegistry,
      GroupRepository groupRepository,
      GroupLdapRepository groupLdapRepository) {

    meterRegistry.gauge(
        "groups_size",
        Collections.singleton(Tag.of("storage", "mongodb")),
        groupRepository,
        this::groupsInDatabaseSize);

    meterRegistry.gauge(
        "groups_size",
        Collections.singleton(Tag.of("storage", "ldap")),
        groupLdapRepository,
        this::groupsInDirectorySize);
  }

  private double groupsInDatabaseSize(GroupRepository groupRepository) {
    Long size = groupRepository.count().block();
    return size != null ? size : 0.;
  }

  private double groupsInDirectorySize(GroupLdapRepository groupRepository) {
    Long size = groupRepository.count().block();
    return size != null ? size : 0.;
  }

}