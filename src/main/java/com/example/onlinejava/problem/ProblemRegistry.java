package com.example.onlinejava.problem;

import com.example.onlinejava.problem.arrays.BubbleSortProblem;
import com.example.onlinejava.problem.arrays.TwoSumProblem;
import com.example.onlinejava.problem.arrays.BinarySearchProblem;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProblemRegistry {

    private final Map<String, ProblemDefinition> problems = new HashMap<>();

    public ProblemRegistry() {
        register(new BubbleSortProblem());
        register(new TwoSumProblem());
        register(new BinarySearchProblem());
    }

    // Adds a problem definition to the registry.
    public void register(ProblemDefinition problemDefinition) {
        Problem problem = problemDefinition.getProblem();
        problems.put(problem.getSlug(), problemDefinition);
    }

    // Finds a problem using its slug.
    public ProblemDefinition getProblem(String slug) {
        return problems.get(slug);
    }

    // Returns all registered problem definitions.
    public List<ProblemDefinition> getAllProblems() {
        return List.copyOf(problems.values());
    }
}