
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
   * Topic the problem is listed under (and part of its URL).
   */
  private final Topic topic;

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
    this.description = builder.problemDescription;
    this.difficulty = builder.problemDifficulty;
    this.examples = builder.problemExamples;
    this.hint = builder.problemHint;
    this.methodSignature = builder.problemMethodSignature;
    this.requirements = builder.problemRequirements;
    this.slug = builder.problemSlug;
    this.starterCode = builder.problemStarterCode;
    this.topic = builder.problemTopic;

  }

  /**
   * Builds a {@link Problem} instance, requiring only title, topic, and
   * difficulty up front and defaulting every other field to an empty
   * value until overridden.
   */
  public static class Builder {
    private String problemTitle;
    private Topic problemTopic;
    private Difficulty problemDifficulty;
    //Optional
    private String problemMethodSignature = "";
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
     * @param problemTopic topic the problem is listed under
     * @param problemDifficulty difficulty level
     */
    public Builder(String problemTitle, Topic problemTopic, Difficulty problemDifficulty) {
      this.problemTitle = problemTitle;
      this.problemTopic = problemTopic;
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
   * Returns the topic the problem is listed under.
   *
   * @return topic
   */
  public Topic getTopic() {
    return topic;
  }

  /**
   * Returns the problem's canonical URL.
   *
   * @return path, e.g. {@code /problems/sorting/bubble-sort}
   */
  public String getPath() {
    return topic.path() + "/" + slug;
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
