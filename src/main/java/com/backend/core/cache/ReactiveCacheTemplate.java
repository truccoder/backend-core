package com.backend.core.cache;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import org.springframework.data.redis.core.ReactiveRedisTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@AllArgsConstructor
public class ReactiveCacheTemplate<T> {
  private static final Duration LOCK_TTL = Duration.ofSeconds(5);
  private static final Duration RETRY_DELAY = Duration.ofMillis(100);
  private static final double JITTER_FACTOR = 0.15;
  private static final double EARLY_REFRESH_FRACTION = 0.1;

  private final ReactiveRedisTemplate<String, String> redis;
  private final ObjectMapper mapper;
  private final String keyPrefix;
  private final Duration ttl;
  private final TypeReference<T> type;

  /*
   Layer 1 — Jitter (the jitteredTtl method): Prevents many different keys from expiring at the same time.
   Layer 2 — Locking (the loadWithLock method): When a key does expire, only one request goes to the database.
   Layer 3 — Probabilistic early refresh (the maybeEarlyRefresh / shouldEarlyRefresh methods):
       Refreshes the cache before it expires, so the stampede never even gets a chance to happen.
  */
  public Mono<T> get(String id, Function<String, Mono<T>> dbFallback) {
    String key = getKey(id);
    return redis
        .opsForValue()
        .get(key)
        .flatMap(this::deserialize)
        .flatMap(entity -> maybeEarlyRefresh(id, key, entity, dbFallback))
        .switchIfEmpty(Mono.defer(() -> loadWithLock(id, key, dbFallback)))
        .doOnError(e -> log.warn("Cache read failed for key={}, falling back to DB", key, e))
        .onErrorResume(e -> dbFallback.apply(id));
  }

  private Mono<T> maybeEarlyRefresh(
      String id, String key, T entity, Function<String, Mono<T>> dbFallback) {
    return redis
        .getExpire(key)
        .flatMap(
            remainingTtl -> {
              if (shouldEarlyRefresh(remainingTtl)) {
                dbFallback.apply(id).flatMap(fresh -> put(id, fresh)).subscribe();
              }
              return Mono.just(entity);
            })
        .defaultIfEmpty(entity);
  }

  private boolean shouldEarlyRefresh(Duration remainingTtl) {
    // If TTL is 60 seconds, then the early refresh window is the last 6 seconds
    long earlyRefreshWindowMs = (long) (ttl.toMillis() * EARLY_REFRESH_FRACTION);
    long remainingMs = remainingTtl.toMillis();
    if (remainingMs > earlyRefreshWindowMs) {
      return false;
    }
    double probability = 1.0 - ((double) remainingMs / earlyRefreshWindowMs);
    // With 500 requests all hit the cache when there are 6 seconds remaining.
    // With a fixed rule, all 500 would try to refresh — that's a stampede
    // With probability at 50%, roughly one out of the first few requests will "win" and trigger the
    // refresh
    return ThreadLocalRandom.current().nextDouble() < probability;
  }

  private Mono<T> loadWithLock(String id, String key, Function<String, Mono<T>> dbFallback) {
    String lockKey = key + ":lock";
    return redis
        .opsForValue()
        .setIfAbsent(lockKey, "1", LOCK_TTL)
        .flatMap(
            acquired -> {
              if (Boolean.TRUE.equals(acquired)) {
                return dbFallback
                    .apply(id)
                    .flatMap(entity -> put(id, entity).thenReturn(entity))
                    .doFinally(signal -> redis.delete(lockKey).subscribe());
              }
              return Mono.delay(RETRY_DELAY)
                  .then(redis.opsForValue().get(key))
                  .flatMap(this::deserialize)
                  .switchIfEmpty(Mono.defer(() -> dbFallback.apply(id)));
            });
  }

  public Mono<Boolean> put(String id, T entity) {
    String key = getKey(id);
    return serialize(entity)
        .flatMap(json -> redis.opsForValue().set(key, json, jitteredTtl()))
        .doOnError(e -> log.warn("Cache put failed for key={}", key, e))
        .onErrorResume(e -> Mono.just(false));
  }

  public Mono<Void> evict(String id) {
    String key = getKey(id);
    return redis
        .delete(key)
        .doOnError(e -> log.error("Cache evict failed for key={}", key, e))
        .then();
  }

  private Duration jitteredTtl() {
    // The TTL varying by up to ±15%
    // ThreadLocalRandom.current().nextDouble() = random decimal between 0.0 and 1.0
    // Random value (0 to 1) | After `2 * x - 1` (result is -1 to +1) jitter should go both
    // direction
    long baseMs = ttl.toMillis();
    long jitter =
        (long) (baseMs * JITTER_FACTOR * (2 * ThreadLocalRandom.current().nextDouble() - 1));
    return Duration.ofMillis(baseMs + jitter);
  }

  private String getKey(String id) {
    return keyPrefix + id;
  }

  private Mono<T> deserialize(String json) {
    return Mono.fromCallable(() -> mapper.readValue(json, type));
  }

  private Mono<String> serialize(T entity) {
    return Mono.fromCallable(() -> mapper.writeValueAsString(entity));
  }
}
