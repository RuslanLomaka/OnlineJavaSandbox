package com.example.onlinejava.problem.arrays;

import com.example.onlinejava.problem.Difficulty;
import com.example.onlinejava.problem.Problem;
import com.example.onlinejava.problem.ProblemDefinition;
import com.example.onlinejava.problem.ProblemType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TwoSumProblem implements ProblemDefinition {

    @Override
    // Returns the metadata and content for the Two Sum problem.
    public Problem getProblem() {
        return new Problem(
                "two-sum",
                "Two Sum",
                "Arrays",
                ProblemType.HASHING,
                Difficulty.EASY,
                "Given an array of integers and a target value, return the indices of two numbers whose sum equals the target.",
                "public static int[] twoSum(int[] numbers, int target)",
                "// Write your solution here",
                List.of(
                        "Return exactly two indices.",
                        "The two selected values must add up to the target.",
                        "Do not use the same element twice.",
                        "Assume exactly one valid solution exists."
                ),
                List.of(
                        "Input: [2, 7, 11, 15], target = 9 → Output: [0, 1]",
                        "Input: [3, 2, 4], target = 6 → Output: [1, 2]",
                        "Input: [3, 3], target = 6 → Output: [0, 1]"
                ),
                "Try using a hash map to remember values you have already seen."
        );
    }

    @Override
    // Builds the complete Java source used to test a Two Sum solution.
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