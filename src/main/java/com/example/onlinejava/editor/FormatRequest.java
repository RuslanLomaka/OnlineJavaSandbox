package com.example.onlinejava.editor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/editor/format}.
 *
 * @param code editor contents
 * @param kind whole class (sandbox) or method body (problem pages)
 * @param organizeImports also optimize imports (only applies to
 *     {@link SourceKind#CLASS}); optional, {@code null} means no
 */
public record FormatRequest(
    @NotNull @Size(max = FormatRequest.MAX_CODE_LENGTH) String code,
    @NotNull SourceKind kind,
    Boolean organizeImports
) {

  /** Largest accepted input, in characters. */
  public static final int MAX_CODE_LENGTH = 65_536;

  /**
   * Returns whether imports should be optimized.
   *
   * @return {@code true} only when explicitly requested
   */
  public boolean shouldOrganizeImports() {
    return Boolean.TRUE.equals(organizeImports);
  }
}
