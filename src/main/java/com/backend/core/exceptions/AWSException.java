package com.backend.core.exceptions;

public class AWSException extends RuntimeException {
  public AWSException(String message) {
    super(message);
  }

  public AWSException(String message, Throwable cause) {
    super(message, cause);
  }

  public AWSException(Throwable cause) {
    super(cause);
  }
}
