package com.backend.core.minio;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "minio", name = "endpoint")
@EnableConfigurationProperties(MinioProperties.class)
public class MinioClientFactory {
  private final MinioProperties properties;

  public MinioClientFactory(MinioProperties properties) {
    this.properties = properties;
  }

  @Bean
  public MinioClient minioClient() {
    MinioClient client =
        MinioClient.builder()
            .endpoint(properties.endpoint())
            .credentials(properties.accessKey(), properties.secretKey())
            .build();
    ensureBucketExists(client);
    return client;
  }

  private void ensureBucketExists(MinioClient client) {
    try {
      boolean exists =
          client.bucketExists(BucketExistsArgs.builder().bucket(properties.bucket()).build());
      if (!exists) {
        client.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket()).build());
        log.info("Created MinIO bucket: {}", properties.bucket());
      }
    } catch (Exception e) {
      log.warn("Failed to ensure MinIO bucket exists: {}", e.getMessage());
    }
  }
}
