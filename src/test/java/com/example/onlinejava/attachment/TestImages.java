package com.example.onlinejava.attachment;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;

/**
 * Builds image fixtures in memory so tests don't depend on binary files.
 */
final class TestImages {

  private TestImages() {
  }

  static byte[] png(final int width, final int height) {
    return encode(image(width, height, BufferedImage.TYPE_INT_ARGB), "png");
  }

  static byte[] jpeg(final int width, final int height) {
    return encode(image(width, height, BufferedImage.TYPE_INT_RGB), "jpeg");
  }

  /**
   * Returns a JPEG with an extra APP1 "Exif" segment holding {@code secret},
   * like the GPS/camera metadata phones embed in photos.
   */
  static byte[] jpegWithExif(final String secret) {
    final byte[] jpeg = jpeg(20, 20);
    final byte[] payload = ("Exif\0\0" + secret).getBytes(StandardCharsets.ISO_8859_1);
    final int length = payload.length + 2;
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.write(jpeg, 0, 2);                       // SOI marker
    out.write(0xFF);
    out.write(0xE1);                             // APP1 marker
    out.write(length >> 8);
    out.write(length & 0xFF);
    out.write(payload, 0, payload.length);
    out.write(jpeg, 2, jpeg.length - 2);         // rest of the original file
    return out.toByteArray();
  }

  /** A valid PNG with an HTML/JS payload appended after the image data. */
  static byte[] pngPolyglot(final String trailer) {
    final byte[] png = png(10, 10);
    final byte[] extra = trailer.getBytes(StandardCharsets.UTF_8);
    final byte[] result = new byte[png.length + extra.length];
    System.arraycopy(png, 0, result, 0, png.length);
    System.arraycopy(extra, 0, result, png.length, extra.length);
    return result;
  }

  private static BufferedImage image(final int width, final int height, final int type) {
    final BufferedImage image = new BufferedImage(width, height, type);
    final Graphics2D graphics = image.createGraphics();
    graphics.setColor(Color.ORANGE);
    graphics.fillRect(0, 0, width, height);
    graphics.dispose();
    return image;
  }

  private static byte[] encode(final BufferedImage image, final String format) {
    try {
      final ByteArrayOutputStream out = new ByteArrayOutputStream();
      ImageIO.write(image, format, out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
