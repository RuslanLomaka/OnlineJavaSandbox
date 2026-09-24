package com.example.onlinejava.attachment;

import com.example.onlinejava.user.AppUserPrincipal;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Uploads and serves screenshot attachments.
 */
@RestController
public class AttachmentController {

  private final AttachmentService service;

  /**
   * Creates the controller.
   *
   * @param service attachment service
   */
  public AttachmentController(final AttachmentService service) {
    this.service = service;
  }

  /**
   * Uploads an image. Requires login and a CSRF token.
   *
   * @param file multipart file field named {@code file}
   * @param uploader current user
   * @return id, URL and Markdown snippet
   * @throws IOException if the upload can't be read
   */
  @PostMapping(value = "/api/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public AttachmentView upload(
      @RequestParam("file") final MultipartFile file,
      @AuthenticationPrincipal final AppUserPrincipal uploader
  ) throws IOException {
    if (uploader == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in");
    }
    return service.upload(uploader.getUserId(), file.getBytes());
  }

  /**
   * Serves an image with headers that stop browsers from treating it as
   * anything but an image: no MIME sniffing, and a CSP that forbids scripts
   * even if the file is opened directly.
   *
   * @param id attachment id
   * @return the image
   */
  @GetMapping("/attachments/{id}")
  public ResponseEntity<byte[]> serve(@PathVariable final UUID id) {
    final Attachment attachment = service.load(id);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(attachment.getContentType()))
        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePrivate().immutable())
        .header("X-Content-Type-Options", "nosniff")
        .header("Content-Security-Policy", "default-src 'none'; sandbox")
        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
            .filename("screenshot." + attachment.fileExtension())
            .build()
            .toString())
        .body(attachment.getData());
  }
}
