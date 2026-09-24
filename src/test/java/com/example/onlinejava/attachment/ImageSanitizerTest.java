package com.example.onlinejava.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for {@link ImageSanitizer}.
 */
class ImageSanitizerTest {

  private final ImageSanitizer sanitizer = new ImageSanitizer();

  @Test
  void reencodesPngAsPng() throws IOException {
    final ImageSanitizer.SanitizedImage result = sanitizer.sanitize(TestImages.png(30, 20));

    assertThat(result.contentType()).isEqualTo("image/png");
    assertThat(result.width()).isEqualTo(30);
    assertThat(result.height()).isEqualTo(20);
    assertThat(ImageIO.read(new ByteArrayInputStream(result.data())).getWidth()).isEqualTo(30);
  }

  @Test
  void reencodesJpegAsJpeg() {
    assertThat(sanitizer.sanitize(TestImages.jpeg(10, 10)).contentType())
        .isEqualTo("image/jpeg");
  }

  @Test
  void stripsExifMetadata() {
    final byte[] input = TestImages.jpegWithExif("GPS-SECRET-LOCATION");
    assertThat(latin1(input)).contains("GPS-SECRET-LOCATION");

    final byte[] output = sanitizer.sanitize(input).data();

    assertThat(latin1(output)).doesNotContain("GPS-SECRET-LOCATION").doesNotContain("Exif");
  }

  @Test
  void dropsDataAppendedAfterTheImage() {
    final byte[] output = sanitizer.sanitize(
        TestImages.pngPolyglot("<script>alert(1)</script>")).data();

    assertThat(latin1(output)).doesNotContain("<script>");
  }

  @Test
  void rejectsNonImages() {
    assertBadRequest("<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8));
    assertBadRequest(new byte[0]);
  }

  @Test
  void rejectsGifJavascriptPolyglot() {
    assertBadRequest("GIF89a/*\u0000\u0000*/=alert(1);".getBytes(StandardCharsets.ISO_8859_1));
  }

  @Test
  void rejectsOversizedDimensionsBeforeDecoding() {
    assertBadRequest(TestImages.png(ImageSanitizer.MAX_DIMENSION + 1, 1));
  }

  private void assertBadRequest(final byte[] data) {
    assertThatThrownBy(() -> sanitizer.sanitize(data))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  private static String latin1(final byte[] data) {
    return new String(data, StandardCharsets.ISO_8859_1);
  }
}
