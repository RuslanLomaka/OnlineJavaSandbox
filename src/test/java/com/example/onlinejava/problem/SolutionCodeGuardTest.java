package com.example.onlinejava.problem;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link SolutionCodeGuard}.
 */
class SolutionCodeGuardTest {

  static Stream<String> everyRealSolutionFile() {
    return new ProblemRegistry().getAllProblems().stream()
        .map(definition -> definition.getProblem().getSlug())
        .flatMap(slug -> Stream.of(slug + ".txt", slug + ".wrong.txt"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("everyRealSolutionFile")
  void acceptsEveryRealReferenceSolution(final String fileName) throws IOException {
    final String solution = readSolution(fileName);

    assertThatCode(() -> SolutionCodeGuard.checkBalancedBraces(solution))
        .as("Real reference solution %s was rejected", fileName)
        .doesNotThrowAnyException();
  }

  private static String readSolution(final String fileName) throws IOException {
    try (InputStream in = SolutionCodeGuardTest.class
        .getResourceAsStream("/solutions/" + fileName)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Test
  void acceptsSimpleBalancedBody() {
    assertThatCode(() -> SolutionCodeGuard.checkBalancedBraces("return 1;"))
        .doesNotThrowAnyException();
  }

  @Test
  void acceptsNestedBalancedBraces() {
    assertThatCode(() -> SolutionCodeGuard.checkBalancedBraces(
        "if (x > 0) {\n    return 1;\n} else {\n    return 0;\n}"))
        .doesNotThrowAnyException();
  }

  @Test
  void rejectsTheStaticInitializerEscapeExploit() {
    assertThatThrownBy(() -> SolutionCodeGuard.checkBalancedBraces(
        "return 1; } static { System.out.println(\"All tests passed!\"); System.exit(0);"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unexpected '}'");
  }

  @Test
  void rejectsUnclosedBrace() {
    assertThatThrownBy(() -> SolutionCodeGuard.checkBalancedBraces("if (x > 0) {\n    return 1;"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must be closed");
  }

  @Test
  void ignoresBracesInsideStringLiteral() {
    assertThatCode(() -> SolutionCodeGuard.checkBalancedBraces("return \"}\";"))
        .doesNotThrowAnyException();
  }

  @Test
  void ignoresEscapedQuoteInsideStringLiteral() {
    assertThatCode(() -> SolutionCodeGuard.checkBalancedBraces("return \"\\\"}\";"))
        .doesNotThrowAnyException();
  }

  @Test
  void ignoresBracesInsideLineComment() {
    assertThatCode(() -> SolutionCodeGuard.checkBalancedBraces("return 1; // }"))
        .doesNotThrowAnyException();
  }

  @Test
  void ignoresBracesInsideBlockComment() {
    assertThatCode(() -> SolutionCodeGuard.checkBalancedBraces("return 1; /* } */"))
        .doesNotThrowAnyException();
  }

  @Test
  void ignoresBracesInsideTextBlock() {
    assertThatCode(() -> SolutionCodeGuard.checkBalancedBraces(
        "String s = \"\"\"\n}\n\"\"\";\nreturn 1;"))
        .doesNotThrowAnyException();
  }

  @Test
  void ignoresBracesInsideCharLiteral() {
    assertThatCode(() -> SolutionCodeGuard.checkBalancedBraces("char c = '}'; return 1;"))
        .doesNotThrowAnyException();
  }
}
