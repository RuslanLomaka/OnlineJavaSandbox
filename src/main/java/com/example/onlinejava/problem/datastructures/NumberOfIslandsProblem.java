package com.example.onlinejava.problem.datastructures;

import com.example.onlinejava.problem.Difficulty;
import com.example.onlinejava.problem.Problem;
import com.example.onlinejava.problem.ProblemDefinition;
import com.example.onlinejava.problem.Topic;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Defines the Number of Islands problem: metadata shown on the problem
 * page and the test harness used to grade a submitted solution.
 */
@Component
public class NumberOfIslandsProblem implements ProblemDefinition {

  /**
   * Returns the Number of Islands problem's metadata and content.
   *
   * @return the problem definition
   */
  @Override
  public Problem getProblem() {
    return new Problem.Builder("Number of Islands", Topic.GRAPHS, Difficulty.MEDIUM)
        .slug("number-of-islands")
        .description("Given an m x n grid of '1's (land) and '0's (water), return the number of "
            + "islands. An island is a group of '1's connected horizontally or vertically. "
            + "Assume all four edges of the grid are surrounded by water.")
        .methodSignature("public static int numIslands(char[][] grid)")
        .starterCode("// Write your solution here")
        .requirements(List.of(
            "Count islands, where an island is a maximal group of '1' cells connected "
                + "horizontally or vertically (not diagonally).",
            "The grid has at least one row and one column.",
            "You may mutate the input grid; only the returned count is checked."
        ))
        .examples(List.of(
            "Input: [[\"1\",\"1\",\"0\"],[\"1\",\"1\",\"0\"],[\"0\",\"0\",\"1\"]] → Output: 2",
            "Input: [[\"1\",\"0\"],[\"0\",\"1\"]] → Output: 2 (diagonal cells don't connect)",
            "Input: [[\"0\",\"0\"],[\"0\",\"0\"]] → Output: 0"
        ))
        .hint("Scan every cell; when you find land, explore outward (depth-first or "
            + "breadth-first) marking every connected land cell as visited so the same island "
            + "isn't counted twice.")
        .build();
  }

  /**
   * Builds the complete Java source used to test a Number of Islands solution.
   *
   * @param solutionCode the user's submitted method body
   * @return the full {@code Main.java} source, including the test harness
   */
  @Override
  public String buildTestSource(String solutionCode) {
    return """
        import java.util.ArrayDeque;
        import java.util.Deque;

        public class Main {

            public static int numIslands(char[][] grid) {
        %s
            }

            private static int passedTests = 0;
            private static int totalTests = 0;

            public static void main(String[] args) {

                test(
                        "single land cell",
                        new char[][]{{'1'}},
                        1
                );

                test(
                        "single water cell",
                        new char[][]{{'0'}},
                        0
                );

                test(
                        "all water",
                        new char[][]{{'0', '0'}, {'0', '0'}},
                        0
                );

                test(
                        "all land",
                        new char[][]{{'1', '1'}, {'1', '1'}},
                        1
                );

                test(
                        "diagonal cells are not connected",
                        new char[][]{{'1', '0'}, {'0', '1'}},
                        2
                );

                test(
                        "single L-shaped island",
                        new char[][]{
                                {'1', '1', '1', '1', '0'},
                                {'1', '1', '0', '1', '0'},
                                {'1', '1', '0', '0', '0'},
                                {'0', '0', '0', '0', '0'}
                        },
                        1
                );

                test(
                        "three separate islands",
                        new char[][]{
                                {'1', '1', '0', '0', '0'},
                                {'1', '1', '0', '0', '0'},
                                {'0', '0', '1', '0', '0'},
                                {'0', '0', '0', '1', '1'}
                        },
                        3
                );

                test(
                        "single row alternating",
                        new char[][]{{'1', '0', '1', '0', '1'}},
                        3
                );

                test(
                        "single column alternating",
                        new char[][]{{'1'}, {'0'}, {'1'}, {'1'}},
                        2
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
                    char[][] grid,
                    int expected
            ) {
                totalTests++;

                try {
                    int actual = numIslands(grid);

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
