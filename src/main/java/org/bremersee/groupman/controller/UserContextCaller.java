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

package org.bremersee.groupman.controller;

import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bremersee.exception.ServiceException;
import org.reactivestreams.Publisher;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The reactive user context caller.
 *
 * @author Christian Bremer
 */
@Validated
public class UserContextCaller {

  /**
   * The constant EMPTY_GROUPS_SUPPLIER.
   */
  public static final Supplier<Mono<Set<String>>> EMPTY_GROUPS_SUPPLIER = () -> Mono
      .just(Collections.emptySet());

  /**
   * The constant FORBIDDEN_SUPPLIER.
   */
  public static final Supplier<Mono<UserContext>> FORBIDDEN_SUPPLIER = () -> Mono
      .error(ServiceException::forbidden);

  private final Function<Authentication, Mono<Set<String>>> groupsFn;

  private final Supplier<Mono<UserContext>> unauthenticatedSupplier;

  /**
   * Instantiates a new reactive user context caller.
   */
  public UserContextCaller() {
    this(EMPTY_GROUPS_SUPPLIER, null);
  }

  /**
   * Instantiates a new reactive user context caller.
   *
   * @param groupsSupplier the groups supplier
   * @param unauthenticatedSupplier the unauthenticated supplier
   */
  public UserContextCaller(
      @Nullable Supplier<Mono<Set<String>>> groupsSupplier,
      @Nullable Supplier<Mono<UserContext>> unauthenticatedSupplier) {
    this(
        groupsSupplier != null
            ? authentication -> groupsSupplier.get()
            : authentication -> EMPTY_GROUPS_SUPPLIER.get(),
        unauthenticatedSupplier);
  }

  /**
   * Instantiates a new reactive user context caller.
   *
   * @param groupsFn the groups fn
   * @param unauthenticatedSupplier the unauthenticated supplier
   */
  public UserContextCaller(
      @Nullable Function<Authentication, Mono<Set<String>>> groupsFn,
      @Nullable Supplier<Mono<UserContext>> unauthenticatedSupplier) {
    this.groupsFn = groupsFn != null ? groupsFn : authentication -> EMPTY_GROUPS_SUPPLIER.get();
    this.unauthenticatedSupplier = unauthenticatedSupplier != null
        ? unauthenticatedSupplier
        : FORBIDDEN_SUPPLIER;
  }

  private Set<String> toRoles(Authentication authentication) {
    return authentication.getAuthorities()
        .stream()
        .map(GrantedAuthority::getAuthority)
        .collect(Collectors.toSet());
  }

  /**
   * One with user context mono.
   *
   * @param <R> the type parameter
   * @param function the function
   * @return the mono
   */
  public <R> Mono<R> oneWithUserContext(
      @NotNull Function<UserContext, ? extends Mono<R>> function) {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .filter(Authentication::isAuthenticated)
        .zipWhen(authentication -> groupsFn
            .apply(authentication)
            .switchIfEmpty(EMPTY_GROUPS_SUPPLIER.get()))
        .map(tuple -> UserContext.newInstance(
            tuple.getT1().getName(),
            toRoles(tuple.getT1()),
            tuple.getT2()))
        .switchIfEmpty(unauthenticatedSupplier.get())
        .flatMap(function);
  }

  /**
   * Many with user context flux.
   *
   * @param <R> the type parameter
   * @param function the function
   * @return the flux
   */
  public <R> Flux<R> manyWithUserContext(
      @NotNull Function<UserContext, ? extends Publisher<R>> function) {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .filter(Authentication::isAuthenticated)
        .zipWhen(authentication -> groupsFn
            .apply(authentication)
            .switchIfEmpty(EMPTY_GROUPS_SUPPLIER.get()))
        .map(tuple -> UserContext.newInstance(
            tuple.getT1().getName(),
            toRoles(tuple.getT1()),
            tuple.getT2()))
        .switchIfEmpty(unauthenticatedSupplier.get())
        .flatMapMany(function);
  }

}