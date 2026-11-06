package com.example.onlinejava.problem.algorithms;

import com.example.onlinejava.problem.Difficulty;
import com.example.onlinejava.problem.Problem;
import com.example.onlinejava.problem.ProblemDefinition;
import com.example.onlinejava.problem.Topic;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Defines the Container With Most Water problem: metadata shown on the
 * problem page and the test harness used to grade a submitted solution.
 */
@Component
public class ContainerWithMostWaterProblem implements ProblemDefinition {

  /**
   * Returns the Container With Most Water problem's metadata and content.
   *
   * @return the problem definition
   */
  @Override
  public Problem getProblem() {
    return new Problem.Builder("Container With Most Water", Topic.TWO_POINTERS,
        Difficulty.MEDIUM)
        .slug("container-with-most-water")
        .description("You are given an array of non-negative integers where each value "
            + "represents the height of a vertical line drawn at that index. Find two lines "
            + "that, together with the x-axis, form a container that holds the most water, "
            + "and return the amount of water it can hold.")
        .methodSignature("public static int maxArea(int[] height)")
        .starterCode("// Write your solution here")
        .requirements(List.of(
            "The array has at least two heights.",
            "The container's width is the distance between the two chosen indices.",
            "The container's height is limited by the shorter of the two chosen lines.",
            "Return the largest amount of water any pair of lines can hold."
        ))
        .examples(List.of(
            "Input: [1, 8, 6, 2, 5, 4, 8, 3, 7] → Output: 49",
            "Input: [1, 1] → Output: 1",
            "Input: [4, 4, 4, 4] → Output: 12"
        ))
        .hint("Start with the widest container, using pointers at both ends. Moving the "
            + "pointer at the shorter line inward is the only move that can increase the "
            + "area, since the width always shrinks and the shorter line otherwise caps the "
            + "height anyway.")
        .build();
  }

  /**
   * Builds the complete Java source used to test a Container With Most Water solution.
   *
   * @param solutionCode the user's submitted method body
   * @return the full {@code Main.java} source, including the test harness
   */
  @Override
  public String buildTestSource(String solutionCode) {
    return """
        public class Main {

            public static int maxArea(int[] height) {
        %s
            }

            private static int passedTests = 0;
            private static int totalTests = 0;

            public static void main(String[] args) {

                test(
                        "LeetCode example",
                        new int[]{1, 8, 6, 2, 5, 4, 8, 3, 7},
                        49
                );

                test(
                        "two equal heights",
                        new int[]{1, 1},
                        1
                );

                test(
                        "two different heights",
                        new int[]{4, 3},
                        3
                );

                test(
                        "increasing heights",
                        new int[]{1, 2, 3, 4, 5, 6},
                        9
                );

                test(
                        "decreasing heights",
                        new int[]{6, 5, 4, 3, 2, 1},
                        9
                );

                test(
                        "all equal heights",
                        new int[]{4, 4, 4, 4},
                        12
                );

                test(
                        "tall spikes at both ends",
                        new int[]{9, 1, 1, 1, 9},
                        36
                );

                test(
                        "zero height lines",
                        new int[]{0, 2, 0},
                        0
                );

                System.out.println();
                System.out.println(
                        "Passed "
                                + passedTests
                                + " of "
                                + totalTests
                                + " tests."
                );

                if (passedTests == totalTests) {
                    System.out.println("All tests passed!");
                } else {
                    System.exit(1);
                }
            }

            private static void test(
                    String name,
                    int[] height,
                    int expected
            ) {
                totalTests++;

                try {
                    int actual = maxArea(height);

                    if (actual == expected) {
                        passedTests++;

                        System.out.println("[PASS] " + name);

                    } else {
                        System.out.println("[FAIL] " + name);
                        System.out.println("       Expected: " + expected);
                        System.out.println("       Actual:   " + actual);
                    }

                } catch (Exception exception) {
                    System.out.println("[ERROR] " + name);
                    System.out.println(
                            "        "
                                    + exception.getClass().getSimpleName()
                                    + ": "
                                    + exception.getMessage()
                    );
                }
            }
        }
        """.formatted(solutionCode);
  }
}
