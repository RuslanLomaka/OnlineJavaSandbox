package com.example.onlinejava.problem.arrays;

import com.example.onlinejava.problem.Difficulty;
import com.example.onlinejava.problem.Problem;
import com.example.onlinejava.problem.ProblemDefinition;
import com.example.onlinejava.problem.ProblemType;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Defines the Bubble Sort problem: metadata shown on the problem page
 * and the test harness used to grade a submitted solution.
 */
@Component
public class BubbleSortProblem implements ProblemDefinition {

  /**
   * Returns the Bubble Sort problem's metadata and content.
   *
   * @return the problem definition
   */
  @Override
  public Problem getProblem() {
    return new Problem.Builder("Bubble Sort", ProblemType.SORTING, Difficulty.EASY)
        .slug("bubble-sort")
        .category("Arrays")
        .description("Sort an array using the Bubble Sort algorithm.")
        .methodSignature("public static void bubbleSort(int[] numbers)")
        .starterCode("// Write your solution here")
        .requirements(List.of("Modify the supplied array.", "Sort values from smallest to largest.",
            "Handle negative numbers and duplicates.", "Do not use Arrays.sort().",
            "Do not return a new array."))
        .examples(List.of("Input: [5, 3, 8, 1] → Output: [1, 3, 5, 8]",
            "Input: [4, 4, 2] → Output: [2, 4, 4]",
            "Input: [-2, 5, 0, -1] → Output: [-2, -1, 0, 5]"))
        .hint("Compare neighbouring values. When the left value is larger than the right value, "
            + "exchange them.")
        .build();
  }

  /**
   * Builds the complete Java source used to test a Bubble Sort solution.
   *
   * @param solutionCode the user's submitted method body
   * @return the full {@code Main.java} source, including the test harness
   */
  @Override
  public String buildTestSource(String solutionCode) {
    return """
        import java.util.Arrays;
        
        public class Main {
        
            public static void bubbleSort(int[] numbers) {
        %s
            }
        
            private static int passedTests = 0;
            private static int totalTests = 0;
        
            public static void main(String[] args) {
                test(
                        "empty array",
                        new int[]{},
                        new int[]{}
                );
        
                test(
                        "one value",
                        new int[]{7},
                        new int[]{7}
                );
        
                test(
                        "already sorted",
                        new int[]{1, 2, 3, 4, 5},
                        new int[]{1, 2, 3, 4, 5}
                );
        
                test(
                        "reverse sorted",
                        new int[]{5, 4, 3, 2, 1},
                        new int[]{1, 2, 3, 4, 5}
                );
        
                test(
                        "duplicates",
                        new int[]{4, 2, 4, 1, 2},
                        new int[]{1, 2, 2, 4, 4}
                );
        
                test(
                        "negative values",
                        new int[]{-2, 5, 0, -8, 3},
                        new int[]{-8, -2, 0, 3, 5}
                );
        
                test(
                        "all equal",
                        new int[]{6, 6, 6, 6},
                        new int[]{6, 6, 6, 6}
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
                    int[] input,
                    int[] expected
            ) {
                totalTests++;
        
                int[] originalReference = input;
        
                try {
                    bubbleSort(input);
        
                    boolean correctResult =
                            Arrays.equals(input, expected);
        
                    boolean sameArray =
                            input == originalReference;
        
                    if (correctResult && sameArray) {
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
                                        + Arrays.toString(input)
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