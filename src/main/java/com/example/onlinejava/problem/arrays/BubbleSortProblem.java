package com.example.onlinejava.problem.arrays;

import com.example.onlinejava.problem.Difficulty;
import com.example.onlinejava.problem.Problem;
import com.example.onlinejava.problem.ProblemDefinition;
import com.example.onlinejava.problem.ProblemType;

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
                """
                public static int[] bubbleSort(int[] nums) {
                    
                    // Write your solution here
                    
                    return nums;
                }
                """
        );
    }

    @Override
    public String buildTestSource(String solutionCode) {
        return solutionCode;
    }
}