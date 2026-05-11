package com.backend.core.security;

import java.security.interfaces.RSAPublicKey;

import org.springframework.stereotype.Component;

import com.backend.core.dtos.UserDto;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenValidator {
  private final JwksClient jwksClient;

  public Mono<UserDto> validate(String token) {
    return jwksClient
        .getPublicKey()
        .flatMap(publicKey -> Mono.fromCallable(() -> parseAndExtractUser(token, publicKey)));
  }

  private UserDto parseAndExtractUser(String token, RSAPublicKey publicKey) {
    try {
      Claims claims =
          Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token).getPayload();

      String id = claims.get("id", String.class);
      String email = claims.get("email", String.class);

      return new UserDto(id, email);
    } catch (ExpiredJwtException e) {
      log.debug("Token expired: {}", e.getMessage());
      throw e;
    } catch (JwtException e) {
      log.warn("Invalid token: {}", e.getMessage());
      throw e;
    }
  }
}
