package com.example.onlinejava.problem.datastructures;

import com.example.onlinejava.problem.Difficulty;
import com.example.onlinejava.problem.Problem;
import com.example.onlinejava.problem.ProblemDefinition;
import com.example.onlinejava.problem.Topic;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Defines the Kth Largest Element problem: metadata shown on the problem
 * page and the test harness used to grade a submitted solution.
 */
@Component
public class KthLargestElementProblem implements ProblemDefinition {

  /**
   * Returns the Kth Largest Element problem's metadata and content.
   *
   * @return the problem definition
   */
  @Override
  public Problem getProblem() {
    return new Problem.Builder("Kth Largest Element", Topic.HEAPS, Difficulty.MEDIUM)
        .slug("kth-largest-element")
        .description("Given an array of integers and an integer k, return the kth largest "
            + "element in the array. It is the kth largest in sorted order, not the kth "
            + "distinct element.")
        .methodSignature("public static int findKthLargest(int[] numbers, int k)")
        .starterCode("// Write your solution here")
        .requirements(List.of(
            "Return the kth largest element (k = 1 returns the largest value).",
            "Duplicate values each count toward k on their own.",
            "Assume 1 <= k <= numbers.length.",
            "Do not sort the array with a built-in sort as your only step; a heap is the "
                + "expected approach."
        ))
        .examples(List.of(
            "Input: [3, 2, 1, 5, 6, 4], k = 2 → Output: 5",
            "Input: [3, 2, 3, 1, 2, 4, 5, 5, 6], k = 4 → Output: 4",
            "Input: [7, 10, 4, 3, 20, 15], k = 6 → Output: 3"
        ))
        .hint("Keep a min-heap of size k as you scan the array. Once the heap holds k "
            + "elements, the smallest one on top is the kth largest seen so far.")
        .build();
  }

  /**
   * Builds the complete Java source used to test a Kth Largest Element solution.
   *
   * @param solutionCode the user's submitted method body
   * @return the full {@code Main.java} source, including the test harness
   */
  @Override
  public String buildTestSource(String solutionCode) {
    return """
        public class Main {

            public static int findKthLargest(int[] numbers, int k) {
        %s
            }

            private static int passedTests = 0;
            private static int totalTests = 0;

            public static void main(String[] args) {

                test(
                        "basic case",
                        new int[]{3, 2, 1, 5, 6, 4},
                        2,
                        5
                );

                test(
                        "duplicate values",
                        new int[]{3, 2, 3, 1, 2, 4, 5, 5, 6},
                        4,
                        4
                );

                test(
                        "single element",
                        new int[]{1},
                        1,
                        1
                );

                test(
                        "k equals array length",
                        new int[]{7, 10, 4, 3, 20, 15},
                        6,
                        3
                );

                test(
                        "negative values",
                        new int[]{-1, -2, -3, -4},
                        1,
                        -1
                );

                test(
                        "all equal values",
                        new int[]{5, 5, 5, 5},
                        3,
                        5
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
                    int k,
                    int expected
            ) {
                totalTests++;

                try {
                    int actual = findKthLargest(numbers, k);

                    if (actual == expected) {
                        passedTests++;
                        System.out.println("[PASS] " + name);
                    } else {
                        System.out.println("[FAIL] " + name);
                        System.out.println(
                                "       Expected: "
                                        + expected
                        );
                        System.out.println(
                                "       Actual:   "
                                        + actual
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
