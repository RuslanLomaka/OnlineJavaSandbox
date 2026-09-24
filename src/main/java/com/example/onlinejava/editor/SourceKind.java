package com.example.onlinejava.editor;

/**
 * What the editor contains, which decides how it is parsed.
 */
public enum SourceKind {
  /** A whole compilation unit (the sandbox page): imports, classes, ... */
  CLASS,
  /** Only the statements of one method (the problem pages). */
  METHOD_BODY
}
