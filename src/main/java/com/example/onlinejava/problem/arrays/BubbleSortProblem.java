package com.example.onlinejava.problem.arrays;

import com.example.onlinejava.problem.Difficulty;
import com.example.onlinejava.problem.Problem;
import com.example.onlinejava.problem.ProblemDefinition;
import com.example.onlinejava.problem.ProblemType;

import java.util.List;

public class BubbleSortProblem implements ProblemDefinition {

    @Override
    public Problem getProblem() {
        return new Problem(
                "bubble-sort",
                "Bubble Sort",
                "Arrays",
                ProblemType.ARRAY,
                Difficulty.EASY,
                "Sort an array using the Bubble Sort algorithm.",
                "public static void bubbleSort(int[] numbers)",
                "// Write your solution here",
                List.of(
                        "Modify the supplied array.",
                        "Sort values from smallest to largest.",
                        "Handle negative numbers and duplicates.",
                        "Do not use Arrays.sort().",
                        "Do not return a new array."
                ),
                List.of(
                        "Input: [5, 3, 8, 1] → Output: [1, 3, 5, 8]",
                        "Input: [4, 4, 2] → Output: [2, 4, 4]",
                        "Input: [-2, 5, 0, -1] → Output: [-2, -1, 0, 5]"
                ),
                "Compare neighbouring values. When the left value is larger than the right value, exchange them."


        );
    }

    @Override
    public String buildTestSource(String solutionCode) {
        return solutionCode;
    }
}