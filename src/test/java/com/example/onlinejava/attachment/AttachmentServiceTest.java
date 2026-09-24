package com.example.onlinejava.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.onlinejava.ClockConfig;
import com.example.onlinejava.TestcontainersConfiguration;
import com.example.onlinejava.ratelimit.UserRateLimiter;
import com.example.onlinejava.user.AppUser;
import com.example.onlinejava.user.AppUserRepository;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Integration tests for {@link AttachmentService} against PostgreSQL.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, AttachmentService.class, ImageSanitizer.class,
    UserRateLimiter.class, ClockConfig.class})
class AttachmentServiceTest {

  @Autowired
  private AttachmentService service;

  @Autowired
  private AttachmentRepository repository;

  @Autowired
  private AppUserRepository users;

  @Autowired
  private JdbcTemplate jdbc;

  private long alice;

  private long bob;

  @BeforeEach
  void createUsers() {
    alice = users.save(new AppUser("github", "a", "alice", "Alice", null, Instant.now())).getId();
    bob = users.save(new AppUser("github", "b", "bob", "Bob", null, Instant.now())).getId();
  }

  @Test
  void uploadStoresSanitizedImageAndReturnsMarkdown() {
    final AttachmentView view = service.upload(alice, TestImages.png(8, 8));

    assertThat(view.url()).isEqualTo("/attachments/" + view.id());
    assertThat(view.markdown()).isEqualTo("![screenshot](/attachments/" + view.id() + ")");
    final Attachment stored = service.load(view.id());
    assertThat(stored.getContentType()).isEqualTo("image/png");
    assertThat(stored.getUploaderId()).isEqualTo(alice);
    assertThat(stored.getPostId()).isNull();
  }

  @Test
  void rejectsFilesOverTheSizeLimit() {
    assertThatThrownBy(() -> service.upload(alice, new byte[AttachmentService.MAX_BYTES + 1]))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
  }

  @Test
  void unknownAttachmentIsNotFound() {
    assertThatThrownBy(() -> service.load(UUID.randomUUID()))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);
  }

  @Test
  void claimOnlyLinksOwnUnclaimedAttachments() {
    final long postId = insertPost(alice);
    final UUID mine = service.upload(alice, TestImages.png(4, 4)).id();
    final UUID theirs = service.upload(bob, TestImages.png(4, 4)).id();

    service.claim(Set.of(mine, theirs), alice, postId);

    assertThat(repository.findById(mine).orElseThrow().getPostId()).isEqualTo(postId);
    assertThat(repository.findById(theirs).orElseThrow().getPostId()).isNull();
  }

  @Test
  void deletesOnlyOldOrphans() {
    final long postId = insertPost(alice);
    final UUID oldOrphan = service.upload(alice, TestImages.png(4, 4)).id();
    final UUID claimed = service.upload(alice, TestImages.png(4, 4)).id();
    service.claim(Set.of(claimed), alice, postId);
    jdbc.update("update attachment set created_at = now() - interval '2 days'");
    final UUID freshOrphan = service.upload(alice, TestImages.png(4, 4)).id();

    final int deleted = service.deleteOrphansCreatedBefore(Instant.now().minusSeconds(3600));

    assertThat(deleted).isEqualTo(1);
    assertThat(repository.existsById(oldOrphan)).isFalse();
    assertThat(repository.existsById(claimed)).isTrue();
    assertThat(repository.existsById(freshOrphan)).isTrue();
  }

  @Test
  void deleteForPostRemovesItsAttachments() {
    final long postId = insertPost(alice);
    final UUID id = service.upload(alice, TestImages.png(4, 4)).id();
    service.claim(Set.of(id), alice, postId);

    service.deleteForPost(postId);

    assertThat(repository.existsById(id)).isFalse();
  }

  private long insertPost(final long authorId) {
    return jdbc.queryForObject("""
        insert into discussion_post (problem_slug, author_id, body_markdown, created_at)
        values ('bubble-sort', ?, 'x', now()) returning id
        """, Long.class, authorId);
  }
}
