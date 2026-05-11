package com.backend.core.security;

import static org.springframework.http.HttpStatus.FORBIDDEN;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
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
public class CustomAccessDeniedHandler implements ServerAccessDeniedHandler {
  private final ObjectMapper objectMapper;

  @Override
  public Mono<Void> handle(
      ServerWebExchange exchange, AccessDeniedException accessDeniedException) {
    return Mono.defer(
        () -> {
          exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
          exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

          ErrorDto errorDto =
              new ErrorDto(
                  FORBIDDEN.getReasonPhrase() + ": " + accessDeniedException.getMessage(),
                  exchange.getRequest().getURI().getPath(),
                  FORBIDDEN);

          try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorDto);
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
          } catch (JsonProcessingException e) {
            log.error("Error writing access denied response", e);
            return exchange.getResponse().setComplete();
          }
        });
  }
}
