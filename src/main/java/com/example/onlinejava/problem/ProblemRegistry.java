package com.example.onlinejava.problem;

import com.example.onlinejava.problem.arrays.BubbleSortProblem;
import org.springframework.stereotype.Component;  // tells Spring: "Create and manage an object of ProblemRegistry for me."

import java.util.HashMap;
import java.util.Map;

@Component
public class ProblemRegistry {

//    // This will store our problems like objects. for example - "bubble-sort" → Bubble Sort object
//    private final Map<String, Problem> problems = new HashMap<>();
//
//    // This method adds a problem.
//    public void register(Problem problem) {
//        problems.put(problem.getSlug(), problem);
//    }
//
//    // finds a problem using its slug.
//    public Problem getProblem(String slug) {
//        return problems.get(slug);
//    }

    private final Map<String, ProblemDefinition> problems = new HashMap<>();

    public ProblemRegistry() {
        register(new BubbleSortProblem());
    }

    public void register(ProblemDefinition problemDefinition) {
        Problem problem = problemDefinition.getProblem();
        problems.put(problem.getSlug(), problemDefinition);
    }

    public ProblemDefinition getProblem(String slug) {
        return problems.get(slug);
    }
}