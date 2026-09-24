package com.example.onlinejava.attachment;

import java.util.UUID;

/**
 * Upload response.
 *
 * @param id attachment id
 * @param url URL the image is served from
 * @param markdown ready-to-insert Markdown image snippet
 */
public record AttachmentView(UUID id, String url, String markdown) {

  /**
   * Builds the view for an attachment id.
   *
   * @param id attachment id
   * @return the view
   */
  public static AttachmentView of(final UUID id) {
    final String url = "/attachments/" + id;
    return new AttachmentView(id, url, "![screenshot](" + url + ")");
  }
}
