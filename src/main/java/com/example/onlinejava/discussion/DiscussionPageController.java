package com.example.onlinejava.discussion;

import com.example.onlinejava.Redirects;
import com.example.onlinejava.problem.Problem;
import com.example.onlinejava.problem.ProblemRegistry;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

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
   * Displays a problem's discussion, redirecting old URLs to the canonical one.
   *
   * @param topicSlug topic slug from the URL (or an old category name)
   * @param slug problem slug
   * @return the discussion page, or a 301 redirect
   */
  @GetMapping("/problems/{topicSlug}/{slug}/discussion")
  public ModelAndView discussion(
      @PathVariable final String topicSlug,
      @PathVariable final String slug
  ) {
    final Problem problem = problemRegistry.findBySlug(slug)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Problem not found"));
    if (!problem.getTopic().slug().equals(topicSlug)) {
      return Redirects.permanent(problem.getPath() + "/discussion");
    }
    return new ModelAndView("discussion", Map.of("problem", problem));
  }
}
