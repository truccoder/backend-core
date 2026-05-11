package com.backend.core.exceptions;

import org.springframework.security.core.AuthenticationException;

public class InvalidTokenException extends AuthenticationException {
  public InvalidTokenException(String message) {
    super(message);
  }
}
