package com.example.onlinejava.problem.arrays;

import com.example.onlinejava.problem.Difficulty;
import com.example.onlinejava.problem.Problem;
import com.example.onlinejava.problem.ProblemDefinition;
import com.example.onlinejava.problem.ProblemType;
import java.util.List;
import org.springframework.stereotype.Component;


/**
 * Defines the Two Sum problem: metadata shown on the problem page and
 * the test harness used to grade a submitted solution.
 */
@Component
public class TwoSumProblem implements ProblemDefinition {

  /**
   * Returns the Two Sum problem's metadata and content.
   *
   * @return the problem definition
   */
  @Override
  public Problem getProblem() {
    return new Problem.Builder("Two Sum", ProblemType.HASHING, Difficulty.EASY)
        .slug("two-sum")
        .category("Arrays")
        .description("Given an array of integers and a target value, return the indices "
            + "of two numbers whose sum equals the target.")
        .methodSignature("public static int[] twoSum(int[] numbers, int target)")
        .starterCode("// Write your solution here")
        .requirements(List.of(
            "Return exactly two indices.",
            "The two selected values must add up to the target.",
            "Do not use the same element twice.",
            "Assume exactly one valid solution exists."
        ))
        .examples(List.of(
            "Input: [2, 7, 11, 15], target = 9 → Output: [0, 1]",
            "Input: [3, 2, 4], target = 6 → Output: [1, 2]",
            "Input: [3, 3], target = 6 → Output: [0, 1]"
        ))
        .hint("Try using a hash map to remember values you have already seen.")
        .build();
  }

  /**
   * Builds the complete Java source used to test a Two Sum solution.
   *
   * @param solutionCode the user's submitted method body
   * @return the full {@code Main.java} source, including the test harness
   */
  @Override
  public String buildTestSource(String solutionCode) {
    return """
        import java.util.Arrays;
        import java.util.HashMap;
        import java.util.Map;
        
        public class Main {
        
            public static int[] twoSum(int[] numbers, int target) {
        %s
            }
        
            private static int passedTests = 0;
            private static int totalTests = 0;
        
            public static void main(String[] args) {
        
                test(
                        "basic case",
                        new int[]{2, 7, 11, 15},
                        9,
                        new int[]{0, 1}
                );
        
                test(
                        "middle values",
                        new int[]{3, 2, 4},
                        6,
                        new int[]{1, 2}
                );
        
                test(
                        "duplicate values",
                        new int[]{3, 3},
                        6,
                        new int[]{0, 1}
                );
        
                test(
                        "negative values",
                        new int[]{-3, 4, 7, -1},
                        1,
                        new int[]{0, 1}
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
        
            // Checks whether the user's result matches the expected indices.
            private static void test(
                    String name,
                    int[] numbers,
                    int target,
                    int[] expected
            ) {
                totalTests++;
        
                try {
                    int[] actual = twoSum(numbers, target);
        
                    if (actual == null || actual.length != 2) {
                        System.out.println("[FAIL] " + name);
                        System.out.println(
                                "       Expected exactly two indices."
                        );
                        return;
                    }
        
                    int[] sortedActual = actual.clone();
                    int[] sortedExpected = expected.clone();
        
                    Arrays.sort(sortedActual);
                    Arrays.sort(sortedExpected);
        
                    if (Arrays.equals(
                            sortedActual,
                            sortedExpected
                    )) {
                        passedTests++;
                        System.out.println("[PASS] " + name);
                    } else {
                        System.out.println("[FAIL] " + name);
                        System.out.println(
                                "       Expected: "
                                        + Arrays.toString(expected)
                        );
                        System.out.println(
                                "       Actual:   "
                                        + Arrays.toString(actual)
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