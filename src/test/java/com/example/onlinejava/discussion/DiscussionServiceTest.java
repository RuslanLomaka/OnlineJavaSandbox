package com.example.onlinejava.discussion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.onlinejava.ClockConfig;
import com.example.onlinejava.TestcontainersConfiguration;
import com.example.onlinejava.discussion.dto.CreatePostRequest;
import com.example.onlinejava.discussion.dto.PostView;
import com.example.onlinejava.discussion.dto.ReactionView;
import com.example.onlinejava.discussion.dto.ThreadPage;
import com.example.onlinejava.problem.ProblemRegistry;
import com.example.onlinejava.ratelimit.UserRateLimiter;
import com.example.onlinejava.user.AppUser;
import com.example.onlinejava.user.AppUserRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Integration tests for {@link DiscussionService} against PostgreSQL.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, DiscussionService.class, MarkdownRenderer.class,
    ProblemRegistry.class, UserRateLimiter.class, ClockConfig.class})
class DiscussionServiceTest {

  private static final String SLUG = "bubble-sort";

  @Autowired
  private DiscussionService service;

  @Autowired
  private AppUserRepository users;

  private long alice;

  private long bob;

  @BeforeEach
  void createUsers() {
    alice = users.save(new AppUser("github", "1", "alice", "Alice", null, Instant.now())).getId();
    bob = users.save(new AppUser("github", "2", "bob", "Bob", null, Instant.now())).getId();
  }

  @Test
  void topLevelPostStartsThread() {
    final PostView post = service.createPost(SLUG, alice, new CreatePostRequest("**hi**", null));

    final ThreadPage page = service.listThreads(SLUG, 0, bob);

    assertThat(page.threads()).hasSize(1);
    assertThat(page.threads().get(0).root().id()).isEqualTo(post.id());
    assertThat(page.threads().get(0).root().bodyHtml()).isEqualTo("<p><strong>hi</strong></p>");
    assertThat(page.threads().get(0).root().author().login()).isEqualTo("alice");
    assertThat(page.threads().get(0).root().own()).isFalse();
  }

  @Test
  void nestedReplyIsStoredUnderThreadRoot() {
    final PostView root = service.createPost(SLUG, alice, new CreatePostRequest("root", null));
    final PostView reply = service.createPost(SLUG, bob, new CreatePostRequest("r1", root.id()));
    final PostView nested = service.createPost(SLUG, alice, new CreatePostRequest("r2",
        reply.id()));

    final ThreadPage page = service.listThreads(SLUG, 0, alice);

    assertThat(page.threads()).hasSize(1);
    final List<PostView> replies = page.threads().get(0).replies();
    assertThat(replies).extracting(PostView::id).containsExactly(reply.id(), nested.id());
    assertThat(replies.get(1).replyTo().id()).isEqualTo(reply.id());
    assertThat(replies.get(1).replyTo().authorLogin()).isEqualTo("bob");
    assertThat(replies.get(1).replyTo().excerpt()).isEqualTo("r1");
  }

  @Test
  void cannotReplyAcrossProblems() {
    final PostView other = service.createPost("two-sum", alice, new CreatePostRequest("x", null));

    assertStatus(() -> service.createPost(SLUG, bob, new CreatePostRequest("y", other.id())),
        HttpStatus.BAD_REQUEST);
  }

  @Test
  void unknownProblemIsNotFound() {
    assertStatus(() -> service.createPost("nope", alice, new CreatePostRequest("x", null)),
        HttpStatus.NOT_FOUND);
    assertStatus(() -> service.listThreads("nope", 0, alice), HttpStatus.NOT_FOUND);
  }

  @Test
  void authorCanEditOwnPost() {
    final PostView post = service.createPost(SLUG, alice, new CreatePostRequest("old", null));

    final PostView edited = service.editPost(post.id(), alice, "new");

    assertThat(edited.bodyHtml()).isEqualTo("<p>new</p>");
    assertThat(edited.editedAt()).isNotNull();
  }

  @Test
  void otherUsersCannotEditOrDelete() {
    final PostView post = service.createPost(SLUG, alice, new CreatePostRequest("mine", null));

    assertStatus(() -> service.editPost(post.id(), bob, "hacked"), HttpStatus.FORBIDDEN);
    assertStatus(() -> service.deletePost(post.id(), bob), HttpStatus.FORBIDDEN);
  }

  @Test
  void softDeleteHidesBodyButKeepsReplies() {
    final PostView root = service.createPost(SLUG, alice, new CreatePostRequest("secret", null));
    service.createPost(SLUG, bob, new CreatePostRequest("reply", root.id()));

    service.deletePost(root.id(), alice);

    final ThreadPage page = service.listThreads(SLUG, 0, bob);
    assertThat(page.threads()).hasSize(1);
    assertThat(page.threads().get(0).root().deleted()).isTrue();
    assertThat(page.threads().get(0).root().bodyHtml()).isEmpty();
    assertThat(page.threads().get(0).replies()).hasSize(1);
    assertStatus(() -> service.editPost(root.id(), alice, "again"), HttpStatus.CONFLICT);
  }

  @Test
  void deletedReplyTargetExcerptIsHidden() {
    final PostView root = service.createPost(SLUG, alice, new CreatePostRequest("root", null));
    final PostView reply = service.createPost(SLUG, alice, new CreatePostRequest("oops",
        root.id()));
    service.createPost(SLUG, bob, new CreatePostRequest("answer", reply.id()));
    service.deletePost(reply.id(), alice);

    final List<PostView> replies = service.listThreads(SLUG, 0, bob).threads().get(0).replies();

    assertThat(replies.get(1).replyTo().excerpt()).isEmpty();
  }

  @Test
  void postingIsRateLimited() {
    for (int i = 0; i < UserRateLimiter.Action.POST.capacity(); i++) {
      service.createPost(SLUG, alice, new CreatePostRequest("post " + i, null));
    }

    assertStatus(() -> service.createPost(SLUG, alice, new CreatePostRequest("spam", null)),
        HttpStatus.TOO_MANY_REQUESTS);
    // Other users have their own budget.
    service.createPost(SLUG, bob, new CreatePostRequest("fine", null));
  }

  @Test
  void threadsArePagedNewestFirst() {
    // One post per user keeps each author under the posting rate limit.
    final int total = DiscussionService.PAGE_SIZE + 1;
    for (int i = 0; i < total; i++) {
      final long author = users.save(
          new AppUser("github", "p" + i, "user" + i, "U" + i, null, Instant.now())).getId();
      service.createPost(SLUG, author, new CreatePostRequest("thread " + i, null));
    }

    final ThreadPage first = service.listThreads(SLUG, 0, alice);
    final ThreadPage second = service.listThreads(SLUG, 1, alice);

    assertThat(first.threads()).hasSize(DiscussionService.PAGE_SIZE);
    assertThat(first.hasMore()).isTrue();
    assertThat(first.totalThreads()).isEqualTo(total);
    assertThat(first.threads().get(0).root().bodyHtml()).isEqualTo("<p>thread " + (total - 1)
        + "</p>");
    assertThat(second.threads()).hasSize(1);
    assertThat(second.hasMore()).isFalse();
  }

  @Test
  void reactionsAreIdempotentAndCounted() {
    final PostView post = service.createPost(SLUG, alice, new CreatePostRequest("nice", null));

    service.addReaction(post.id(), bob, Reaction.THUMBS_UP);
    service.addReaction(post.id(), bob, Reaction.THUMBS_UP);
    final List<ReactionView> reactions = service.addReaction(post.id(), alice, Reaction.THUMBS_UP);

    assertThat(reactions).singleElement().satisfies(r -> {
      assertThat(r.code()).isEqualTo("thumbs_up");
      assertThat(r.emoji()).isEqualTo("👍");
      assertThat(r.count()).isEqualTo(2);
      assertThat(r.reactedByMe()).isTrue();
    });

    final List<ReactionView> afterRemove = service.removeReaction(post.id(), alice,
        Reaction.THUMBS_UP);
    assertThat(afterRemove).singleElement().satisfies(r -> {
      assertThat(r.count()).isEqualTo(1);
      assertThat(r.reactedByMe()).isFalse();
    });
  }

  @Test
  void cannotReactToDeletedPost() {
    final PostView post = service.createPost(SLUG, alice, new CreatePostRequest("x", null));
    service.deletePost(post.id(), alice);

    assertStatus(() -> service.addReaction(post.id(), bob, Reaction.HEART), HttpStatus.CONFLICT);
  }

  @Test
  void countsVisiblePosts() {
    final PostView root = service.createPost(SLUG, alice, new CreatePostRequest("a", null));
    service.createPost(SLUG, bob, new CreatePostRequest("b", root.id()));
    final PostView gone = service.createPost(SLUG, bob, new CreatePostRequest("c", null));
    service.deletePost(gone.id(), bob);

    assertThat(service.countPosts(SLUG)).isEqualTo(2);
  }

  private static void assertStatus(final Runnable action, final HttpStatus status) {
    assertThatThrownBy(action::run)
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(status);
  }
}
