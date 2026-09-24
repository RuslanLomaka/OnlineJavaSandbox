package com.example.onlinejava.discussion;

import com.example.onlinejava.problem.Problem;
import com.example.onlinejava.problem.ProblemRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

/**
 * Serves the discussion page of a problem. The page is a shell; threads are
 * loaded and posted by {@code discussion.js} through
 * {@link DiscussionApiController}.
 */
@Controller
public class DiscussionPageController {

  private final ProblemRegistry problemRegistry;

  /**
   * Creates the controller.
   *
   * @param problemRegistry registry of available problems
   */
  public DiscussionPageController(final ProblemRegistry problemRegistry) {
    this.problemRegistry = problemRegistry;
  }

  /**
   * Displays a problem's discussion.
   *
   * @param category problem category from the URL
   * @param slug problem slug
   * @param model view model
   * @return the discussion template
   */
  @GetMapping("/problems/{category}/{slug}/discussion")
  public String discussion(
      @PathVariable final String category,
      @PathVariable final String slug,
      final Model model
  ) {
    final Problem problem = problemRegistry.findProblem(category, slug)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Problem not found"));
    model.addAttribute("problem", problem);
    return "discussion";
  }
}
