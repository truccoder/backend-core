package com.backend.core.security;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class JwksClient {
  private final WebClient webClient;
  private final AtomicReference<RSAPublicKey> cachedPublicKey = new AtomicReference<>();
  private final Duration cacheTtl = Duration.ofMinutes(30);
  private volatile long lastFetchTime = 0;

  public JwksClient(
      WebClient.Builder webClientBuilder,
      @Value("${services.users-management.url:http://localhost:8090}") String usersManagementUrl) {
    this.webClient = webClientBuilder.baseUrl(usersManagementUrl).build();
  }

  public Mono<RSAPublicKey> getPublicKey() {
    RSAPublicKey cached = cachedPublicKey.get();
    if (Objects.nonNull(cached) && !isCacheExpired()) {
      return Mono.just(cached);
    }
    return fetchJwks();
  }

  @SuppressWarnings("unchecked")
  private Mono<RSAPublicKey> fetchJwks() {
    return webClient
        .get()
        .uri("/.well-known/jwks.json")
        .retrieve()
        .bodyToMono(Map.class)
        .map(
            jwksResponse -> {
              List<Map<String, String>> keys = (List<Map<String, String>>) jwksResponse.get("keys");
              if (Objects.isNull(keys) || keys.isEmpty()) {
                throw new IllegalStateException("No keys found in JWKS response");
              }
              Map<String, String> jwk = keys.get(0);
              RSAPublicKey publicKey = parseRsaPublicKey(jwk);
              cachedPublicKey.set(publicKey);
              lastFetchTime = System.currentTimeMillis();
              log.info("JWKS public key fetched and cached successfully");
              return publicKey;
            })
        .doOnError(e -> log.error("Failed to fetch JWKS: {}", e.getMessage()));
  }

  private RSAPublicKey parseRsaPublicKey(Map<String, String> jwk) {
    try {
      byte[] nBytes = Base64.getUrlDecoder().decode(jwk.get("n"));
      byte[] eBytes = Base64.getUrlDecoder().decode(jwk.get("e"));

      BigInteger modulus = new BigInteger(1, nBytes);
      BigInteger exponent = new BigInteger(1, eBytes);

      RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return (RSAPublicKey) keyFactory.generatePublic(spec);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse RSA public key from JWKS", e);
    }
  }

  private boolean isCacheExpired() {
    return System.currentTimeMillis() - lastFetchTime > cacheTtl.toMillis();
  }
}
