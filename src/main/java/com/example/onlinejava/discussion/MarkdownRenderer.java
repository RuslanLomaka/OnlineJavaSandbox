package com.example.onlinejava.discussion;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Image;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

/**
 * Turns user-written Markdown into HTML that is safe to insert into a page.
 *
 * <p>Two independent layers, so a bug in one is caught by the other:
 * <ol>
 *   <li>commonmark renders with raw HTML escaped ({@code escapeHtml}) and
 *       dangerous URL schemes removed ({@code sanitizeUrls}); images that do
 *       not point at this site's own {@code /attachments/{uuid}} are replaced
 *       by their alt text, so posts can't embed tracking pixels.</li>
 *   <li>The result then passes through an OWASP allowlist policy that only
 *       keeps formatting tags and a few vetted attributes.</li>
 * </ol>
 */
@Component
public class MarkdownRenderer {

  /** Matches this site's attachment URLs, capturing the UUID. */
  static final Pattern ATTACHMENT_URL = Pattern.compile(
      "^/attachments/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})$");

  private static final List<Extension> EXTENSIONS = List.of(
      TablesExtension.create(),
      StrikethroughExtension.create(),
      AutolinkExtension.create()
  );

  private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
      .allowElements(
          "p", "br", "hr", "em", "strong", "del", "code", "pre", "blockquote",
          "ul", "ol", "li", "h3", "h4", "h5", "h6",
          "table", "thead", "tbody", "tr", "th", "td", "a", "img")
      .allowAttributes("class")
      .matching(Pattern.compile("language-[A-Za-z0-9+#_-]{1,30}"))
      .onElements("code")
      .allowAttributes("start").matching(Pattern.compile("\\d{1,6}")).onElements("ol")
      .allowAttributes("align").matching(true, "left", "center", "right").onElements("th", "td")
      .allowUrlProtocols("http", "https")
      .allowAttributes("href").onElements("a")
      .allowAttributes("target").matching(true, "_blank").onElements("a")
      .requireRelsOnLinks("nofollow", "noopener", "noreferrer")
      .allowAttributes("src").matching(ATTACHMENT_URL).onElements("img")
      .allowAttributes("alt", "title").onElements("img")
      .allowAttributes("loading").matching(true, "lazy").onElements("img")
      .disallowWithoutAttributes("img")
      .toFactory();

  private final Parser parser = Parser.builder().extensions(EXTENSIONS).build();

  private final HtmlRenderer htmlRenderer = HtmlRenderer.builder()
      .extensions(EXTENSIONS)
      .escapeHtml(true)
      .sanitizeUrls(true)
      .softbreak("<br>")
      .attributeProviderFactory(context -> (node, tagName, attributes) -> {
        if (node instanceof Link) {
          attributes.put("target", "_blank");
        } else if (node instanceof Image) {
          attributes.put("loading", "lazy");
        }
      })
      .build();

  /**
   * Renders Markdown to sanitized HTML.
   *
   * @param markdown user-written Markdown
   * @return HTML safe for {@code innerHTML}
   */
  public String render(final String markdown) {
    final Node document = parser.parse(markdown);
    document.accept(new ForeignImageRemover());
    return POLICY.sanitize(htmlRenderer.render(document)).trim();
  }

  /**
   * Returns the ids of this site's attachments that the Markdown embeds.
   *
   * @param markdown user-written Markdown
   * @return attachment ids in order of first appearance
   */
  public Set<UUID> referencedAttachments(final String markdown) {
    final Set<UUID> ids = new LinkedHashSet<>();
    parser.parse(markdown).accept(new AbstractVisitor() {
      @Override
      public void visit(final Image image) {
        final Matcher matcher = ATTACHMENT_URL.matcher(image.getDestination());
        if (matcher.matches()) {
          ids.add(UUID.fromString(matcher.group(1)));
        }
        visitChildren(image);
      }
    });
    return ids;
  }

  /**
   * Replaces images that don't point at an own attachment with their alt text.
   */
  private static final class ForeignImageRemover extends AbstractVisitor {
    @Override
    public void visit(final Image image) {
      if (!ATTACHMENT_URL.matcher(image.getDestination()).matches()) {
        final StringBuilder alt = new StringBuilder();
        image.accept(new AbstractVisitor() {
          @Override
          public void visit(final Text text) {
            alt.append(text.getLiteral());
          }
        });
        image.insertBefore(new Text(alt.isEmpty() ? "[image]" : alt.toString()));
        image.unlink();
      }
    }
  }
}
