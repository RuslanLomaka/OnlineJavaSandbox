package com.example.onlinejava.editor;

import com.example.onlinejava.user.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * JSON API behind the editor's Ctrl+Alt+L / Ctrl+Alt+O shortcuts.
 */
@RestController
public class EditorController {

  private final CodeFormattingService service;

  /**
   * Creates the controller.
   *
   * @param service formatting service
   */
  public EditorController(final CodeFormattingService service) {
    this.service = service;
  }

  /**
   * Formats code (and optionally optimizes imports).
   *
   * @param request code and options
   * @param user current user
   * @return formatted code; 422 with the first syntax error if unparseable
   */
  @PostMapping("/api/editor/format")
  public FormatResponse format(
      @Valid @RequestBody final FormatRequest request,
      @AuthenticationPrincipal final AppUserPrincipal user
  ) {
    if (user == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in");
    }
    return service.format(user.getUserId(), request);
  }
}
