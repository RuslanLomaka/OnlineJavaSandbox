package com.example.onlinejava.problem;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Supplies the navbar's "Data Structures" and "Algorithms" menus to every page
 * as the {@code navSections} model attribute.
 *
 * <p>Problems are registered at startup and never change, so the menu model is
 * built once and reused.
 */
@ControllerAdvice(annotations = Controller.class)
public class NavigationModelAdvice {

  /**
   * One navbar menu.
   *
   * @param title menu title
   * @param topics topics in menu order
   */
  public record NavSection(String title, List<NavTopic> topics) {
  }

  /**
   * One menu entry.
   *
   * @param title topic title
   * @param path topic page URL
   * @param problemCount number of problems in the topic (0 shows as "soon")
   */
  public record NavTopic(String title, String path, int problemCount) {
  }

  private final List<NavSection> sections;

  /**
   * Builds the menu model.
   *
   * @param registry problem registry; absent in web-layer test slices that
   *     don't load it, in which case the menus are simply empty
   */
  public NavigationModelAdvice(final ObjectProvider<ProblemRegistry> registry) {
    final ProblemRegistry available = registry.getIfAvailable();
    this.sections = available == null ? List.of() : build(available.problemsByTopic());
  }

  /**
   * Returns the navbar menus.
   *
   * @return sections in display order
   */
  @ModelAttribute("navSections")
  public List<NavSection> navSections() {
    return sections;
  }

  private static List<NavSection> build(final Map<Topic, List<Problem>> byTopic) {
    return Arrays.stream(Topic.Section.values())
        .map(section -> new NavSection(section.title(), Topic.inSection(section).stream()
            .map(topic -> new NavTopic(topic.title(), topic.path(), byTopic.get(topic).size()))
            .toList()))
        .toList();
  }
}
