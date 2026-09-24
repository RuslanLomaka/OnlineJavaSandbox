package com.example.onlinejava.discussion;

import com.example.onlinejava.attachment.AttachmentService;
import com.example.onlinejava.discussion.PostReactionRepository.ReactionCount;
import com.example.onlinejava.discussion.dto.AuthorView;
import com.example.onlinejava.discussion.dto.CreatePostRequest;
import com.example.onlinejava.discussion.dto.PostView;
import com.example.onlinejava.discussion.dto.ReactionView;
import com.example.onlinejava.discussion.dto.ReplyRef;
import com.example.onlinejava.discussion.dto.ThreadPage;
import com.example.onlinejava.discussion.dto.ThreadView;
import com.example.onlinejava.problem.ProblemRegistry;
import com.example.onlinejava.ratelimit.UserRateLimiter;
import com.example.onlinejava.user.AppUser;
import com.example.onlinejava.user.AppUserRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Business rules for problem discussions.
 *
 * <p>All authorization decisions (who may edit/delete what) are made here,
 * not in the UI. Post bodies are stored as raw Markdown and rendered to
 * sanitized HTML by {@link MarkdownRenderer} on the way out.
 */
@Service
@Transactional
public class DiscussionService {

  /** Threads per page. */
  public static final int PAGE_SIZE = 20;

  private static final int EXCERPT_LENGTH = 140;

  private final DiscussionPostRepository posts;

  private final PostReactionRepository reactions;

  private final AppUserRepository users;

  private final ProblemRegistry problemRegistry;

  private final MarkdownRenderer markdownRenderer;

  private final UserRateLimiter rateLimiter;

  private final AttachmentService attachmentService;

  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param posts post repository
   * @param reactions reaction repository
   * @param users user repository
   * @param problemRegistry used to reject unknown problem slugs
   * @param markdownRenderer renders post bodies
   * @param rateLimiter per-user rate limits
   * @param attachmentService links embedded screenshots to posts
   * @param clock source of the current time
   */
  public DiscussionService(
      final DiscussionPostRepository posts,
      final PostReactionRepository reactions,
      final AppUserRepository users,
      final ProblemRegistry problemRegistry,
      final MarkdownRenderer markdownRenderer,
      final UserRateLimiter rateLimiter,
      final AttachmentService attachmentService,
      final Clock clock
  ) {
    this.posts = posts;
    this.reactions = reactions;
    this.users = users;
    this.problemRegistry = problemRegistry;
    this.markdownRenderer = markdownRenderer;
    this.rateLimiter = rateLimiter;
    this.attachmentService = attachmentService;
    this.clock = clock;
  }

  /**
   * Returns one page of a problem's threads, newest first.
   *
   * @param slug problem slug
   * @param page zero-based page number
   * @param viewerId current user id
   * @return the page
   */
  @Transactional(readOnly = true)
  public ThreadPage listThreads(final String slug, final int page, final long viewerId) {
    requireProblem(slug);
    if (page < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must not be negative");
    }
    final Page<DiscussionPost> roots = posts.findThreads(slug, PageRequest.of(page, PAGE_SIZE));
    final List<Long> rootIds = roots.stream().map(DiscussionPost::getId).toList();
    final Map<Long, List<DiscussionPost>> repliesByThread = rootIds.isEmpty()
        ? Map.of()
        : posts.findReplies(rootIds).stream()
            .collect(Collectors.groupingBy(p -> p.getThread().getId()));

    final List<Long> allIds = new ArrayList<>(rootIds);
    repliesByThread.values().forEach(list -> list.forEach(p -> allIds.add(p.getId())));
    final Map<Long, List<ReactionView>> reactionsByPost = reactionViews(allIds, viewerId);

    final List<ThreadView> threads = roots.stream()
        .map(root -> new ThreadView(
            toView(root, viewerId, reactionsByPost),
            repliesByThread.getOrDefault(root.getId(), List.of()).stream()
                .map(reply -> toView(reply, viewerId, reactionsByPost))
                .toList()))
        .toList();
    return new ThreadPage(threads, page, roots.hasNext(), roots.getTotalElements());
  }

  /**
   * Counts a problem's visible posts.
   *
   * @param slug problem slug
   * @return number of non-deleted posts
   */
  @Transactional(readOnly = true)
  public long countPosts(final String slug) {
    requireProblem(slug);
    return posts.countByProblemSlugAndDeletedAtIsNull(slug);
  }

  /**
   * Creates a thread or a reply.
   *
   * @param slug problem slug
   * @param authorId current user id
   * @param request body and optional replied-to post
   * @return the created post
   */
  public PostView createPost(
      final String slug,
      final long authorId,
      final CreatePostRequest request
  ) {
    requireProblem(slug);
    rateLimiter.check(authorId, UserRateLimiter.Action.POST);

    DiscussionPost replyTo = null;
    if (request.replyToId() != null) {
      replyTo = findPost(request.replyToId());
      if (!replyTo.getProblemSlug().equals(slug)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Cannot reply to a post from another problem");
      }
      if (replyTo.isDeleted()) {
        throw new ResponseStatusException(HttpStatus.CONFLICT,
            "Cannot reply to a deleted post");
      }
    }
    final AppUser author = users.getReferenceById(authorId);
    final DiscussionPost post = posts.save(
        new DiscussionPost(slug, author, replyTo, request.body(), clock.instant()));
    // Build the response before claiming: the claim is a bulk update that
    // clears the persistence context, detaching the (lazy) entities.
    final PostView view = toView(post, authorId, Map.of());
    claimAttachments(post, authorId);
    return view;
  }

  /**
   * Edits a post. Only its author may do this.
   *
   * @param postId post id
   * @param userId current user id
   * @param body new Markdown body
   * @return the updated post
   */
  public PostView editPost(final long postId, final long userId, final String body) {
    final DiscussionPost post = findOwnLivePost(postId, userId);
    post.edit(body, clock.instant());
    // Build the response before the claim clears the persistence context
    // (the claim's bulk update flushes this edit first).
    final PostView view = toView(post, userId, reactionViews(List.of(postId), userId));
    claimAttachments(post, userId);
    return view;
  }

  /**
   * Soft-deletes a post. Only its author may do this; replies are kept.
   *
   * @param postId post id
   * @param userId current user id
   */
  public void deletePost(final long postId, final long userId) {
    findOwnLivePost(postId, userId).softDelete(clock.instant());
    attachmentService.deleteForPost(postId);
  }

  /**
   * Adds a reaction (no-op if already present).
   *
   * @param postId post id
   * @param userId current user id
   * @param reaction reaction type
   * @return the post's updated reaction counts
   */
  public List<ReactionView> addReaction(
      final long postId,
      final long userId,
      final Reaction reaction
  ) {
    rateLimiter.check(userId, UserRateLimiter.Action.REACTION);
    requireLive(findPost(postId));
    final PostReactionId id = new PostReactionId(postId, userId, reaction);
    if (!reactions.existsById(id)) {
      reactions.save(new PostReaction(id, clock.instant()));
    }
    return reactionViews(List.of(postId), userId).getOrDefault(postId, List.of());
  }

  /**
   * Removes a reaction (no-op if absent).
   *
   * @param postId post id
   * @param userId current user id
   * @param reaction reaction type
   * @return the post's updated reaction counts
   */
  public List<ReactionView> removeReaction(
      final long postId,
      final long userId,
      final Reaction reaction
  ) {
    rateLimiter.check(userId, UserRateLimiter.Action.REACTION);
    findPost(postId);
    reactions.deleteById(new PostReactionId(postId, userId, reaction));
    reactions.flush();
    return reactionViews(List.of(postId), userId).getOrDefault(postId, List.of());
  }

  private void claimAttachments(final DiscussionPost post, final long authorId) {
    attachmentService.claim(
        markdownRenderer.referencedAttachments(post.getBodyMarkdown()), authorId, post.getId());
  }

  private void requireProblem(final String slug) {
    if (problemRegistry.getProblem(slug) == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Problem not found");
    }
  }

  private DiscussionPost findPost(final long postId) {
    return posts.findById(postId).orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
  }

  private static void requireLive(final DiscussionPost post) {
    if (post.isDeleted()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Post was deleted");
    }
  }

  private DiscussionPost findOwnLivePost(final long postId, final long userId) {
    final DiscussionPost post = findPost(postId);
    if (!post.isAuthoredBy(userId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN,
          "You can only change your own posts");
    }
    requireLive(post);
    return post;
  }

  private Map<Long, List<ReactionView>> reactionViews(
      final List<Long> postIds,
      final long viewerId
  ) {
    if (postIds.isEmpty()) {
      return Map.of();
    }
    return reactions.summarize(postIds, viewerId).stream()
        .sorted(Comparator.comparing(ReactionCount::reaction))
        .collect(Collectors.groupingBy(
            ReactionCount::postId,
            Collectors.mapping(
                c -> new ReactionView(c.reaction().code(), c.reaction().emoji(), c.count(),
                    c.mine() > 0),
                Collectors.toList())));
  }

  private PostView toView(
      final DiscussionPost post,
      final long viewerId,
      final Map<Long, List<ReactionView>> reactionsByPost
  ) {
    final AppUser author = post.getAuthor();
    final DiscussionPost target = post.getReplyTo();
    final ReplyRef replyRef = target == null ? null : new ReplyRef(
        target.getId(),
        target.getAuthor().getLogin(),
        target.isDeleted() ? "" : excerpt(target.getBodyMarkdown()));
    final boolean own = post.isAuthoredBy(viewerId);
    return new PostView(
        post.getId(),
        new AuthorView(author.getLogin(), author.getDisplayName(), author.getAvatarUrl()),
        post.isDeleted() ? "" : markdownRenderer.render(post.getBodyMarkdown()),
        own && !post.isDeleted() ? post.getBodyMarkdown() : null,
        post.getCreatedAt(),
        post.getEditedAt(),
        post.isDeleted(),
        own,
        replyRef,
        reactionsByPost.getOrDefault(post.getId(), List.of()));
  }

  private static String excerpt(final String markdown) {
    final String flat = markdown.replaceAll("\\s+", " ").trim();
    return flat.length() <= EXCERPT_LENGTH ? flat : flat.substring(0, EXCERPT_LENGTH) + "…";
  }
}
