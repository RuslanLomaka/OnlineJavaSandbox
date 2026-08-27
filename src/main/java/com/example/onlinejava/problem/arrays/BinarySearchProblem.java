package com.example.onlinejava.problem.arrays;

import com.example.onlinejava.problem.Difficulty;
import com.example.onlinejava.problem.Problem;
import com.example.onlinejava.problem.ProblemDefinition;
import com.example.onlinejava.problem.ProblemType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BinarySearchProblem implements ProblemDefinition {

    @Override
    public Problem getProblem() {
        return new Problem(
                "binary-search",
                "Binary Search",
                "Arrays",
                ProblemType.SEARCHING,
                Difficulty.EASY,
                "Given a sorted array of integers and a target value, return the index of the target. Return -1 if the target is not present.",
                "public static int binarySearch(int[] numbers, int target)",
                "// Write your solution here",
                List.of(
                        "The input array is sorted in ascending order.",
                        "Return the index of the target if it exists.",
                        "Return -1 if the target is not present.",
                        "Do not use a linear search."
                ),
                List.of(
                        "Input: [1, 3, 5, 7, 9], target = 7 → Output: 3",
                        "Input: [2, 4, 6, 8, 10], target = 5 → Output: -1",
                        "Input: [-5, -2, 0, 3, 8], target = -5 → Output: 0"
                ),
                "Check the middle element and eliminate half of the remaining search space each time."
        );
    }

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