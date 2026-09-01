
package com.example.onlinejava.problem;

import java.util.ArrayList;
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


  private Problem(Builder builder) {
    this.title = builder.problemTitle;
    this.category = builder.problemCategory;
    this.description = builder.problemDescription;
    this.difficulty = builder.problemDifficulty;
    this.examples = builder.problemExamples;
    this.hint = builder.problemHint;
    this.methodSignature = builder.problemMethodSignature;
    this.requirements = builder.problemRequirements;
    this.slug = builder.problemSlug;
    this.starterCode = builder.problemStarterCode;
    this.type = builder.problemType;

  }

  /**
   * Builds a {@link Problem} instance, requiring only title, type, and
   * difficulty up front and defaulting every other field to an empty
   * value until overridden.
   */
  public static class Builder {
    private String problemTitle;
    private ProblemType problemType;
    private Difficulty problemDifficulty;
    //Optional
    private String problemMethodSignature = "";
    private String problemCategory = "";
    private String problemSlug = "";
    private String problemDescription = "";
    private String problemStarterCode = "";
    private List<String> problemRequirements = new ArrayList<>();
    private List<String> problemExamples = new ArrayList<>();
    private String problemHint = "";

    /**
     * Creates a builder with the required problem fields.
     *
     * @param problemTitle display title
     * @param problemType problem-solving technique
     * @param problemDifficulty difficulty level
     */
    public Builder(String problemTitle, ProblemType problemType, Difficulty problemDifficulty) {
      this.problemTitle = problemTitle;
      this.problemType = problemType;
      this.problemDifficulty = problemDifficulty;
    }

    /**
     * Sets the unique identifier used in problem URLs.
     *
     * @param problemSlug problem slug
     * @return this builder
     */
    public Builder slug(String problemSlug) {
      this.problemSlug = problemSlug;
      return this;
    }

    /**
     * Sets the problem's main category.
     *
     * @param problemCategory problem category
     * @return this builder
     */
    public Builder category(String problemCategory) {
      this.problemCategory = problemCategory;
      return this;
    }

    /**
     * Sets the problem description.
     *
     * @param problemDescription problem description
     * @return this builder
     */
    public Builder description(String problemDescription) {
      this.problemDescription = problemDescription;
      return this;
    }

    /**
     * Sets the required method signature for the solution.
     *
     * @param problemMethodSignature required method signature
     * @return this builder
     */
    public Builder methodSignature(String problemMethodSignature) {
      this.problemMethodSignature = problemMethodSignature;
      return this;
    }

    /**
     * Sets the starter code displayed in the editor.
     *
     * @param problemStarterCode starter code
     * @return this builder
     */
    public Builder starterCode(String problemStarterCode) {
      this.problemStarterCode = problemStarterCode;
      return this;
    }

    /**
     * Sets the requirements the submitted solution must satisfy.
     *
     * @param problemRequirements solution requirements
     * @return this builder
     */
    public Builder requirements(List<String> problemRequirements) {
      this.problemRequirements = problemRequirements;
      return this;
    }

    /**
     * Sets the examples displayed on the problem page.
     *
     * @param problemExamples problem examples
     * @return this builder
     */
    public Builder examples(List<String> problemExamples) {
      this.problemExamples = problemExamples;
      return this;
    }

    /**
     * Sets the hint provided to the user.
     *
     * @param problemHint problem hint
     * @return this builder
     */
    public Builder hint(String problemHint) {
      this.problemHint = problemHint;
      return this;
    }

    /**
     * Builds the {@link Problem} from this builder's current state.
     *
     * @return the constructed problem
     */
    public Problem build() {
      return new Problem(this);
    }
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
