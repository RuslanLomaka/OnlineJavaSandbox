package com.example.onlinejava.problem;

/**
 * Rejects a submitted method body that could escape the single method-body
 * slot a {@link ProblemDefinition} splices it into.
 *
 * <p>{@link ProblemDefinition#buildTestSource} pastes the user's solution
 * code verbatim between one open and one close brace, with the hidden test
 * harness immediately below. Without this check, a submission can close
 * that brace early and open a {@code static} initializer in the space the
 * harness expects to own -- static initializers run at class-load time,
 * before the harness's own {@code main} method, so a submission like
 * {@code "return 0; } static { System.out.println(\"All tests passed!\");
 * System.exit(0);"} prints the harness's own success marker and exits 0
 * without ever running a single hidden test.
 */
public final class SolutionCodeGuard {

  private SolutionCodeGuard() {
  }

  /**
   * Checks that every brace the submission opens is also closed within the
   * submission itself, so splicing it into one method body can never close
   * that method early or leave a brace open for the harness that follows to
   * unexpectedly close.
   *
   * @param solutionCode the submitted method body
   * @throws IllegalArgumentException if a closing brace has no matching
   *     open brace earlier in the submission, or an open brace is never
   *     closed
   */
  public static void checkBalancedBraces(String solutionCode) {
    int depth = 0;
    boolean inLineComment = false;
    boolean inBlockComment = false;
    boolean inTextBlock = false;
    boolean inString = false;
    boolean inChar = false;

    int index = 0;
    int length = solutionCode.length();

    while (index < length) {
      char current = solutionCode.charAt(index);
      char next = index + 1 < length ? solutionCode.charAt(index + 1) : '\0';

      if (inLineComment) {
        inLineComment = current != '\n';
        index++;
        continue;
      }

      if (inBlockComment) {
        if (current == '*' && next == '/') {
          inBlockComment = false;
          index += 2;
        } else {
          index++;
        }
        continue;
      }

      if (inTextBlock) {
        if (current == '\\') {
          index += 2;
        } else if (isTripleQuote(solutionCode, index)) {
          inTextBlock = false;
          index += 3;
        } else {
          index++;
        }
        continue;
      }

      if (inString) {
        if (current == '\\') {
          index += 2;
        } else {
          inString = current != '"';
          index++;
        }
        continue;
      }

      if (inChar) {
        if (current == '\\') {
          index += 2;
        } else {
          inChar = current != '\'';
          index++;
        }
        continue;
      }

      if (current == '/' && next == '/') {
        inLineComment = true;
        index += 2;
      } else if (current == '/' && next == '*') {
        inBlockComment = true;
        index += 2;
      } else if (isTripleQuote(solutionCode, index)) {
        inTextBlock = true;
        index += 3;
      } else if (current == '"') {
        inString = true;
        index++;
      } else if (current == '\'') {
        inChar = true;
        index++;
      } else if (current == '{') {
        depth++;
        index++;
      } else if (current == '}') {
        depth--;
        if (depth < 0) {
          throw new IllegalArgumentException(
              "Unexpected '}' with no matching '{' earlier in the submission.");
        }
        index++;
      } else {
        index++;
      }
    }

    if (depth != 0) {
      throw new IllegalArgumentException(
          "Every '{' in the submission must be closed within the submission.");
    }
  }

  private static boolean isTripleQuote(String text, int index) {
    return index + 2 < text.length()
        && text.charAt(index) == '"'
        && text.charAt(index + 1) == '"'
        && text.charAt(index + 2) == '"';
  }
}
