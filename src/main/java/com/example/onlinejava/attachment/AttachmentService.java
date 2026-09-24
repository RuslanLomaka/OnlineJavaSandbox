package com.example.onlinejava.attachment;

import com.example.onlinejava.ratelimit.UserRateLimiter;
import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Stores, serves and cleans up screenshot attachments.
 */
@Service
@Transactional
public class AttachmentService {

  /** Upload size limit in bytes (the multipart limit in application.properties matches). */
  public static final int MAX_BYTES = 2 * 1024 * 1024;

  private final AttachmentRepository repository;

  private final ImageSanitizer sanitizer;

  private final UserRateLimiter rateLimiter;

  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param repository attachment repository
   * @param sanitizer validates and re-encodes images
   * @param rateLimiter per-user upload limits
   * @param clock source of the current time
   */
  public AttachmentService(
      final AttachmentRepository repository,
      final ImageSanitizer sanitizer,
      final UserRateLimiter rateLimiter,
      final Clock clock
  ) {
    this.repository = repository;
    this.sanitizer = sanitizer;
    this.rateLimiter = rateLimiter;
    this.clock = clock;
  }

  /**
   * Validates, re-encodes and stores an uploaded image.
   *
   * @param uploaderId current user id
   * @param data raw uploaded bytes
   * @return id, URL and Markdown snippet for the stored image
   */
  public AttachmentView upload(final long uploaderId, final byte[] data) {
    if (data.length > MAX_BYTES) {
      throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE,
          "Images must be at most 2 MB");
    }
    rateLimiter.check(uploaderId, UserRateLimiter.Action.UPLOAD);
    final ImageSanitizer.SanitizedImage image = sanitizer.sanitize(data);
    final Attachment attachment = repository.save(new Attachment(
        UUID.randomUUID(), uploaderId, image.contentType(), image.width(), image.height(),
        image.data(), clock.instant()));
    return AttachmentView.of(attachment.getId());
  }

  /**
   * Loads an attachment for serving.
   *
   * @param id attachment id
   * @return the attachment
   * @throws ResponseStatusException 404 if it doesn't exist
   */
  @Transactional(readOnly = true)
  public Attachment load(final UUID id) {
    return repository.findById(id).orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
  }

  /**
   * Links attachments embedded in a post to that post, so they survive the
   * orphan cleanup. Only the author's own, still-unclaimed uploads are linked.
   *
   * @param ids attachment ids referenced in the post body
   * @param authorId post author id
   * @param postId post id
   */
  public void claim(final Set<UUID> ids, final long authorId, final long postId) {
    if (!ids.isEmpty()) {
      repository.claim(ids, authorId, postId);
    }
  }

  /**
   * Deletes the attachments of a deleted post.
   *
   * @param postId post id
   */
  public void deleteForPost(final long postId) {
    repository.deleteByPost(postId);
  }

  /**
   * Deletes uploads that were never used in a post.
   *
   * @param cutoff only uploads older than this are removed
   * @return number deleted
   */
  public int deleteOrphansCreatedBefore(final Instant cutoff) {
    return repository.deleteOrphansCreatedBefore(cutoff);
  }
}
