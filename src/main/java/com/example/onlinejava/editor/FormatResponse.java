package com.example.onlinejava.editor;

/**
 * Result of {@code POST /api/editor/format}.
 *
 * @param code the formatted code
 * @param changed whether it differs from the input
 */
public record FormatResponse(String code, boolean changed) {
}
