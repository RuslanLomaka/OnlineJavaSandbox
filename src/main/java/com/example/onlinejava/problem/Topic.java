package com.example.onlinejava.problem;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The topics problems are grouped under, split into two navbar sections.
 *
 * <p>Declaration order is the menu order. The slug is part of problem URLs
 * ({@code /problems/{slug}/...}), so changing a slug changes public URLs.
 */
public enum Topic {

  ARRAYS(Section.DATA_STRUCTURES, "arrays", "Arrays",
      "Traversal, in-place updates and prefix sums."),
  STRINGS(Section.DATA_STRUCTURES, "strings", "Strings",
      "Character manipulation, parsing and building strings."),
  HASHING(Section.DATA_STRUCTURES, "hash-maps-and-sets", "Hash Maps & Sets",
      "Constant-time lookups, counting and de-duplication."),
  STACKS_AND_QUEUES(Section.DATA_STRUCTURES, "stacks-and-queues", "Stacks & Queues",
      "Last-in-first-out and first-in-first-out processing."),
  LINKED_LISTS(Section.DATA_STRUCTURES, "linked-lists", "Linked Lists",
      "Rewiring chains of nodes."),
  TREES(Section.DATA_STRUCTURES, "trees", "Trees",
      "Binary trees, search trees and traversals."),
  GRAPHS(Section.DATA_STRUCTURES, "graphs", "Graphs",
      "Breadth-first and depth-first search, connectivity."),
  HEAPS(Section.DATA_STRUCTURES, "heaps", "Heaps",
      "Priority queues and top-k problems."),

  SORTING(Section.ALGORITHMS, "sorting", "Sorting",
      "Putting data in order with classic sorting algorithms."),
  SEARCHING(Section.ALGORITHMS, "searching", "Searching",
      "Binary search and other ways to find things fast."),
  TWO_POINTERS(Section.ALGORITHMS, "two-pointers", "Two Pointers",
      "Walking a sequence from both ends or at two speeds."),
  SLIDING_WINDOW(Section.ALGORITHMS, "sliding-window", "Sliding Window",
      "Tracking a moving range over a sequence."),
  RECURSION(Section.ALGORITHMS, "recursion-and-backtracking", "Recursion & Backtracking",
      "Solving problems by exploring choices."),
  DYNAMIC_PROGRAMMING(Section.ALGORITHMS, "dynamic-programming", "Dynamic Programming",
      "Building answers from overlapping subproblems."),
  GREEDY(Section.ALGORITHMS, "greedy", "Greedy",
      "Making the locally best choice at each step.");

  /**
   * The two top-level navbar menus.
   */
  public enum Section {
    DATA_STRUCTURES("Data Structures"),
    ALGORITHMS("Algorithms");

    private final String title;

    Section(final String title) {
      this.title = title;
    }

    /**
     * Returns the menu title.
     *
     * @return title, e.g. {@code Data Structures}
     */
    public String title() {
      return title;
    }
  }

  private final Section section;

  private final String slug;

  private final String title;

  private final String description;

  Topic(final Section section, final String slug, final String title, final String description) {
    this.section = section;
    this.slug = slug;
    this.title = title;
    this.description = description;
  }

  /**
   * Returns the section this topic belongs to.
   *
   * @return section
   */
  public Section section() {
    return section;
  }

  /**
   * Returns the URL slug.
   *
   * @return slug, e.g. {@code sliding-window}
   */
  public String slug() {
    return slug;
  }

  /**
   * Returns the display title.
   *
   * @return title, e.g. {@code Sliding Window}
   */
  public String title() {
    return title;
  }

  /**
   * Returns a one-line description.
   *
   * @return description
   */
  public String description() {
    return description;
  }

  /**
   * Returns the topic page URL.
   *
   * @return path, e.g. {@code /problems/sliding-window}
   */
  public String path() {
    return "/problems/" + slug;
  }

  /**
   * Finds a topic by its URL slug.
   *
   * @param slug URL slug
   * @return the topic, or empty if none matches
   */
  public static Optional<Topic> fromSlug(final String slug) {
    return Arrays.stream(values()).filter(topic -> topic.slug.equals(slug)).findFirst();
  }

  /**
   * Returns a section's topics in menu order.
   *
   * @param section section
   * @return topics of that section
   */
  public static List<Topic> inSection(final Section section) {
    return Arrays.stream(values()).filter(topic -> topic.section == section).toList();
  }
}
