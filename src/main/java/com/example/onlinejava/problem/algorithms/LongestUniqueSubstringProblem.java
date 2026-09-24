package com.example.onlinejava.problem.algorithms;

import com.example.onlinejava.problem.Difficulty;
import com.example.onlinejava.problem.Problem;
import com.example.onlinejava.problem.ProblemDefinition;
import com.example.onlinejava.problem.Topic;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Defines the Longest Substring Without Repeating Characters problem.
 *
 * <p>Migrated from a hand-written page whose hidden tests were assembled in
 * the browser; the same tests now live only in {@link #buildTestSource}.
 */
@Component
public class LongestUniqueSubstringProblem implements ProblemDefinition {

  /**
   * Harness code placed before the user's method body. Plain concatenation
   * (not {@code String.formatted}) because the harness itself uses
   * {@code printf} format specifiers.
   */
  private static final String HARNESS_START = """
      import java.util.HashMap;
      import java.util.Map;

      public class Main {

          public static int longestUniqueLength(String text) {
      """;

  /**
   * Harness code placed after the user's method body: the hidden tests.
   */
  private static final String HARNESS_END = """
          }

          private static int passedTests = 0;
          private static int totalTests = 0;

          public static void main(String[] args) {
              test("Typical repeated sequence", "abcabcbb", 3);
              test("Every character is the same", "bbbbb", 1);
              test("Window moves past earlier repeat", "pwwkew", 3);
              test("Empty string", "", 0);
              test("Null input", null, 0);
              test("One character", "x", 1);
              test("All characters are unique", "abcdef", 6);
              test("Repeat after unique prefix", "abcddef", 4);
              test("Repeat inside current window", "abba", 2);
              test("Spaces count as characters", "a b c a", 3);
              test("Uppercase and lowercase differ", "aAbBcA", 5);
              test("Digits and punctuation", "1!2@1#", 5);
              test("Longest window occurs at end", "aabcde", 5);
              test("Window must not move backward", "tmmzuxt", 5);
              test("Non-ASCII Java chars", "\\u4f60\\u597d\\u5417\\u4f60\\u5f88", 4);
              test("Do not discard reusable characters", "abaca", 3);
              test("Repeated char before current window", "dvdf", 3);
              test("Unique suffix after repetitions", "aaaaabcdef", 6);
              test("Whitespace variations", "a\\tb a", 4);
              performanceTest();

              System.out.println();
              System.out.println("Passed " + passedTests + " of " + totalTests + " tests.");

              if (passedTests == totalTests) {
                  System.out.println("All tests passed!");
              } else {
                  System.exit(1);
              }
          }

          private static void test(String name, String input, int expected) {
              totalTests++;
              try {
                  int actual = longestUniqueLength(input);
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
                  System.out.println("        " + exception.getClass().getSimpleName()
                          + ": " + exception.getMessage());
              }
          }

          /** 30,000 distinct chars must be handled in well under two seconds (O(n)). */
          private static void performanceTest() {
              totalTests++;
              String name = "Large-input performance check";
              int length = 30_000;
              StringBuilder input = new StringBuilder(length);
              for (int c = 1_000; c < 1_000 + length; c++) {
                  input.append((char) c);
              }
              try {
                  long started = System.nanoTime();
                  int actual = longestUniqueLength(input.toString());
                  long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
                  if (actual == length && elapsedMillis < 2_000) {
                      passedTests++;
                      System.out.println("[PASS] " + name + " (" + elapsedMillis + " ms)");
                  } else {
                      System.out.println("[FAIL] " + name);
                      System.out.println("       Expected: " + length + " in under 2000 ms");
                      System.out.println("       Actual:   " + actual
                              + " in " + elapsedMillis + " ms");
                  }
              } catch (Exception exception) {
                  System.out.println("[ERROR] " + name);
                  System.out.println("        " + exception.getClass().getSimpleName()
                          + ": " + exception.getMessage());
              }
          }
      }
      """;

  /**
   * Returns the problem's metadata and content.
   *
   * @return the problem definition
   */
  @Override
  public Problem getProblem() {
    return new Problem.Builder(
        "Longest Substring Without Repeating Characters", Topic.SLIDING_WINDOW, Difficulty.MEDIUM)
        .slug("longest-unique-substring")
        .description("Find the length of the longest contiguous part of the input string in "
            + "which no character occurs more than once. A substring is a continuous part of "
            + "the string: \"bcd\" is a substring of \"abcde\", but \"ace\" is not.")
        .methodSignature("public static int longestUniqueLength(String text)")
        .starterCode("// Write your solution here")
        .requirements(List.of(
            "Return 0 for null and for an empty string.",
            "Spaces, punctuation and digits count as characters.",
            "Uppercase and lowercase letters are different.",
            "Do not generate every possible substring.",
            "Aim for O(n) time; a HashMap<Character, Integer> helps."
        ))
        .examples(List.of(
            "Input: \"abcabcbb\" → Output: 3 (\"abc\")",
            "Input: \"bbbbb\" → Output: 1",
            "Input: \"pwwkew\" → Output: 3 (\"wke\")",
            "Input: \"abba\" → Output: 2 (the window's left edge never moves backwards)"
        ))
        .hint("Keep a window [left, right]. Remember the last index of each character; when "
            + "the character at right was already seen inside the window, move left just "
            + "past that earlier position.")
        .build();
  }

  /**
   * Builds the complete Java source used to test a solution.
   *
   * @param solutionCode the user's submitted method body
   * @return the full {@code Main.java} source, including the hidden tests
   */
  @Override
  public String buildTestSource(String solutionCode) {
    return HARNESS_START + solutionCode + "\n" + HARNESS_END;
  }
}
