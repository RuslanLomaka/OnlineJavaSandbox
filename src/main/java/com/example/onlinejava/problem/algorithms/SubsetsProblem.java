package com.example.onlinejava.problem.algorithms;

import com.example.onlinejava.problem.Difficulty;
import com.example.onlinejava.problem.Problem;
import com.example.onlinejava.problem.ProblemDefinition;
import com.example.onlinejava.problem.Topic;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Defines the Subsets (Power Set) problem: metadata shown on the problem
 * page and the test harness used to grade a submitted solution.
 */
@Component
public class SubsetsProblem implements ProblemDefinition {

  /**
   * Returns the Subsets (Power Set) problem's metadata and content.
   *
   * @return the problem definition
   */
  @Override
  public Problem getProblem() {
    return new Problem.Builder("Subsets (Power Set)", Topic.RECURSION, Difficulty.MEDIUM)
        .slug("subsets")
        .description("Given an array of unique integers, return every possible subset "
            + "(the power set). The subsets may be returned in any order, and the order of "
            + "the elements within each subset does not matter.")
        .methodSignature("public static List<List<Integer>> subsets(int[] nums)")
        .starterCode("// Write your solution here")
        .requirements(List.of(
            "Return all 2^n subsets, including the empty subset and the full array.",
            "Do not return duplicate subsets.",
            "Assume every value in nums is unique.",
            "Handle an empty input array (it has exactly one subset: the empty one)."
        ))
        .examples(List.of(
            "Input: [1, 2] → Output: [[], [1], [2], [1, 2]]",
            "Input: [0] → Output: [[], [0]]",
            "Input: [] → Output: [[]]"
        ))
        .hint("Start with just the empty subset. For each number, take every subset built so "
            + "far and add a copy of it with that number included.")
        .build();
  }

  /**
   * Builds the complete Java source used to test a Subsets solution.
   *
   * @param solutionCode the user's submitted method body
   * @return the full {@code Main.java} source, including the test harness
   */
  @Override
  public String buildTestSource(String solutionCode) {
    return """
        import java.util.ArrayList;
        import java.util.Arrays;
        import java.util.Collections;
        import java.util.HashSet;
        import java.util.List;
        import java.util.Set;

        public class Main {

            public static List<List<Integer>> subsets(int[] nums) {
        %s
            }

            private static int passedTests = 0;
            private static int totalTests = 0;

            public static void main(String[] args) {

                test("empty array", new int[]{});
                test("single element", new int[]{1});
                test("two elements", new int[]{1, 2});
                test("three elements", new int[]{1, 2, 3});
                test("negative values", new int[]{-2, 0, 3});
                test("four elements", new int[]{4, 1, 3, 2});

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

            private static void test(String name, int[] nums) {
                totalTests++;

                try {
                    List<List<Integer>> actual = subsets(nums);
                    int expectedCount = 1 << nums.length;

                    if (actual == null) {
                        System.out.println("[FAIL] " + name);
                        System.out.println(
                                "       Expected " + expectedCount + " subsets, got null"
                        );
                        return;
                    }

                    Set<List<Integer>> actualCanonical = new HashSet<>();
                    for (List<Integer> subset : actual) {
                        List<Integer> sorted = new ArrayList<>(subset);
                        Collections.sort(sorted);
                        actualCanonical.add(sorted);
                    }

                    Set<List<Integer>> expectedCanonical = expectedSubsets(nums);

                    if (actual.size() != expectedCount
                            || actualCanonical.size() != expectedCount
                            || !actualCanonical.equals(expectedCanonical)) {
                        System.out.println("[FAIL] " + name);
                        System.out.println("       Input:    " + Arrays.toString(nums));
                        System.out.println(
                                "       Expected " + expectedCount + " unique subsets"
                        );
                        System.out.println(
                                "       Actual:   " + actual.size() + " subsets -> " + actual
                        );
                    } else {
                        passedTests++;
                        System.out.println("[PASS] " + name);
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

            private static Set<List<Integer>> expectedSubsets(int[] nums) {
                Set<List<Integer>> result = new HashSet<>();
                int n = nums.length;

                for (int mask = 0; mask < (1 << n); mask++) {
                    List<Integer> subset = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        if ((mask & (1 << i)) != 0) {
                            subset.add(nums[i]);
                        }
                    }
                    Collections.sort(subset);
                    result.add(subset);
                }

                return result;
            }
        }
        """.formatted(solutionCode);
  }
}
