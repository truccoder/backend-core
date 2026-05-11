package com.backend.core.exceptions;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.*;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.logging.Level;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import com.backend.core.dtos.ErrorResponseDto;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
  private static final String INVALID_CREDS_MESSAGE = "Invalid credentials";
  private static final String INVALIDATION_MESSAGE = "Invalid request parameters or payload";
  private static final String FIELD_VALIDATION_MSG_TEMPLATE = "Property %s: %s";
  private static final String PAYLOAD_VALIDATION_MSG_TEMPLATE = "Payload: %s";

  @ResponseStatus(FORBIDDEN)
  @ExceptionHandler(ForbiddenException.class)
  public ErrorResponseDto handle(ForbiddenException ex, ServerHttpRequest request) {
    writeLog(ex, request);
    return ErrorResponseDto.builder()
        .code(HttpStatus.FORBIDDEN.value())
        .error(HttpStatus.FORBIDDEN.getReasonPhrase())
        .message(ex.getMessage())
        .path(request.getURI().getPath())
        .build();
  }

  @ResponseStatus(BAD_REQUEST)
  @ExceptionHandler(ValidationException.class)
  public ErrorResponseDto handle(ValidationException ex, ServerHttpRequest request) {
    writeLog(ex, request);
    return ErrorResponseDto.builder()
        .code(BAD_REQUEST.value())
        .error(BAD_REQUEST.getReasonPhrase())
        .message(ex.getMessage())
        .path(request.getURI().getPath())
        .build();
  }

  @ResponseStatus(UNPROCESSABLE_ENTITY)
  @ExceptionHandler(jakarta.validation.ValidationException.class)
  protected ErrorResponseDto handle(
      jakarta.validation.ValidationException ex, ServerHttpRequest request) {
    writeLog(ex, request);
    return ErrorResponseDto.builder()
        .code(BAD_REQUEST.value())
        .error(BAD_REQUEST.getReasonPhrase())
        .message(ex.getMessage())
        .path(request.getURI().getPath())
        .build();
  }

  @ResponseStatus(NOT_FOUND)
  @ExceptionHandler(ResourceNotFoundException.class)
  public ErrorResponseDto handle(ResourceNotFoundException ex, ServerHttpRequest request) {
    writeLog(ex, request);
    return ErrorResponseDto.builder()
        .code(HttpStatus.NOT_FOUND.value())
        .error(HttpStatus.NOT_FOUND.getReasonPhrase())
        .message(ex.getMessage())
        .path(request.getURI().getPath())
        .build();
  }

  @ResponseStatus(UNAUTHORIZED)
  @ExceptionHandler(AuthenticationException.class)
  public ErrorResponseDto handle(AuthenticationException ex, ServerHttpRequest request) {
    writeLog(ex, request);
    return ErrorResponseDto.builder()
        .code(HttpStatus.UNAUTHORIZED.value())
        .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
        .message(INVALID_CREDS_MESSAGE)
        .path(request.getURI().getPath())
        .build();
  }

  @ResponseStatus(UNPROCESSABLE_ENTITY)
  @ExceptionHandler(WebExchangeBindException.class)
  public ErrorResponseDto handle(WebExchangeBindException ex, ServerHttpRequest request) {
    writeLog(ex, request);
    ErrorResponseDto errorDto =
        ErrorResponseDto.builder()
            .code(UNPROCESSABLE_ENTITY.value())
            .error(UNPROCESSABLE_ENTITY.getReasonPhrase())
            .message(INVALIDATION_MESSAGE)
            .path(request.getURI().getPath())
            .build();
    errorDto.setDetails(
        ex.getBindingResult().getAllErrors().stream()
            .map(
                error -> {
                  if (error instanceof FieldError fieldError) {
                    return format(
                        FIELD_VALIDATION_MSG_TEMPLATE,
                        fieldError.getField(),
                        fieldError.getDefaultMessage());
                  }
                  return format(PAYLOAD_VALIDATION_MSG_TEMPLATE, error.getDefaultMessage());
                })
            .collect(toList()));
    return errorDto;
  }

  @ResponseStatus(INTERNAL_SERVER_ERROR)
  @ExceptionHandler(Exception.class)
  public ErrorResponseDto handle(Exception ex, ServerHttpRequest request) {
    writeLog(ex, request);
    return ErrorResponseDto.builder()
        .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
        .message("Internal server error")
        .timestamp(OffsetDateTime.now())
        .path(request.getURI().getPath())
        .build();
  }

  protected void writeLog(Throwable throwable, ServerHttpRequest request) {
    String errorMessage = getErrorLogMessage(throwable, request);
    Level level = getErrorLevel(throwable);

    if (level == Level.INFO) {
      logInfo(errorMessage, throwable);
    } else if (level == Level.WARNING) {
      logWarn(errorMessage, throwable);
    } else {
      logError(errorMessage, throwable);
    }
  }

  protected void logInfo(String message, Throwable throwable) {
    if (log.isInfoEnabled()) {
      if (Objects.isNull(throwable) || throwable instanceof ResourceNotFoundException) {
        log.info(message);
      } else {
        log.info(message, throwable);
      }
    }
  }

  protected void logError(String message, Throwable throwable) {
    if (log.isErrorEnabled()) {
      if (Objects.isNull(throwable)) {
        log.error(message);
      } else {
        log.error(message, throwable);
      }
    }
  }

  protected void logWarn(String message, Throwable throwable) {
    if (log.isWarnEnabled()) {
      if (Objects.isNull(throwable)) {
        log.warn(message);
      } else {
        log.warn(message, throwable);
      }
    }
  }

  protected String getErrorLogMessage(Throwable throwable, ServerHttpRequest request) {
    if (Objects.isNull(request)) {
      return String.format("API request failed: %s", throwable.getMessage());
    }
    return String.format(
        "%s %s failed: %s",
        request.getMethod(), request.getURI().getPath(), throwable.getMessage());
  }

  protected Level getErrorLevel(Throwable throwable) {
    if (throwable instanceof AuthenticationException
        || throwable instanceof AccessDeniedException
        || throwable instanceof ResourceNotFoundException) {
      return Level.INFO;
    }
    if (throwable instanceof ValidationException
        || throwable instanceof jakarta.validation.ValidationException
        || throwable instanceof WebExchangeBindException) {
      return Level.WARNING;
    }
    return Level.SEVERE;
  }
}
