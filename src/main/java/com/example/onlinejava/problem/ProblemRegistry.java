package com.example.onlinejava.problem;

import com.example.onlinejava.problem.arrays.BinarySearchProblem;
import com.example.onlinejava.problem.arrays.BubbleSortProblem;
import com.example.onlinejava.problem.arrays.TwoSumProblem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Holds every known {@link ProblemDefinition}, indexed by slug, and
 * registers the built-in problems at startup.
 */
@Component
public class ProblemRegistry {

  private final Map<String, ProblemDefinition> problems = new HashMap<>();

  /**
   * Creates the registry and registers every built-in problem.
   */
  public ProblemRegistry() {
    register(new BubbleSortProblem());
    register(new TwoSumProblem());
    register(new BinarySearchProblem());
  }

  /**
   * Adds a problem definition to the registry.
   *
   * @param problemDefinition the problem definition to register
   */
  public void register(ProblemDefinition problemDefinition) {
    Problem problem = problemDefinition.getProblem();
    problems.put(problem.getSlug(), problemDefinition);
  }

  /**
   * Finds a problem using its slug.
   *
   * @param slug the problem's unique identifier
   * @return the matching problem definition, or {@code null} if none is
   *     registered under that slug
   */
  public ProblemDefinition getProblem(String slug) {
    return problems.get(slug);
  }

  /**
   * Returns all registered problem definitions.
   *
   * @return an immutable snapshot of every registered problem definition
   */
  public List<ProblemDefinition> getAllProblems() {
    return List.copyOf(problems.values());
  }
}