package com.backend.core.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.backend.core.dtos.ValidateTokenRequestDto;
import com.backend.core.dtos.ValidateTokenResponseDto;

import reactor.core.publisher.Mono;

@Component
public class UserClient {
  private final WebClient webClient;

  public UserClient(
      WebClient.Builder webClientBuilder,
      @Value("${services.users-management.url:http://localhost:8090}") String usersManagementUrl) {
    this.webClient = webClientBuilder.baseUrl(usersManagementUrl).build();
  }

  public Mono<ValidateTokenResponseDto> validateToken(ValidateTokenRequestDto request) {
    return webClient
        .post()
        .uri("/v1/api/auth/validate-token")
        .bodyValue(request)
        .retrieve()
        .bodyToMono(ValidateTokenResponseDto.class);
  }
}
