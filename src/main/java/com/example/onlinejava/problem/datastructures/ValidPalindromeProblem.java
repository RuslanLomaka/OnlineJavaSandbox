package com.example.onlinejava.problem.datastructures;

import com.example.onlinejava.problem.Difficulty;
import com.example.onlinejava.problem.Problem;
import com.example.onlinejava.problem.ProblemDefinition;
import com.example.onlinejava.problem.Topic;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Defines the Valid Palindrome problem: metadata shown on the problem page
 * and the test harness used to grade a submitted solution.
 */
@Component
public class ValidPalindromeProblem implements ProblemDefinition {

  /**
   * Returns the Valid Palindrome problem's metadata and content.
   *
   * @return the problem definition
   */
  @Override
  public Problem getProblem() {
    return new Problem.Builder("Valid Palindrome", Topic.STRINGS, Difficulty.EASY)
        .slug("valid-palindrome")
        .description("Given a string, determine whether it is a palindrome, considering only "
            + "alphanumeric characters and ignoring case.")
        .methodSignature("public static boolean isPalindrome(String s)")
        .starterCode("// Write your solution here")
        .requirements(List.of(
            "Ignore characters that are not letters or digits.",
            "Treat uppercase and lowercase letters as equal.",
            "An empty string or a string with no alphanumeric characters is a palindrome."
        ))
        .examples(List.of(
            "Input: \"A man, a plan, a canal: Panama\" → Output: true",
            "Input: \"race a car\" → Output: false",
            "Input: \" \" → Output: true",
            "Input: \"0P\" → Output: false"
        ))
        .hint("Walk two pointers inward from both ends, skipping non-alphanumeric characters "
            + "and comparing letters case-insensitively.")
        .build();
  }

  /**
   * Builds the complete Java source used to test a Valid Palindrome solution.
   *
   * @param solutionCode the user's submitted method body
   * @return the full {@code Main.java} source, including the test harness
   */
  @Override
  public String buildTestSource(String solutionCode) {
    return """
        public class Main {

            public static boolean isPalindrome(String s) {
        %s
            }

            private static int passedTests = 0;
            private static int totalTests = 0;

            public static void main(String[] args) {

                test(
                        "mixed case with punctuation",
                        "A man, a plan, a canal: Panama",
                        true
                );

                test(
                        "not a palindrome",
                        "race a car",
                        false
                );

                test(
                        "empty string",
                        "",
                        true
                );

                test(
                        "only non-alphanumeric characters",
                        ".,:;!",
                        true
                );

                test(
                        "single space",
                        " ",
                        true
                );

                test(
                        "letter versus digit",
                        "0P",
                        false
                );

                test(
                        "underscore is ignored",
                        "ab_a",
                        true
                );

                test(
                        "digits only, palindrome",
                        "12321",
                        true
                );

                test(
                        "digits only, not a palindrome",
                        "12345",
                        false
                );

                test(
                        "single character",
                        "a",
                        true
                );

                test(
                        "distinct supplementary-plane letters are not equal",
                        "𐐀a𐐁",
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
                    boolean actual = isPalindrome(input);

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
