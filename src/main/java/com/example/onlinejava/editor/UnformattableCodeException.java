package com.example.onlinejava.editor;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * The code has syntax errors, so it can't be formatted. Reported as
 * 422 Unprocessable Content with the first error as the detail message.
 */
public class UnformattableCodeException extends ResponseStatusException {

  /**
   * Creates the exception.
   *
   * @param message user-facing description, e.g. {@code Line 3: Syntax error}
   */
  public UnformattableCodeException(final String message) {
    super(HttpStatus.UNPROCESSABLE_CONTENT, message);
  }

  @Override
  public String getMessage() {
    return getReason();
  }
}
