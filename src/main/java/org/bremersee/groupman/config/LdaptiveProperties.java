/*
 * Copyright 2016 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.ldaptive.pool.PoolConfig;
import org.ldaptive.pool.SearchValidator;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The ldaptive properties.
 *
 * @author Christian Bremer
 */
@ConfigurationProperties(prefix = "bremersee.ldaptive")
@Getter
@Setter
@ToString(exclude = {"bindCredential"})
@EqualsAndHashCode(exclude = {"bindCredential"})
@NoArgsConstructor
public class LdaptiveProperties {

  /**
   * URL to the LDAP(s).
   */
  private String ldapUrl = "ldap://localhost:1389";

  /**
   * Amount of time in milliseconds that connects will block.
   */
  private long connectTimeout = -1;

  /**
   * Amount of time in milliseconds to wait for responses.
   */
  private long responseTimeout = -1;

  /**
   * Connect to LDAP using SSL protocol.
   */
  private boolean useSsl;

  /**
   * Connect to LDAP using startTLS.
   */
  private boolean useStartTls;

  /**
   * Name of the trust certificates to use for the SSL connection.
   */
  private String trustCertificates;

  /**
   * Name of the authentication certificate to use for the SSL connection.
   */
  private String authenticationCertificate;

  /**
   * Name of the key to use for the SSL connection.
   */
  private String authenticationKey;

  /**
   * DN to bind as before performing operations.
   */
  private String bindDn;

  /**
   * Credential for the bind DN.
   */
  private String bindCredential;

  private boolean pooled = false;

  /**
   * Minimum pool size.
   */
  private int minPoolSize = PoolConfig.DEFAULT_MIN_POOL_SIZE;

  /**
   * Maximum pool size.
   */
  private int maxPoolSize = PoolConfig.DEFAULT_MAX_POOL_SIZE;

  /**
   * Whether the ldap object should be validated when returned to the pool.
   */
  private boolean validateOnCheckIn = PoolConfig.DEFAULT_VALIDATE_ON_CHECKIN;

  /**
   * Whether the ldap object should be validated when given from the pool.
   */
  private boolean validateOnCheckOut = PoolConfig.DEFAULT_VALIDATE_ON_CHECKOUT;

  /**
   * Whether the pool should be validated periodically.
   */
  private boolean validatePeriodically = PoolConfig.DEFAULT_VALIDATE_PERIODICALLY;

  /**
   * Time in seconds that the validate pool should repeat.
   */
  private long validatePeriod = PoolConfig.DEFAULT_VALIDATE_PERIOD.toMillis();

  /**
   * Prune period in seconds.
   */
  private long prunePeriod = 300L;

  /**
   * Idle time in seconds.
   */
  private long idleTime = 600L;

  /**
   * Time in milliseconds to wait for an available connection.
   */
  private long blockWaitTime = 10000L;

  private SearchValidator searchValidator = new SearchValidator();

}
