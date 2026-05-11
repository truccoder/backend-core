package com.backend.core.exceptions;

public class ResourceNotFoundException extends RuntimeException {
  private static final String MSG_TEMPLATE = "%s resource with id %s does not exist.";

  public ResourceNotFoundException(String message) {
    super(message);
  }

  public ResourceNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public ResourceNotFoundException(Object resourceId, String resourceName) {
    super(String.format(MSG_TEMPLATE, resourceName, resourceId.toString()));
  }

  public ResourceNotFoundException(Object resourceId, String resourceName, Throwable cause) {
    super(String.format(MSG_TEMPLATE, resourceName, resourceId.toString()), cause);
  }
}
