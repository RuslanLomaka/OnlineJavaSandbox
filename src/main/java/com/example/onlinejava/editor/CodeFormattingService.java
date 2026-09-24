package com.example.onlinejava.editor;

import com.example.onlinejava.ratelimit.UserRateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Handles one "format" request from the editor: rate limit, optionally
 * optimize imports, then format.
 *
 * <p>Logs one DEBUG line per request (kind, size, duration, outcome) but
 * never the code itself. Enable with
 * {@code logging.level.com.example.onlinejava.editor=DEBUG}.
 */
@Service
public class CodeFormattingService {

  private static final Logger log = LoggerFactory.getLogger(CodeFormattingService.class);

  private final JavaFormatter formatter;

  private final ImportOrganizer importOrganizer;

  private final UserRateLimiter rateLimiter;

  /**
   * Creates the service.
   *
   * @param formatter formats code
   * @param importOrganizer optimizes imports
   * @param rateLimiter per-user limits
   */
  public CodeFormattingService(
      final JavaFormatter formatter,
      final ImportOrganizer importOrganizer,
      final UserRateLimiter rateLimiter
  ) {
    this.formatter = formatter;
    this.importOrganizer = importOrganizer;
    this.rateLimiter = rateLimiter;
  }

  /**
   * Formats code for a user.
   *
   * @param userId current user id
   * @param request code and options
   * @return formatted code and whether it changed
   * @throws UnformattableCodeException if the code has syntax errors
   */
  public FormatResponse format(final long userId, final FormatRequest request) {
    rateLimiter.check(userId, UserRateLimiter.Action.FORMAT);
    final long started = System.nanoTime();
    String outcome = "error";
    try {
      String code = request.code();
      if (request.organizeImports() && request.kind() == SourceKind.CLASS) {
        code = importOrganizer.organize(code);
      }
      code = formatter.format(code, request.kind());
      final boolean changed = !code.equals(request.code());
      outcome = changed ? "changed" : "unchanged";
      return new FormatResponse(code, changed);
    } catch (UnformattableCodeException e) {
      outcome = "syntax-error";
      throw e;
    } finally {
      log.debug("format kind={} imports={} chars={} outcome={} took={}ms",
          request.kind(), request.organizeImports(), request.code().length(), outcome,
          (System.nanoTime() - started) / 1_000_000);
    }
  }
}
