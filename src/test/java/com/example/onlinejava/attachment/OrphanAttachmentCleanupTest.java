package com.example.onlinejava.attachment;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link OrphanAttachmentCleanup}.
 */
class OrphanAttachmentCleanupTest {

  @Test
  void deletesOrphansOlderThanOneDay() {
    final AttachmentService service = mock(AttachmentService.class);
    final Instant now = Instant.parse("2026-05-02T12:00:00Z");

    new OrphanAttachmentCleanup(service, Clock.fixed(now, ZoneOffset.UTC)).run();

    verify(service).deleteOrphansCreatedBefore(Instant.parse("2026-05-01T12:00:00Z"));
  }
}
