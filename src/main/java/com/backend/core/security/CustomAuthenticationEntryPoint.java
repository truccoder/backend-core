package com.backend.core.security;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.backend.core.dtos.ErrorDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {
  private final ObjectMapper objectMapper;

  @Override
  public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException authException) {
    return Mono.defer(
        () -> {
          exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
          exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

          ErrorDto errorDto =
              new ErrorDto(
                  UNAUTHORIZED.getReasonPhrase() + ": " + authException.getMessage(),
                  exchange.getRequest().getURI().getPath(),
                  UNAUTHORIZED);

          try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorDto);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
          } catch (JsonProcessingException e) {
            log.error("Error writing authentication error response", e);
            return exchange.getResponse().setComplete();
          }
        });
  }
}
