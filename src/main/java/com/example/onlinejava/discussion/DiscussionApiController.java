package com.example.onlinejava.discussion;

import com.example.onlinejava.discussion.dto.CreatePostRequest;
import com.example.onlinejava.discussion.dto.DiscussionSummary;
import com.example.onlinejava.discussion.dto.EditPostRequest;
import com.example.onlinejava.discussion.dto.PostView;
import com.example.onlinejava.discussion.dto.ReactionView;
import com.example.onlinejava.discussion.dto.ThreadPage;
import com.example.onlinejava.user.AppUserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * JSON API behind the discussion page.
 *
 * <p>Every endpoint requires a signed-in {@link AppUserPrincipal}; state
 * changes additionally require a CSRF token (enforced by Spring Security).
 * Business rules and ownership checks live in {@link DiscussionService}.
 */
@RestController
@RequestMapping("/api")
public class DiscussionApiController {

  private final DiscussionService service;

  /**
   * Creates the controller.
   *
   * @param service discussion business logic
   */
  public DiscussionApiController(final DiscussionService service) {
    this.service = service;
  }

  /**
   * Lists a page of threads.
   *
   * @param slug problem slug
   * @param page zero-based page number
   * @param viewer current user
   * @return threads with replies
   */
  @GetMapping("/problems/{slug}/threads")
  public ThreadPage threads(
      @PathVariable final String slug,
      @RequestParam(defaultValue = "0") final int page,
      @AuthenticationPrincipal final AppUserPrincipal viewer
  ) {
    return service.listThreads(slug, page, requireUser(viewer));
  }

  /**
   * Returns post counts for the problem page's "Discussion" link.
   *
   * @param slug problem slug
   * @param viewer current user
   * @return summary
   */
  @GetMapping("/problems/{slug}/summary")
  public DiscussionSummary summary(
      @PathVariable final String slug,
      @AuthenticationPrincipal final AppUserPrincipal viewer
  ) {
    requireUser(viewer);
    return new DiscussionSummary(service.countPosts(slug));
  }

  /**
   * Creates a thread or reply.
   *
   * @param slug problem slug
   * @param request post body and optional reply target
   * @param viewer current user
   * @return the created post
   */
  @PostMapping("/problems/{slug}/posts")
  @ResponseStatus(HttpStatus.CREATED)
  public PostView create(
      @PathVariable final String slug,
      @Valid @RequestBody final CreatePostRequest request,
      @AuthenticationPrincipal final AppUserPrincipal viewer
  ) {
    return service.createPost(slug, requireUser(viewer), request);
  }

  /**
   * Edits one of the current user's posts.
   *
   * @param id post id
   * @param request new body
   * @param viewer current user
   * @return the updated post
   */
  @PatchMapping("/posts/{id}")
  public PostView edit(
      @PathVariable final long id,
      @Valid @RequestBody final EditPostRequest request,
      @AuthenticationPrincipal final AppUserPrincipal viewer
  ) {
    return service.editPost(id, requireUser(viewer), request.body());
  }

  /**
   * Deletes one of the current user's posts.
   *
   * @param id post id
   * @param viewer current user
   */
  @DeleteMapping("/posts/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @PathVariable final long id,
      @AuthenticationPrincipal final AppUserPrincipal viewer
  ) {
    service.deletePost(id, requireUser(viewer));
  }

  /**
   * Adds a reaction.
   *
   * @param id post id
   * @param reaction reaction code, e.g. {@code thumbs_up}
   * @param viewer current user
   * @return updated reaction counts
   */
  @PutMapping("/posts/{id}/reactions/{reaction}")
  public List<ReactionView> react(
      @PathVariable final long id,
      @PathVariable final Reaction reaction,
      @AuthenticationPrincipal final AppUserPrincipal viewer
  ) {
    return service.addReaction(id, requireUser(viewer), reaction);
  }

  /**
   * Removes a reaction.
   *
   * @param id post id
   * @param reaction reaction code
   * @param viewer current user
   * @return updated reaction counts
   */
  @DeleteMapping("/posts/{id}/reactions/{reaction}")
  public List<ReactionView> unreact(
      @PathVariable final long id,
      @PathVariable final Reaction reaction,
      @AuthenticationPrincipal final AppUserPrincipal viewer
  ) {
    return service.removeReaction(id, requireUser(viewer), reaction);
  }

  private static long requireUser(final AppUserPrincipal viewer) {
    // Security config already requires login; this also covers principals
    // of an unexpected type (e.g. a session created before AppUserPrincipal).
    if (viewer == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in");
    }
    return viewer.getUserId();
  }
}
