package com.example.onlinejava.discussion;

import com.example.onlinejava.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

/**
 * A message in a problem's discussion.
 *
 * <p>Top-level posts have no {@link #getThread() thread}; replies reference
 * their thread's root post, plus the specific post they answer via
 * {@link #getReplyTo()}. Deletion is soft so replies keep their context.
 */
@Entity
@Table(name = "discussion_post")
public class DiscussionPost {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "problem_slug", nullable = false, updatable = false)
  private String problemSlug;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "author_id", nullable = false, updatable = false)
  private AppUser author;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "thread_id", updatable = false)
  private DiscussionPost thread;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reply_to_id", updatable = false)
  private DiscussionPost replyTo;

  @Column(name = "body_markdown", nullable = false)
  private String bodyMarkdown;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "edited_at")
  private Instant editedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  /** Optimistic-locking version; concurrent edits fail instead of overwriting. */
  @Version
  private long version;

  /**
   * Required by JPA.
   */
  protected DiscussionPost() {
  }

  /**
   * Creates a new post.
   *
   * @param problemSlug slug of the problem being discussed
   * @param author post author
   * @param replyTo post being answered, or {@code null} to start a thread
   * @param bodyMarkdown raw Markdown body
   * @param now creation time
   */
  public DiscussionPost(
      final String problemSlug,
      final AppUser author,
      final DiscussionPost replyTo,
      final String bodyMarkdown,
      final Instant now
  ) {
    this.problemSlug = problemSlug;
    this.author = author;
    this.replyTo = replyTo;
    // Replies always hang off the thread root, keeping threads two levels deep.
    this.thread = replyTo == null ? null : replyTo.threadRoot();
    this.bodyMarkdown = bodyMarkdown;
    this.createdAt = now;
  }

  /**
   * Replaces the body.
   *
   * @param newBody new Markdown body
   * @param now edit time
   */
  public void edit(final String newBody, final Instant now) {
    this.bodyMarkdown = newBody;
    this.editedAt = now;
  }

  /**
   * Marks the post deleted and discards its content.
   *
   * @param now deletion time
   */
  public void softDelete(final Instant now) {
    this.bodyMarkdown = "";
    this.deletedAt = now;
  }

  /**
   * Returns the root post of this post's thread (itself for a top-level post).
   *
   * @return thread root
   */
  public DiscussionPost threadRoot() {
    return thread == null ? this : thread;
  }

  /**
   * Returns whether the given user wrote this post.
   *
   * @param userId app user id
   * @return {@code true} if {@code userId} is the author
   */
  public boolean isAuthoredBy(final long userId) {
    return author.getId() == userId;
  }

  /**
   * Returns whether the post has been deleted.
   *
   * @return {@code true} once soft-deleted
   */
  public boolean isDeleted() {
    return deletedAt != null;
  }

  /**
   * Returns the database id.
   *
   * @return id
   */
  public Long getId() {
    return id;
  }

  /**
   * Returns the discussed problem's slug.
   *
   * @return problem slug
   */
  public String getProblemSlug() {
    return problemSlug;
  }

  /**
   * Returns the author.
   *
   * @return author
   */
  public AppUser getAuthor() {
    return author;
  }

  /**
   * Returns the thread root, or {@code null} for a top-level post.
   *
   * @return thread root or {@code null}
   */
  public DiscussionPost getThread() {
    return thread;
  }

  /**
   * Returns the post this one answers, or {@code null}.
   *
   * @return replied-to post or {@code null}
   */
  public DiscussionPost getReplyTo() {
    return replyTo;
  }

  /**
   * Returns the raw Markdown body (empty once deleted).
   *
   * @return Markdown body
   */
  public String getBodyMarkdown() {
    return bodyMarkdown;
  }

  /**
   * Returns the creation time.
   *
   * @return creation time
   */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Returns the last edit time.
   *
   * @return edit time, or {@code null} if never edited
   */
  public Instant getEditedAt() {
    return editedAt;
  }
}
