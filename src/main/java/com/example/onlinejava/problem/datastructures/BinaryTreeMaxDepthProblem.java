package com.example.onlinejava.problem.datastructures;

import com.example.onlinejava.problem.Difficulty;
import com.example.onlinejava.problem.Problem;
import com.example.onlinejava.problem.ProblemDefinition;
import com.example.onlinejava.problem.Topic;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Defines the Maximum Depth of Binary Tree problem: metadata shown on the
 * problem page and the test harness used to grade a submitted solution.
 */
@Component
public class BinaryTreeMaxDepthProblem implements ProblemDefinition {

  /**
   * Returns the Maximum Depth of Binary Tree problem's metadata and content.
   *
   * @return the problem definition
   */
  @Override
  public Problem getProblem() {
    return new Problem.Builder("Maximum Depth of Binary Tree", Topic.TREES, Difficulty.EASY)
        .slug("binary-tree-max-depth")
        .description("Given the root of a binary tree, return its maximum depth: the number of "
            + "nodes along the longest path from the root down to the farthest leaf. The tree is "
            + "made of TreeNode objects, a plain class with three fields: int val (the node's "
            + "value), TreeNode left (the left child, or null), and TreeNode right (the right "
            + "child, or null). root is the topmost node, or null if the tree is empty. TreeNode "
            + "has no methods: you visit nodes by following left and right yourself.")
        .methodSignature("public static int maxDepth(TreeNode root)")
        .starterCode("""
            // TreeNode is already defined for you, like this:
            //
            //   static class TreeNode {
            //       int val;         // the value stored in this node
            //       TreeNode left;   // the left child, or null
            //       TreeNode right;  // the right child, or null
            //   }
            //
            // Visiting both children looks like:
            //
            //   int leftDepth = maxDepth(root.left);
            //   int rightDepth = maxDepth(root.right);

            // Write your solution here
            """)
        .requirements(List.of(
            "Handle an empty tree (a null root) by returning 0.",
            "Handle a tree with a single node by returning 1.",
            "Count nodes along the longest root-to-leaf path, not edges.",
            "Do not modify the tree."
        ))
        .examples(List.of(
            "Input: [3, 9, 20, null, null, 15, 7] → Output: 3",
            "Input: [1, null, 2] → Output: 2",
            "Input: [] → Output: 0"
        ))
        .hint("Recursively find the depth of the left and right subtrees. The depth of the "
            + "current node is 1 plus whichever of the two is larger. A null node has depth 0.")
        .build();
  }

  /**
   * Builds the complete Java source used to test a Maximum Depth of Binary Tree solution.
   *
   * @param solutionCode the user's submitted method body
   * @return the full {@code Main.java} source, including the test harness
   */
  @Override
  public String buildTestSource(String solutionCode) {
    return """
        import java.util.ArrayList;
        import java.util.List;

        public class Main {

            static class TreeNode {
                int val;
                TreeNode left;
                TreeNode right;

                TreeNode(int val) {
                    this.val = val;
                }
            }

            public static int maxDepth(TreeNode root) {
        %s
            }

            private static int passedTests = 0;
            private static int totalTests = 0;

            public static void main(String[] args) {
                test("empty tree", null, 0);
                test("single node", leaf(1), 1);
                test(
                        "left-skewed chain",
                        node(1, node(2, node(3, leaf(4), null), null), null),
                        4
                );
                test(
                        "right-skewed chain",
                        node(1, null, node(2, null, node(3, null, leaf(4)))),
                        4
                );
                test(
                        "balanced tree",
                        node(1, node(2, leaf(4), leaf(5)), node(3, leaf(6), leaf(7))),
                        3
                );
                test(
                        "left subtree deeper",
                        node(1, node(2, leaf(4), null), leaf(3)),
                        3
                );
                test("only right child at root", node(1, null, leaf(2)), 2);
                test(
                        "right subtree deeper",
                        node(1, leaf(2), node(3, null, leaf(4))),
                        3
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

            private static void test(String name, TreeNode root, int expected) {
                totalTests++;

                try {
                    List<TreeNode> nodes = new ArrayList<>();
                    collectNodes(root, nodes);
                    int[] values = new int[nodes.size()];
                    TreeNode[] lefts = new TreeNode[nodes.size()];
                    TreeNode[] rights = new TreeNode[nodes.size()];
                    for (int i = 0; i < nodes.size(); i++) {
                        values[i] = nodes.get(i).val;
                        lefts[i] = nodes.get(i).left;
                        rights[i] = nodes.get(i).right;
                    }

                    int actual = maxDepth(root);

                    boolean unchanged = true;
                    for (int i = 0; i < nodes.size(); i++) {
                        TreeNode treeNode = nodes.get(i);
                        if (treeNode.val != values[i]
                                || treeNode.left != lefts[i]
                                || treeNode.right != rights[i]) {
                            unchanged = false;
                            break;
                        }
                    }

                    if (actual != expected) {
                        System.out.println("[FAIL] " + name);
                        System.out.println("       Expected: " + expected);
                        System.out.println("       Actual:   " + actual);
                    } else if (!unchanged) {
                        System.out.println("[FAIL] " + name);
                        System.out.println(
                                "       Depth is right, but the tree was modified;"
                                        + " maxDepth must only read it"
                        );
                    } else {
                        passedTests++;
                        System.out.println("[PASS] " + name);
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

            private static void collectNodes(TreeNode treeNode, List<TreeNode> nodes) {
                if (treeNode == null) {
                    return;
                }
                nodes.add(treeNode);
                collectNodes(treeNode.left, nodes);
                collectNodes(treeNode.right, nodes);
            }

            private static TreeNode leaf(int val) {
                return new TreeNode(val);
            }

            private static TreeNode node(int val, TreeNode left, TreeNode right) {
                TreeNode treeNode = new TreeNode(val);
                treeNode.left = left;
                treeNode.right = right;
                return treeNode;
            }
        }
        """.formatted(solutionCode);
  }
}
