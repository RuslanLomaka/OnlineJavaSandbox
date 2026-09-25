package com.example.onlinejava.problem;

import com.example.onlinejava.problem.algorithms.BinarySearchProblem;
import com.example.onlinejava.problem.algorithms.BubbleSortProblem;
import com.example.onlinejava.problem.algorithms.LongestUniqueSubstringProblem;
import com.example.onlinejava.problem.datastructures.MaximumSubarrayProblem;
import com.example.onlinejava.problem.datastructures.TwoSumProblem;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Holds every known {@link ProblemDefinition}, indexed by slug, and
 * registers the built-in problems at startup.
 */
@Component
public class ProblemRegistry {

  /**
   * Order problems are listed in within a topic: easiest first, then by title.
   */
  public static final Comparator<Problem> DISPLAY_ORDER = Comparator
      .comparing(Problem::getDifficulty)
      .thenComparing(Problem::getTitle, String.CASE_INSENSITIVE_ORDER);

  private final Map<String, ProblemDefinition> problems = new HashMap<>();

  /**
   * Creates the registry and registers every built-in problem.
   */
  public ProblemRegistry() {
    register(new BubbleSortProblem());
    register(new BinarySearchProblem());
    register(new LongestUniqueSubstringProblem());
    register(new TwoSumProblem());
    register(new MaximumSubarrayProblem());
  }

  /**
   * Adds a problem definition to the registry.
   *
   * @param problemDefinition the problem definition to register
   * @throws IllegalStateException if the slug is already registered
   */
  public void register(ProblemDefinition problemDefinition) {
    Problem problem = problemDefinition.getProblem();
    if (problems.putIfAbsent(problem.getSlug(), problemDefinition) != null) {
      throw new IllegalStateException("Duplicate problem slug: " + problem.getSlug());
    }
  }

  /**
   * Finds a problem definition using its slug.
   *
   * @param slug the problem's unique identifier
   * @return the matching problem definition, or {@code null} if none is
   *     registered under that slug
   */
  public ProblemDefinition getProblem(String slug) {
    return problems.get(slug);
  }

  /**
   * Finds a problem's metadata using its slug.
   *
   * @param slug the problem's unique identifier
   * @return the problem, or empty if none is registered under that slug
   */
  public Optional<Problem> findBySlug(String slug) {
    return Optional.ofNullable(problems.get(slug)).map(ProblemDefinition::getProblem);
  }

  /**
   * Returns every topic in menu order with its problems in
   * {@link #DISPLAY_ORDER}. Topics without problems map to an empty list.
   *
   * @return problems grouped by topic
   */
  public Map<Topic, List<Problem>> problemsByTopic() {
    final Map<Topic, List<Problem>> byTopic = new EnumMap<>(Topic.class);
    for (final Topic topic : Topic.values()) {
      byTopic.put(topic, problems.values().stream()
          .map(ProblemDefinition::getProblem)
          .filter(problem -> problem.getTopic() == topic)
          .sorted(DISPLAY_ORDER)
          .toList());
    }
    return byTopic;
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
