package com.example.onlinejava.problem.datastructures;

import com.example.onlinejava.problem.Difficulty;
import com.example.onlinejava.problem.Problem;
import com.example.onlinejava.problem.ProblemDefinition;
import com.example.onlinejava.problem.Topic;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Defines the Reverse a Singly Linked List problem: metadata shown on the
 * problem page and the test harness used to grade a submitted solution.
 */
@Component
public class ReverseLinkedListProblem implements ProblemDefinition {

  /**
   * Returns the Reverse a Singly Linked List problem's metadata and content.
   *
   * @return the problem definition
   */
  @Override
  public Problem getProblem() {
    return new Problem.Builder("Reverse a Singly Linked List", Topic.LINKED_LISTS,
        Difficulty.EASY)
        .slug("reverse-linked-list")
        .description("Given the head of a singly linked list, reverse the list in place and "
            + "return the new head.")
        .methodSignature("public static Node reverseList(Node head)")
        .starterCode("// Write your solution here")
        .requirements(List.of(
            "Reverse the list by re-pointing existing nodes; do not build new nodes.",
            "Return the new head (the original tail).",
            "Handle an empty list (a null head).",
            "Handle a list with a single node."
        ))
        .examples(List.of(
            "Input: 1 -> 2 -> 3 -> 4 -> 5 → Output: 5 -> 4 -> 3 -> 2 -> 1",
            "Input: 1 -> 2 → Output: 2 -> 1",
            "Input: (empty list) → Output: (empty list)"
        ))
        .hint("Walk the list once, keeping track of the previous node. At each step, point the "
            + "current node's next at the previous node before moving on.")
        .build();
  }

  /**
   * Builds the complete Java source used to test a Reverse Linked List solution.
   *
   * @param solutionCode the user's submitted method body
   * @return the full {@code Main.java} source, including the test harness
   */
  @Override
  public String buildTestSource(String solutionCode) {
    return """
        import java.util.ArrayList;
        import java.util.Arrays;
        import java.util.List;

        public class Main {

            static class Node {
                int val;
                Node next;

                Node(int val) {
                    this.val = val;
                }
            }

            public static Node reverseList(Node head) {
        %s
            }

            private static int passedTests = 0;
            private static int totalTests = 0;

            public static void main(String[] args) {

                test("empty list", new int[]{});
                test("single node", new int[]{7});
                test("two nodes", new int[]{1, 2});
                test("multiple nodes", new int[]{1, 2, 3, 4, 5});
                test("duplicate values", new int[]{4, 4, 2, 2, 1});
                test("negative values", new int[]{-3, 5, -1, 0, 2});

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

            private static void test(String name, int[] values) {
                totalTests++;

                try {
                    Node head = buildList(values);
                    Node reversed = reverseList(head);

                    int[] expected = reverseArray(values);
                    int[] actual = toArray(reversed);

                    if (Arrays.equals(actual, expected)) {
                        passedTests++;
                        System.out.println("[PASS] " + name);
                    } else {
                        System.out.println("[FAIL] " + name);
                        System.out.println(
                                "       Expected: " + Arrays.toString(expected)
                        );
                        System.out.println(
                                "       Actual:   " + Arrays.toString(actual)
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

            private static Node buildList(int[] values) {
                Node dummy = new Node(0);
                Node tail = dummy;

                for (int value : values) {
                    tail.next = new Node(value);
                    tail = tail.next;
                }

                return dummy.next;
            }

            private static int[] toArray(Node head) {
                List<Integer> values = new ArrayList<>();

                Node current = head;
                while (current != null) {
                    values.add(current.val);
                    current = current.next;
                }

                int[] result = new int[values.size()];
                for (int i = 0; i < result.length; i++) {
                    result[i] = values.get(i);
                }

                return result;
            }

            private static int[] reverseArray(int[] values) {
                int[] result = new int[values.length];

                for (int i = 0; i < values.length; i++) {
                    result[i] = values[values.length - 1 - i];
                }

                return result;
            }
        }
        """.formatted(solutionCode);
  }
}
