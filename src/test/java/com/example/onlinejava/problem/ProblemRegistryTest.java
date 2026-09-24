package com.example.onlinejava.problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.onlinejava.problem.arrays.BubbleSortProblem;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ProblemRegistry}.
 */
class ProblemRegistryTest {

  private final ProblemRegistry registry = new ProblemRegistry();

  @Test
  void registersBuiltInProblemsBySlug() {
    assertThat(registry.getProblem("bubble-sort")).isInstanceOf(BubbleSortProblem.class);
    assertThat(registry.getProblem("two-sum")).isNotNull();
    assertThat(registry.getProblem("binary-search")).isNotNull();
  }

  @Test
  void returnsNullForUnknownSlug() {
    assertThat(registry.getProblem("does-not-exist")).isNull();
  }

  @Test
  void getAllProblemsReturnsImmutableSnapshot() {
    assertThat(registry.getAllProblems()).hasSize(3);
    assertThatThrownBy(
        () -> registry.getAllProblems().clear()
    ).isInstanceOf(UnsupportedOperationException.class);
  }
}
