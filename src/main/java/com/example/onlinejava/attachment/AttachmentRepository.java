package com.example.onlinejava.attachment;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for {@link Attachment}. Bulk operations avoid loading image
 * bytes into memory.
 */
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

  /**
   * Links the uploader's own unclaimed attachments to a post.
   *
   * @param ids attachment ids referenced by the post
   * @param uploaderId post author; other users' uploads are left alone
   * @param postId post id
   * @return number of attachments linked
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("""
      update Attachment a set a.postId = :postId
      where a.id in :ids and a.uploaderId = :uploaderId and a.postId is null
      """)
  int claim(
      @Param("ids") Collection<UUID> ids,
      @Param("uploaderId") long uploaderId,
      @Param("postId") long postId
  );

  /**
   * Deletes attachments that were never used in a post.
   *
   * @param cutoff only uploads created before this time are removed
   * @return number deleted
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from Attachment a where a.postId is null and a.createdAt < :cutoff")
  int deleteOrphansCreatedBefore(@Param("cutoff") Instant cutoff);

  /**
   * Deletes all attachments of a post.
   *
   * @param postId post id
   * @return number deleted
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from Attachment a where a.postId = :postId")
  int deleteByPost(@Param("postId") long postId);

  /**
   * Who uploaded a set of attachments, and which post (if any) currently
   * claims each one. Used to decide whether a post embedding one of these
   * ids is actually entitled to render it -- an id alone isn't enough,
   * since {@link #claim} lets the same attachment be referenced by
   * Markdown in more than one post's text even though only the first
   * claimer actually owns it in the database.
   *
   * @param ids attachment ids to look up
   * @return ownership info for each id that still exists
   */
  @Query("""
      select new com.example.onlinejava.attachment.AttachmentRepository$AttachmentOwnership(
          a.id, a.uploaderId, a.postId)
      from Attachment a
      where a.id in :ids
      """)
  List<AttachmentOwnership> findOwnership(@Param("ids") Collection<UUID> ids);

  /**
   * One attachment's uploader and current claiming post, as returned by
   * {@link #findOwnership}.
   *
   * @param id attachment id
   * @param uploaderId who uploaded it
   * @param postId the post that currently claims it, or {@code null} if
   *     still unclaimed
   */
  record AttachmentOwnership(UUID id, long uploaderId, Long postId) {
  }
}
