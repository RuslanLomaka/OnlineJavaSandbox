package com.example.onlinejava;

import com.example.onlinejava.problem.Problem;
import com.example.onlinejava.problem.ProblemRegistry;
import com.example.onlinejava.problem.Topic;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

/**
 * Handles page requests for the Online Java application.
 *
 * <p>Problem URLs are {@code /problems/{topic}/{slug}}. Older URLs used a
 * category instead of the topic ({@code /problems/arrays/bubble-sort}); those
 * are answered with a 301 redirect to the canonical URL.
 */
@Controller
public class PageController {

  /**
   * Category listing pages that existed before topics; they now redirect to
   * the overview. ({@code arrays} is still valid: it's a topic.)
   */
  private static final Set<String> RETIRED_SECTION_PAGES = Set.of("collections", "algorithms");

  /**
   * Registry containing all available coding problems.
   */
  private final ProblemRegistry problemRegistry;

  /**
   * Creates a page controller.
   *
   * @param registry registry containing available problems
   */
  public PageController(final ProblemRegistry registry) {
    this.problemRegistry = registry;
  }

  /**
   * One section of the overview page.
   *
   * @param title section title
   * @param topics the section's topics with their problems
   */
  public record SectionView(String title, List<TopicView> topics) {
  }

  /**
   * A topic with its problems, for listing pages.
   *
   * @param topic the topic
   * @param problems problems in display order (may be empty)
   */
  public record TopicView(Topic topic, List<Problem> problems) {
  }

  /**
   * Displays the sandbox page. The navbar's {@code username} comes from
   * {@link com.example.onlinejava.user.CurrentUserModelAdvice}.
   *
   * @return the sandbox page name
   */
  @GetMapping("/sandbox")
  public String sandbox() {
    return "sandbox";
  }

  /**
   * Displays every topic of both sections with its problems.
   *
   * @return the overview page
   */
  @GetMapping("/problems")
  public ModelAndView problems() {
    final Map<Topic, List<Problem>> byTopic = problemRegistry.problemsByTopic();
    final List<SectionView> sections = Arrays.stream(Topic.Section.values())
        .map(section -> new SectionView(section.title(), Topic.inSection(section).stream()
            .map(topic -> new TopicView(topic, byTopic.get(topic)))
            .toList()))
        .toList();
    return new ModelAndView("problems", Map.of("sections", sections));
  }

  /**
   * Displays one topic's problems.
   *
   * @param topicSlug topic slug from the URL
   * @return the topic page, or a redirect for retired section pages
   */
  @GetMapping("/problems/{topicSlug}")
  public ModelAndView topic(@PathVariable final String topicSlug) {
    if (RETIRED_SECTION_PAGES.contains(topicSlug)) {
      return Redirects.permanent("/problems");
    }
    final Topic topic = Topic.fromSlug(topicSlug).orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
    return new ModelAndView("topic", Map.of(
        "topicView", new TopicView(topic, problemRegistry.problemsByTopic().get(topic)),
        "siblings", Topic.inSection(topic.section())));
  }

  /**
   * Displays a registered problem, redirecting old URLs to the canonical one.
   *
   * @param topicSlug topic slug from the URL (or an old category name)
   * @param slug problem slug
   * @return the generic problem page, or a 301 redirect
   */
  @GetMapping("/problems/{topicSlug}/{slug}")
  public ModelAndView problem(
      @PathVariable final String topicSlug,
      @PathVariable final String slug
  ) {
    final Problem problem = problemRegistry.findBySlug(slug)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Problem not found"));
    if (!problem.getTopic().slug().equals(topicSlug)) {
      return Redirects.permanent(problem.getPath());
    }
    return new ModelAndView("problem", Map.of("problem", problem));
  }
}
