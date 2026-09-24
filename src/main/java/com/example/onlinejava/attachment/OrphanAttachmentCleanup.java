package com.example.onlinejava.attachment;

import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hourly job that deletes screenshots uploaded more than a day ago but never
 * used in a post (e.g. the user abandoned the draft).
 */
@Component
public class OrphanAttachmentCleanup {

  /** How long an unused upload is kept, giving time to finish a long post. */
  static final Duration MAX_ORPHAN_AGE = Duration.ofHours(24);

  private static final Logger log = LoggerFactory.getLogger(OrphanAttachmentCleanup.class);

  private final AttachmentService service;

  private final Clock clock;

  /**
   * Creates the job.
   *
   * @param service attachment service
   * @param clock source of the current time
   */
  public OrphanAttachmentCleanup(final AttachmentService service, final Clock clock) {
    this.service = service;
    this.clock = clock;
  }

  /**
   * Deletes orphaned uploads older than {@link #MAX_ORPHAN_AGE}.
   */
  @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT5M")
  public void run() {
    final int deleted = service.deleteOrphansCreatedBefore(clock.instant().minus(MAX_ORPHAN_AGE));
    if (deleted > 0) {
      log.info("Deleted {} unused attachment(s)", deleted);
    }
  }
}
