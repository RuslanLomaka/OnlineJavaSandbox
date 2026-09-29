package com.example.onlinejava.problem.datastructures;

import com.example.onlinejava.problem.Difficulty;
import com.example.onlinejava.problem.Problem;
import com.example.onlinejava.problem.ProblemDefinition;
import com.example.onlinejava.problem.Topic;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Defines the Valid Parentheses problem: metadata shown on the problem page
 * and the test harness used to grade a submitted solution.
 */
@Component
public class ValidParenthesesProblem implements ProblemDefinition {

  /**
   * Returns the Valid Parentheses problem's metadata and content.
   *
   * @return the problem definition
   */
  @Override
  public Problem getProblem() {
    return new Problem.Builder("Valid Parentheses", Topic.STACKS_AND_QUEUES, Difficulty.EASY)
        .slug("valid-parentheses")
        .description("Given a string containing just the characters '(', ')', '{', '}', '[' "
            + "and ']', determine if the input string is valid.")
        .methodSignature("public static boolean isValid(String s)")
        .starterCode("// Write your solution here")
        .requirements(List.of(
            "Open brackets must be closed by the same type of bracket.",
            "Open brackets must be closed in the correct order.",
            "Every closing bracket must have a matching, unclosed opening bracket.",
            "An empty string is valid."
        ))
        .examples(List.of(
            "Input: \"()\" → Output: true",
            "Input: \"()[]{}\" → Output: true",
            "Input: \"(]\" → Output: false",
            "Input: \"([)]\" → Output: false"
        ))
        .hint("Push each opening bracket onto a stack. When you see a closing bracket, it must "
            + "match the bracket on top of the stack.")
        .build();
  }

  /**
   * Builds the complete Java source used to test a Valid Parentheses solution.
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

            public static boolean isValid(String s) {
        %s
            }

            private static int passedTests = 0;
            private static int totalTests = 0;

            public static void main(String[] args) {

                test(
                        "single pair",
                        "()",
                        true
                );

                test(
                        "multiple types",
                        "()[]{}",
                        true
                );

                test(
                        "wrong type match",
                        "(]",
                        false
                );

                test(
                        "wrong order",
                        "([)]",
                        false
                );

                test(
                        "nested",
                        "{[]}",
                        true
                );

                test(
                        "deeply nested mixed",
                        "{[()()]}",
                        true
                );

                test(
                        "empty string",
                        "",
                        true
                );

                test(
                        "unclosed open",
                        "(",
                        false
                );

                test(
                        "unmatched close",
                        ")",
                        false
                );

                test(
                        "only opens",
                        "(((",
                        false
                );

                test(
                        "only closes",
                        "]]]",
                        false
                );

                test(
                        "close before any open",
                        "]",
                        false
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
                    String input,
                    boolean expected
            ) {
                totalTests++;

                try {
                    boolean actual = isValid(input);

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
