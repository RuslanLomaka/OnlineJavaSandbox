package com.example.onlinejava.problem;

/**
 * A coding problem's metadata and the test harness used to grade a
 * submitted solution. Implemented once per problem.
 */
public interface ProblemDefinition {

  /**
   * Returns the problem's metadata and content.
   *
   * @return the problem definition
   */
  Problem getProblem();

  /**
   * Builds the complete Java source used to test a submitted solution.
   *
   * @param solutionCode the user's submitted method body
   * @return the full {@code Main.java} source, including the test harness
   */
  String buildTestSource(String solutionCode);
}