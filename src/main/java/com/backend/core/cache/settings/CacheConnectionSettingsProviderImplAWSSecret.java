package com.backend.core.cache.settings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.backend.core.exceptions.AWSException;
import com.backend.core.exceptions.ConfigurationException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.DecryptionFailureException;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.InternalServiceErrorException;
import software.amazon.awssdk.services.secretsmanager.model.InvalidParameterException;
import software.amazon.awssdk.services.secretsmanager.model.InvalidRequestException;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

@Component("awsSecretsCacheConnectionSettingsProvider")
@ConditionalOnProperty(name = "AWS_REGION")
public class CacheConnectionSettingsProviderImplAWSSecret
    implements CacheConnectionSettingsProvider {
  private static final String AWS_CACHE_SECRET_NAME_PARAM = "AWS_CACHE_SECRET_NAME";
  private static final String AWS_REGION = "AWS_REGION";

  private final Environment environment;
  private final SecretsManagerClient secretsManagerClient;
  private final ObjectMapper objectMapper;

  public CacheConnectionSettingsProviderImplAWSSecret(
      Environment environment, ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.environment = environment;
    this.secretsManagerClient = initializeSecretsManagerClient();
  }

  private SecretsManagerClient initializeSecretsManagerClient() {
    String region = assertRequiredEnvironmentParam(AWS_REGION, "AWS Region is not defined");
    return SecretsManagerClient.builder().region(Region.of(region)).build();
  }

  @Override
  public CacheConnectionSettings provide() {
    try {
      return objectMapper.readValue(getSecretJson(), CacheConnectionSettings.class);
    } catch (IOException e) {
      throw new AWSException(e);
    }
  }

  private String getSecretJson() {
    String secretName =
        assertRequiredEnvironmentParam(
            AWS_CACHE_SECRET_NAME_PARAM, "AWS Cache Secret Name is not defined");

    try {
      GetSecretValueRequest request = GetSecretValueRequest.builder().secretId(secretName).build();
      GetSecretValueResponse response = secretsManagerClient.getSecretValue(request);

      if (response.secretString() != null) {
        return response.secretString();
      }
      return new String(
          Base64.getDecoder().decode(response.secretBinary().asByteBuffer()).array(),
          StandardCharsets.UTF_8);
    } catch (DecryptionFailureException
        | InternalServiceErrorException
        | InvalidParameterException
        | InvalidRequestException
        | ResourceNotFoundException e) {
      throw new AWSException(e);
    }
  }

  private String assertRequiredEnvironmentParam(String paramName, String errorMsg) {
    return Optional.ofNullable(environment.getProperty(paramName))
        .orElseThrow(() -> new ConfigurationException(errorMsg));
  }
}
