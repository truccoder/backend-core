package com.backend.core.cache.settings;

import lombok.Data;

@Data
public class CacheConnectionSettings {
  private String host;
  private String port;
  private String username;
  private String password;
  private boolean tlsEnabled;
  // True for MemoryDB cluster, false for ElastiCache standalone
  private boolean clusterMode;
}
