package com.example.onlinejava.problem.arrays;

import com.example.onlinejava.problem.Difficulty;
import com.example.onlinejava.problem.Problem;
import com.example.onlinejava.problem.ProblemDefinition;
import com.example.onlinejava.problem.ProblemType;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Defines the Binary Search problem: metadata shown on the problem page
 * and the test harness used to grade a submitted solution.
 */
@Component
public class BinarySearchProblem implements ProblemDefinition {

  /**
   * Returns the Binary Search problem's metadata and content.
   *
   * @return the problem definition
   */
  @Override
  public Problem getProblem() {
    return new Problem.Builder("Binary Search", ProblemType.SEARCHING, Difficulty.EASY)
        .slug("binary-search")
        .category("Arrays")
        .description("Given a sorted array of integers and a target value, return the index "
            + "of the target. Return -1 if the target is not present.")
        .methodSignature("public static int binarySearch(int[] numbers, int target)")
        .starterCode("// Write your solution here")
        .requirements(List.of(
            "The input array is sorted in ascending order.",
            "Return the index of the target if it exists.",
            "Return -1 if the target is not present.",
            "Do not use a linear search."
        ))
        .examples(List.of(
            "Input: [1, 3, 5, 7, 9], target = 7 → Output: 3",
            "Input: [2, 4, 6, 8, 10], target = 5 → Output: -1",
            "Input: [-5, -2, 0, 3, 8], target = -5 → Output: 0"
        ))
        .hint(
            "Check the middle element and eliminate half of the remaining search space each time.")
        .build();
  }

  /**
   * Builds the complete Java source used to test a Binary Search solution.
   *
   * @param solutionCode the user's submitted method body
   * @return the full {@code Main.java} source, including the test harness
   */
  @Override
  public String buildTestSource(String solutionCode) {
    return """
        public class Main {
        
            public static int binarySearch(
                    int[] numbers,
                    int target
            ) {
        %s
            }
        
            private static int passedTests = 0;
            private static int totalTests = 0;
        
            public static void main(String[] args) {
        
                test(
                        "target in middle",
                        new int[]{1, 3, 5, 7, 9},
                        7,
                        3
                );
        
                test(
                        "target not present",
                        new int[]{2, 4, 6, 8, 10},
                        5,
                        -1
                );
        
                test(
                        "target at beginning",
                        new int[]{-5, -2, 0, 3, 8},
                        -5,
                        0
                );
        
                test(
                        "target at end",
                        new int[]{1, 4, 6, 9, 12},
                        12,
                        4
                );
        
                test(
                        "single element",
                        new int[]{8},
                        8,
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
                    int[] numbers,
                    int target,
                    int expected
            ) {
                totalTests++;
        
                try {
                    int actual = binarySearch(numbers, target);
        
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