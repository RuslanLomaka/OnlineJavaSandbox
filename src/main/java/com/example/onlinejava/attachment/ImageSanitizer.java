package com.example.onlinejava.attachment;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates an uploaded image and re-encodes it from decoded pixels.
 *
 * <p>Re-encoding is the key defence: whatever the upload contained besides
 * pixels (EXIF/GPS metadata, appended scripts, polyglot tricks) is not copied
 * to the stored file. The format is detected from the bytes themselves, never
 * from the client's file name or Content-Type, and dimensions are checked from
 * the header before decoding so a tiny file can't claim a huge bitmap
 * ("decompression bomb").
 */
@Component
public class ImageSanitizer {

  /** Maximum width or height in pixels. */
  static final int MAX_DIMENSION = 4096;

  /** Maximum total pixels (a 4K screenshot is about 8.3 million). */
  static final long MAX_PIXELS = 10_000_000L;

  private static final Set<String> ACCEPTED_FORMATS = Set.of("png", "jpeg", "gif", "bmp");

  private static final float JPEG_QUALITY = 0.9f;

  /**
   * A cleaned image ready to store.
   *
   * @param data encoded image bytes
   * @param contentType {@code image/png} or {@code image/jpeg}
   * @param width width in pixels
   * @param height height in pixels
   */
  public record SanitizedImage(byte[] data, String contentType, int width, int height) {
  }

  /**
   * Validates and re-encodes an image. JPEG stays JPEG (photos); everything
   * else becomes PNG (lossless, keeps transparency).
   *
   * @param input uploaded bytes
   * @return the re-encoded image
   * @throws ResponseStatusException 400 if the data is not an acceptable image
   */
  public SanitizedImage sanitize(final byte[] input) {
    try (ImageInputStream stream =
             ImageIO.createImageInputStream(new ByteArrayInputStream(input))) {
      final ImageReader reader = firstReader(stream);
      try {
        final String format = reader.getFormatName().toLowerCase(Locale.ROOT);
        if (!ACCEPTED_FORMATS.contains(format)) {
          throw invalid("Unsupported image format");
        }
        reader.setInput(stream, true, true);
        final int width = reader.getWidth(0);
        final int height = reader.getHeight(0);
        if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION
            || (long) width * height > MAX_PIXELS) {
          throw invalid("Image dimensions must be at most " + MAX_DIMENSION + " pixels");
        }
        final BufferedImage image = reader.read(0);
        return "jpeg".equals(format)
            ? new SanitizedImage(writeJpeg(image), "image/jpeg", width, height)
            : new SanitizedImage(writePng(image), "image/png", width, height);
      } finally {
        reader.dispose();
      }
    } catch (IOException | RuntimeException e) {
      if (e instanceof ResponseStatusException rse) {
        throw rse;
      }
      // Corrupt or hostile files make ImageIO throw all sorts of exceptions.
      throw invalid("Could not read image");
    }
  }

  private static ImageReader firstReader(final ImageInputStream stream) {
    if (stream == null) {
      throw invalid("Not an image");
    }
    final Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
    if (!readers.hasNext()) {
      throw invalid("Not a supported image (use PNG, JPEG or GIF)");
    }
    return readers.next();
  }

  private static byte[] writePng(final BufferedImage image) throws IOException {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    if (!ImageIO.write(image, "png", out)) {
      throw invalid("Could not encode image");
    }
    return out.toByteArray();
  }

  private static byte[] writeJpeg(final BufferedImage source) throws IOException {
    // JPEG has no alpha channel; draw onto a plain RGB canvas first.
    final BufferedImage rgb = new BufferedImage(
        source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
    rgb.createGraphics().drawImage(source, 0, 0, null);

    final ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    try (ImageOutputStream output = ImageIO.createImageOutputStream(out)) {
      writer.setOutput(output);
      final ImageWriteParam params = writer.getDefaultWriteParam();
      params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      params.setCompressionQuality(JPEG_QUALITY);
      // null metadata: nothing from the original file is carried over.
      writer.write(null, new IIOImage(rgb, null, null), params);
    } finally {
      writer.dispose();
    }
    return out.toByteArray();
  }

  private static ResponseStatusException invalid(final String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
