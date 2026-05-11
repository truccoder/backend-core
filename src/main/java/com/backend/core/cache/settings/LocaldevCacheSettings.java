package com.backend.core.cache.settings;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "cache")
public class LocaldevCacheSettings {
  private String host;
  private String port;
  private String username;
  private String password;
  private boolean clusterMode;
}
