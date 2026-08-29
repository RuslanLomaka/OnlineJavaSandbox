package com.example.onlinejava.problem;

public interface ProblemDefinition {

    // Gives information about the problem
    Problem getProblem();

    String buildTestSource(String solutionCode);
}