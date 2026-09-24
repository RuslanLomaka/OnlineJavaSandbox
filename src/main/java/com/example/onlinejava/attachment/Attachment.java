package com.example.onlinejava.attachment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * An uploaded, already-sanitized screenshot.
 *
 * <p>The random UUID id doubles as the unguessable part of its URL. Rows are
 * unclaimed ({@code postId == null}) until a post that embeds them is saved.
 */
@Entity
@Table(name = "attachment")
public class Attachment {

  @Id
  private UUID id;

  @Column(name = "uploader_id", nullable = false, updatable = false)
  private long uploaderId;

  @Column(name = "post_id")
  private Long postId;

  @Column(name = "content_type", nullable = false, updatable = false)
  private String contentType;

  @Column(nullable = false, updatable = false)
  private int width;

  @Column(nullable = false, updatable = false)
  private int height;

  @Column(name = "size_bytes", nullable = false, updatable = false)
  private int sizeBytes;

  @Column(nullable = false, updatable = false)
  private byte[] data;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  /**
   * Required by JPA.
   */
  protected Attachment() {
  }

  /**
   * Creates an unclaimed attachment.
   *
   * @param id random id
   * @param uploaderId uploading user's id
   * @param contentType {@code image/png} or {@code image/jpeg}
   * @param width width in pixels
   * @param height height in pixels
   * @param data sanitized image bytes
   * @param now upload time
   */
  public Attachment(
      final UUID id,
      final long uploaderId,
      final String contentType,
      final int width,
      final int height,
      final byte[] data,
      final Instant now
  ) {
    this.id = id;
    this.uploaderId = uploaderId;
    this.contentType = contentType;
    this.width = width;
    this.height = height;
    this.data = data.clone();
    this.sizeBytes = data.length;
    this.createdAt = now;
  }

  /**
   * Returns the id.
   *
   * @return id
   */
  public UUID getId() {
    return id;
  }

  /**
   * Returns the uploader's user id.
   *
   * @return uploader id
   */
  public long getUploaderId() {
    return uploaderId;
  }

  /**
   * Returns the owning post id.
   *
   * @return post id, or {@code null} while unclaimed
   */
  public Long getPostId() {
    return postId;
  }

  /**
   * Returns the MIME type.
   *
   * @return content type
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * Returns the image bytes.
   *
   * @return a copy of the stored bytes
   */
  public byte[] getData() {
    return data.clone();
  }

  /**
   * Returns the file extension matching the content type.
   *
   * @return {@code png} or {@code jpg}
   */
  public String fileExtension() {
    return "image/jpeg".equals(contentType) ? "jpg" : "png";
  }
}
