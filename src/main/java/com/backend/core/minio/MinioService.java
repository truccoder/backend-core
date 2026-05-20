package com.backend.core.minio;

import java.io.ByteArrayInputStream;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@ConditionalOnBean(MinioClient.class)
public class MinioService {
  private final MinioClient minioClient;
  private final MinioProperties properties;

  public MinioService(MinioClient minioClient, MinioProperties properties) {
    this.minioClient = minioClient;
    this.properties = properties;
  }

  public Mono<String> uploadObject(String objectName, byte[] data, String contentType) {
    return Mono.fromCallable(
            () -> {
              minioClient.putObject(
                  PutObjectArgs.builder().bucket(properties.bucket()).object(objectName).stream(
                          new ByteArrayInputStream(data), data.length, -1)
                      .contentType(contentType)
                      .build());
              return getObjectUrl(objectName);
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  public Mono<Void> deleteObject(String objectName) {
    return Mono.fromRunnable(
            () -> {
              try {
                minioClient.removeObject(
                    RemoveObjectArgs.builder()
                        .bucket(properties.bucket())
                        .object(objectName)
                        .build());
              } catch (Exception e) {
                log.warn("Failed to delete MinIO object {}: {}", objectName, e.getMessage());
              }
            })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
  }

  public String getObjectUrl(String objectName) {
    return properties.endpoint() + "/" + properties.bucket() + "/" + objectName;
  }
}
