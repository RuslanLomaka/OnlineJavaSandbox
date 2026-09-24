package com.example.onlinejava.problem;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Checks the contract every registered {@link ProblemDefinition} must
 * honour, so newly added problems are covered automatically.
 */
class ProblemDefinitionContractTest {

  static Stream<ProblemDefinition> definitions() {
    return new ProblemRegistry().getAllProblems().stream();
  }

  @ParameterizedTest
  @MethodSource("definitions")
  void problemMetadataIsComplete(final ProblemDefinition definition) {
    final Problem problem = definition.getProblem();

    assertThat(problem.getSlug()).matches("[a-z0-9]+(-[a-z0-9]+)*");
    assertThat(problem.getTitle()).isNotBlank();
    assertThat(problem.getCategory()).isNotBlank();
    assertThat(problem.getType()).isNotNull();
    assertThat(problem.getDifficulty()).isNotNull();
    assertThat(problem.getMethodSignature()).isNotBlank();
  }

  @ParameterizedTest
  @MethodSource("definitions")
  void testSourceEmbedsSolutionInPublicMainClass(final ProblemDefinition definition) {
    final String marker = "/* SOLUTION-MARKER-42 */";

    final String source = definition.buildTestSource(marker);

    assertThat(source)
        .contains("public class Main")
        .contains("public static void main(String[] args)")
        .contains(marker);
  }
}
