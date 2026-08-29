
package com.example.onlinejava.problem;

import java.util.List;

/**
 * Represents a coding problem and its associated metadata.
 */
public class Problem {

  /**
   * Unique identifier used in problem URLs.
   */
  private final String slug;

  /**
   * Display title of the problem.
   */
  private final String title;

  /**
   * Main category of the problem.
   */
  private final String category;

  /**
   * Problem-solving technique associated with the problem.
   */
  private final ProblemType type;

  /**
   * Difficulty level of the problem.
   */
  private final Difficulty difficulty;

  /**
   * Description of the problem.
   */
  private final String description;

  /**
   * Required method signature for the solution.
   */
  private final String methodSignature;

  /**
   * Starter code displayed in the editor.
   */
  private final String starterCode;

  /**
   * Requirements that the submitted solution must satisfy.
   */
  private final List<String> requirements;

  /**
   * Examples displayed on the problem page.
   */
  private final List<String> examples;

  /**
   * Hint provided to the user.
   */
  private final String hint;

  /**
   * Creates a problem definition.
   *
   * @param problemSlug unique problem identifier
   * @param problemTitle display title
   * @param problemCategory problem category
   * @param problemType problem-solving technique
   * @param problemDifficulty difficulty level
   * @param problemDescription problem description
   * @param problemMethodSignature required method signature
   * @param problemStarterCode starter code for the editor
   * @param problemRequirements solution requirements
   * @param problemExamples examples shown to the user
   * @param problemHint hint provided to the user
   */

  public Problem(
      final String problemSlug,
      final String problemTitle,
      final String problemCategory,
      final ProblemType problemType,
      final Difficulty problemDifficulty,
      final String problemDescription,
      final String problemMethodSignature,
      final String problemStarterCode,
      final List<String> problemRequirements,
      final List<String> problemExamples,
      final String problemHint
  ) {
    this.slug = problemSlug;
    this.title = problemTitle;
    this.category = problemCategory;
    this.type = problemType;
    this.difficulty = problemDifficulty;
    this.description = problemDescription;
    this.methodSignature = problemMethodSignature;
    this.starterCode = problemStarterCode;
    this.requirements = problemRequirements;
    this.examples = problemExamples;
    this.hint = problemHint;
  }

  /**
   * Returns the problem slug.
   *
   * @return problem slug
   */
  public String getSlug() {
    return slug;
  }

  /**
   * Returns the problem title.
   *
   * @return problem title
   */
  public String getTitle() {
    return title;
  }

  /**
   * Returns the problem category.
   *
   * @return problem category
   */
  public String getCategory() {
    return category;
  }

  /**
   * Returns the problem type.
   *
   * @return problem type
   */
  public ProblemType getType() {
    return type;
  }

  /**
   * Returns the difficulty level.
   *
   * @return difficulty level
   */
  public Difficulty getDifficulty() {
    return difficulty;
  }

  /**
   * Returns the problem description.
   *
   * @return problem description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Returns the required method signature.
   *
   * @return method signature
   */
  public String getMethodSignature() {
    return methodSignature;
  }

  /**
   * Returns the starter code.
   *
   * @return starter code
   */
  public String getStarterCode() {
    return starterCode;
  }

  /**
   * Returns the requirements for the problem.
   *
   * @return problem requirements
   */
  public List<String> getRequirements() {
    return requirements;
  }

  /**
   * Returns the examples for the problem.
   *
   * @return problem examples
   */
  public List<String> getExamples() {
    return examples;
  }

  /**
   * Returns the hint for the problem.
   *
   * @return problem hint
   */
  public String getHint() {
    return hint;
  }
}
