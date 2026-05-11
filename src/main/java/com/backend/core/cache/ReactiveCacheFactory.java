package com.backend.core.cache;

import java.time.Duration;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.backend.core.cache.settings.CacheConnectionSettings;
import com.backend.core.cache.settings.CacheConnectionSettingsProvider;
import com.backend.core.exceptions.ConfigurationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ReactiveCacheFactory {
  private final CacheConnectionSettingsProvider cacheConnectionSettingsProvider;

  @Bean
  @Primary
  public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
    CacheConnectionSettings settings = cacheConnectionSettingsProvider.provide();
    validate(settings);

    log.info("Cache connection — Host: {}", settings.getHost());
    log.info("Cache connection — Port: {}", settings.getPort());
    log.info("Cache connection — TLS: {}", settings.isTlsEnabled());
    log.info("Cache connection — Cluster mode: {}", settings.isClusterMode());

    LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder =
        LettuceClientConfiguration.builder().commandTimeout(Duration.ofMillis(500));

    if (settings.isTlsEnabled()) {
      clientConfigBuilder.useSsl().disablePeerVerification();
    }

    if (settings.isClusterMode()) {
      RedisClusterConfiguration clusterConfig =
          new RedisClusterConfiguration()
              .clusterNode(settings.getHost(), Integer.parseInt(settings.getPort()));
      clusterConfig.setUsername(settings.getUsername());
      clusterConfig.setPassword(RedisPassword.of(settings.getPassword()));
      return new LettuceConnectionFactory(clusterConfig, clientConfigBuilder.build());
    }

    RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
    standaloneConfig.setHostName(settings.getHost());
    standaloneConfig.setPort(Integer.parseInt(settings.getPort()));
    standaloneConfig.setUsername(settings.getUsername());
    standaloneConfig.setPassword(RedisPassword.of(settings.getPassword()));
    return new LettuceConnectionFactory(standaloneConfig, clientConfigBuilder.build());
  }

  @Bean
  public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
      ReactiveRedisConnectionFactory connectionFactory) {
    RedisSerializationContext<String, String> context =
        RedisSerializationContext.<String, String>newSerializationContext(
                new StringRedisSerializer())
            .build();

    return new ReactiveRedisTemplate<>(connectionFactory, context);
  }

  private void validate(CacheConnectionSettings settings) {
    if (StringUtils.isBlank(settings.getHost())) {
      throw new ConfigurationException("Cache connection [host] is not defined");
    }
    if (StringUtils.isBlank(settings.getPort())) {
      throw new ConfigurationException("Cache connection [port] is not defined");
    }
  }
}
