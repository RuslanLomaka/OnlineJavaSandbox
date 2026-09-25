package com.example.onlinejava.editor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.onlinejava.ratelimit.UserRateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tests for {@link CodeFormattingService}, which combines import organizing,
 * formatting and rate limiting.
 */
class CodeFormattingServiceTest {

  private final CodeFormattingService service = new CodeFormattingService(
      new JavaFormatter(), new ImportOrganizer(), new UserRateLimiter());

  @Test
  void organizesImportsThenFormatsClasses() {
    final FormatResponse response = service.format(1L, new FormatRequest(
        "import java.io.File;\nclass A{List<String> xs=new ArrayList<>();}",
        SourceKind.CLASS, true));

    assertThat(response.changed()).isTrue();
    assertThat(response.code()).isEqualTo("""
        import java.util.ArrayList;
        import java.util.List;

        class A {
            List<String> xs = new ArrayList<>();
        }
        """);
  }

  @Test
  void ignoresImportOrganizingForMethodBodies() {
    final FormatResponse response = service.format(2L,
        new FormatRequest("List<Integer> xs=new ArrayList<>();", SourceKind.METHOD_BODY, true));

    assertThat(response.code()).isEqualTo("List<Integer> xs = new ArrayList<>();\n");
  }

  @Test
  void reportsUnchangedCode() {
    assertThat(service.format(3L,
        new FormatRequest("int x = 1;\n", SourceKind.METHOD_BODY, false)).changed()).isFalse();
  }

  @Test
  void isRateLimitedPerUser() {
    final FormatRequest request = new FormatRequest("int x = 1;\n", SourceKind.METHOD_BODY, false);
    for (int i = 0; i < UserRateLimiter.Action.FORMAT.capacity(); i++) {
      service.format(4L, request);
    }

    assertThatThrownBy(() -> service.format(4L, request))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
  }
}
