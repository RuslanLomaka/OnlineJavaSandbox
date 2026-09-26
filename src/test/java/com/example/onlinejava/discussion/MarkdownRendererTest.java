package com.example.onlinejava.discussion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link MarkdownRenderer}: formatting that must survive, and an
 * XSS corpus that must be neutralized.
 */
class MarkdownRendererTest {

  private static final UUID ATTACHMENT_ID =
      UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

  private static final String ATTACHMENT = "/attachments/" + ATTACHMENT_ID;

  private static final Set<UUID> NO_ATTACHMENTS = Set.of();

  private final MarkdownRenderer renderer = new MarkdownRenderer();

  @Test
  void rendersBasicFormatting() {
    final String html = renderer.render("**bold** _it_ ~~gone~~ `x`", NO_ATTACHMENTS);

    assertThat(html)
        .contains("<strong>bold</strong>")
        .contains("<em>it</em>")
        .contains("<del>gone</del>")
        .contains("<code>x</code>");
  }

  @Test
  void keepsFencedCodeWithLanguageAndEscapesItsContent() {
    final String html = renderer.render(
        "```java\nif (a < b && c > d) {}\n```", NO_ATTACHMENTS);

    assertThat(html)
        .contains("<pre><code class=\"language-java\">")
        .contains("if (a &lt; b &amp;&amp; c &gt; d) {}");
  }

  @Test
  void dropsMaliciousCodeLanguageClass() {
    final String html = renderer.render(
        "```java\" onmouseover=\"alert(1)\ncode\n```", NO_ATTACHMENTS);

    assertThat(html).doesNotContain("onmouseover=");
  }

  @Test
  void linksOpenSafelyInNewTab() {
    final String html = renderer.render("[docs](https://example.com/x)", NO_ATTACHMENTS);

    assertThat(html)
        .contains("href=\"https://example.com/x\"")
        .contains("target=\"_blank\"")
        .contains("rel=\"nofollow noopener noreferrer\"");
  }

  @Test
  void autolinksBareUrls() {
    assertThat(renderer.render("see https://example.com", NO_ATTACHMENTS))
        .contains("<a href=\"https://example.com\"");
  }

  @Test
  void rendersTables() {
    assertThat(renderer.render("| a | b |\n|---|---|\n| 1 | 2 |", NO_ATTACHMENTS))
        .contains("<table>").contains("<td>1</td>");
  }

  @Test
  void keepsImagesThatAreInTheAllowedSet() {
    final String html = renderer.render("![shot](" + ATTACHMENT + ")", Set.of(ATTACHMENT_ID));

    assertThat(html).contains("<img src=\"" + ATTACHMENT + "\"").contains("alt=\"shot\"");
  }

  @Test
  void dropsAttachmentShapedImagesNotInTheAllowedSet() {
    // Same URL shape as a real attachment, but its id isn't in the allowed
    // set -- e.g. it belongs to someone else's upload, or a different
    // post. This is the actual fix: a well-formed /attachments/{uuid} URL
    // is necessary but not sufficient to render.
    final String html = renderer.render("![shot](" + ATTACHMENT + ")", NO_ATTACHMENTS);

    assertThat(html).doesNotContain("<img").contains("shot");
  }

  @Test
  void extractsReferencedAttachmentIds() {
    final String markdown = "![a](" + ATTACHMENT + ") and ![b](https://evil.example/x.png)";

    assertThat(renderer.referencedAttachments(markdown)).containsExactly(ATTACHMENT_ID);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "<script>alert(1)</script>",
      "<img src=x onerror=alert(1)>",
      "<a href=\"javascript:alert(1)\">x</a>",
      "[x](javascript:alert(1))",
      "[x](JaVaScRiPt:alert(1))",
      "[x](data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==)",
      "[x](vbscript:msgbox(1))",
      "![x](https://tracker.example/pixel.png)",
      "![x](data:image/png;base64,iVBORw0KGgo=)",
      "![x](/attachments/../../etc/passwd)",
      "<iframe src=\"https://evil.example\"></iframe>",
      "<svg onload=alert(1)>",
      "<!-- <script>alert(1)</script> -->",
      "<style>body{display:none}</style>",
      "<div style=\"background:url(javascript:alert(1))\">x</div>"
  })
  void neutralizesXssPayloads(final String payload) {
    final String html = renderer.render(payload, NO_ATTACHMENTS).toLowerCase();

    assertThat(html)
        .doesNotContain("<script")
        .doesNotContain("<iframe")
        .doesNotContain("<svg")
        .doesNotContain("<style")
        .doesNotContain("<!--")
        .doesNotContain("href=\"javascript")
        .doesNotContain("href=\"data:")
        .doesNotContain("href=\"vbscript")
        .doesNotContain("<img")
        .doesNotContain("style=");
    assertThat(html).doesNotContainPattern("<[^>]*\\son[a-z]+\\s*=");
  }

  @Test
  void blankInputRendersEmpty() {
    assertThat(renderer.render("", NO_ATTACHMENTS)).isEmpty();
  }
}
