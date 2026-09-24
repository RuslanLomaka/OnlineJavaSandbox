package com.example.onlinejava;

import com.example.onlinejava.problem.Problem;
import com.example.onlinejava.problem.ProblemDefinition;
import com.example.onlinejava.problem.ProblemRegistry;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles page requests for the Online Java application.
 */
@Controller
public class PageController {

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
   * Displays the main problems page.
   *
   * @return the problems page name
   */
  @GetMapping("/problems")
  public String problems() {
    return "problems";
  }

  /**
   * Displays the array problems page.
   *
   * @param model model used to pass array problems to the view
   * @return the array problems page name
   */
  @GetMapping("/problems/arrays")
  public String arrays(final Model model) {
    final List<Problem> arrayProblems =
        problemRegistry.getAllProblems()
            .stream()
            .map(ProblemDefinition::getProblem)
            .filter(problem ->
                "Arrays".equalsIgnoreCase(
                    problem.getCategory()
                )
            )
            .toList();

    model.addAttribute("problems", arrayProblems);

    return "problems-arrays";
  }

  /**
   * Displays the collections problems page.
   *
   * @return the collections problems page name
   */
  @GetMapping("/problems/collections")
  public String collections() {
    return "problems-collections";
  }

  /**
   * Displays the algorithms problems page.
   *
   * @return the algorithms problems page name
   */
  @GetMapping("/problems/algorithms")
  public String algorithms() {
    return "problems-algorithms";
  }

  /**
   * Displays a registered problem using its category and slug.
   *
   * @param category problem category
   * @param slug problem slug
   * @param model model used to pass the problem to the view
   * @return the generic problem page name
   */
  @GetMapping("/problems/{category}/{slug}")
  public String problem(
      @PathVariable final String category,
      @PathVariable final String slug,
      final Model model
  ) {
    final Problem problem = problemRegistry.findProblem(category, slug)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "Problem not found"
        ));

    model.addAttribute("problem", problem);

    return "problem";
  }

  /**
   * Displays the Longest Unique Substring problem.
   *
   * @return the Longest Unique Substring page name
   */
  @GetMapping("/problems/collections/longest-unique-substring")
  public String longestUniqueSubstring() {
    return "collections/longest-unique-substring";
  }
}
