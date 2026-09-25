package com.example.onlinejava.problem.datastructures;

import com.example.onlinejava.problem.Difficulty;
import com.example.onlinejava.problem.Problem;
import com.example.onlinejava.problem.ProblemDefinition;
import com.example.onlinejava.problem.Topic;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Defines the Maximum Subarray problem: metadata shown on the problem page
 * and the test harness used to grade a submitted solution.
 */
@Component
public class MaximumSubarrayProblem implements ProblemDefinition {

  /**
   * Returns the Maximum Subarray problem's metadata and content.
   *
   * @return the problem definition
   */
  @Override
  public Problem getProblem() {
    return new Problem.Builder("Maximum Subarray (Kadane's Algorithm)", Topic.ARRAYS,
        Difficulty.MEDIUM)
        .slug("maximum-subarray")
        .description("Given an array of integers, find the contiguous subarray "
            + "(containing at least one number) with the largest sum, and return that sum.")
        .methodSignature("public static int maxSubArray(int[] numbers)")
        .requirements(List.of(
            "The array has at least one element.",
            "The subarray must be contiguous and non-empty.",
            "Handle arrays where every value is negative.",
            "Array values are between -10,000 and 10,000, inclusive.",
            "Run in O(n) time."
        ))
        .examples(List.of(
            "Input: [-2, 1, -3, 4, -1, 2, 1, -5, 4] → Output: 6 (subarray [4, -1, 2, 1])",
            "Input: [1] → Output: 1",
            "Input: [-3, -1, -2] → Output: -1"
        ))
        .hint(
            "Track the best sum ending at the current index. Either extend the previous "
                + "subarray or start a new one at the current value, whichever is larger.")
        .build();
  }

  /**
   * Builds the complete Java source used to test a Maximum Subarray solution.
   *
   * @param solutionCode the user's submitted method body
   * @return the full {@code Main.java} source, including the test harness
   */
  @Override
  public String buildTestSource(String solutionCode) {
    return """
        public class Main {

            public static int maxSubArray(int[] numbers) {
        %s
            }

            private static int passedTests = 0;
            private static int totalTests = 0;

            public static void main(String[] args) {

                test(
                        "single element",
                        new int[]{1},
                        1
                );

                test(
                        "mixed positive and negative",
                        new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4},
                        6
                );

                test(
                        "all negative",
                        new int[]{-3, -1, -2},
                        -1
                );

                test(
                        "all positive",
                        new int[]{1, 2, 3, 4},
                        10
                );

                test(
                        "all equal negative",
                        new int[]{-5, -5, -5},
                        -5
                );

                test(
                        "single negative then large positive",
                        new int[]{-1, 100},
                        100
                );

                test(
                        "values at the documented bound",
                        new int[]{10000, -10000, 10000, 10000, -5000, 10000},
                        25000
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
                    int[] numbers,
                    int expected
            ) {
                totalTests++;

                try {
                    int actual = maxSubArray(numbers);

                    if (actual == expected) {
                        passedTests++;
                        System.out.println("[PASS] " + name);
                    } else {
                        System.out.println("[FAIL] " + name);
                        System.out.println(
                                "       Expected: " + expected
                        );
                        System.out.println(
                                "       Actual:   " + actual
                        );
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
