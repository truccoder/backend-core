package com.backend.core.tsid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.f4b6a3.tsid.TsidFactory;

@Configuration
public class TsidConfig {
  @Value("${POD_NAME}")
  private String podName;

  @Bean
  public TsidFactory tsidFactory() {
    // 1024 = 2^10 (default 10 bits)
    int nodeId = Math.abs(podName.hashCode()) % 1024;
    return TsidFactory.builder().withNode(nodeId).build();
  }
}
