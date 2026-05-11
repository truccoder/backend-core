package com.backend.core.security;

import java.util.Collections;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.backend.core.annotations.Anonymous;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class JwtTokenAuthenticationFilter implements WebFilter {
  private static final String BEARER = "Bearer ";
  private static final String ACTUATOR_ENDPOINT = "/actuator";
  private static final String HEALTH_ENDPOINT = "/health";
  private final JwtTokenValidator jwtTokenValidator;
  private final CustomAuthenticationEntryPoint authenticationEntryPoint;
  private final RequestMappingHandlerMapping handlerMapping;

  public JwtTokenAuthenticationFilter(
      JwtTokenValidator jwtTokenValidator,
      CustomAuthenticationEntryPoint authenticationEntryPoint,
      @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
    this.jwtTokenValidator = jwtTokenValidator;
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.handlerMapping = handlerMapping;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    if (isActuatorRequest(exchange)) {
      return chain.filter(exchange);
    }

    return isAnonymous(exchange)
        .flatMap(
            isAnonymous -> {
              if (Boolean.TRUE.equals(isAnonymous)) {
                return chain.filter(exchange);
              }
              return authenticate(exchange, chain);
            });
  }

  private Mono<Void> authenticate(ServerWebExchange exchange, WebFilterChain chain) {
    String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (Objects.isNull(authHeader) || !authHeader.startsWith(BEARER)) {
      return chain.filter(exchange);
    }

    String token = authHeader.substring(BEARER.length());

    return jwtTokenValidator
        .validate(token)
        .flatMap(
            user -> {
              UsernamePasswordAuthenticationToken authentication =
                  new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
              return chain
                  .filter(exchange)
                  .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
            })
        .onErrorResume(
            e -> {
              log.error("Token validation failed: {}", e.getMessage());
              return authenticationEntryPoint.commence(
                  exchange,
                  new org.springframework.security.authentication.AuthenticationServiceException(
                      "Token validation failed: " + e.getMessage()));
            });
  }

  private Mono<Boolean> isAnonymous(ServerWebExchange exchange) {
    return handlerMapping
        .getHandler(exchange)
        .ofType(HandlerMethod.class)
        .map(this::hasAnonymousAnnotation)
        .defaultIfEmpty(false);
  }

  private boolean hasAnonymousAnnotation(HandlerMethod handlerMethod) {
    return handlerMethod.hasMethodAnnotation(Anonymous.class)
        || handlerMethod.getBeanType().isAnnotationPresent(Anonymous.class);
  }

  private boolean isActuatorRequest(ServerWebExchange exchange) {
    String path = exchange.getRequest().getURI().getPath();
    return path.startsWith(ACTUATOR_ENDPOINT) || path.startsWith(HEALTH_ENDPOINT);
  }
}
