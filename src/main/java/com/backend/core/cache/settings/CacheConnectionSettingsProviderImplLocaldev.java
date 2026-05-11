package com.backend.core.cache.settings;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component("localdevCacheConnectionSettingsProvider")
@ConditionalOnBean(LocaldevCacheSettings.class)
@RequiredArgsConstructor
public class CacheConnectionSettingsProviderImplLocaldev
    implements CacheConnectionSettingsProvider {
  private final LocaldevCacheSettings localdevCacheSettings;

  @Override
  public CacheConnectionSettings provide() {
    CacheConnectionSettings settings = new CacheConnectionSettings();
    settings.setHost(localdevCacheSettings.getHost());
    settings.setPort(localdevCacheSettings.getPort());
    settings.setUsername(localdevCacheSettings.getUsername());
    settings.setPassword(localdevCacheSettings.getPassword());
    settings.setTlsEnabled(false);
    settings.setClusterMode(localdevCacheSettings.isClusterMode());
    return settings;
  }
}
