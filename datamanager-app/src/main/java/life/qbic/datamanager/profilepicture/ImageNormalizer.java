package life.qbic.datamanager.profilepicture;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Validates and normalizes an uploaded profile picture into a square PNG derivative.
 *
 * <p><b>Never trust the client.</b> The declared content type is ignored; the format is determined
 * by magic-byte sniffing. The original upload is discarded (see the implementation plan
 * {@code docs/plans/FEAT-PROFILE-PICTURES-implementation-plan.md}) and only the square
 * {@link #TARGET_SIZE}×{@link #TARGET_SIZE} PNG is returned. Metadata (including EXIF) is dropped
 * by re-encoding: the derivative is drawn onto a fresh image, so no source metadata survives.</p>
 *
 * <p>Guards applied, in order:</p>
 * <ol>
 *   <li>size limit {@link #MAX_UPLOAD_BYTES};</li>
 *   <li>format allowlist by magic bytes (PNG or JPEG only);</li>
 *   <li>animated content rejection (PNG {@code acTL} chunk, or more than one frame);</li>
 *   <li>decompression-bomb guard: {@link #MAX_DECODED_PIXELS} before any scaling;</li>
 *   <li>crop-frame bounds validation.</li>
 * </ol>
 *
 * <p>This class is stateless and thread-safe. It is not a Spring bean; callers construct it
 * directly or receive it through the application service.</p>
 *
 * @since 1.19.0
 */
public final class ImageNormalizer {

  /** Maximum accepted upload size: 1 MiB. */
  public static final long MAX_UPLOAD_BYTES = 1024L * 1024L;

  /** Edge length of the stored square derivative, in pixels. */
  public static final int TARGET_SIZE = 256;

  /** Content type of every stored derivative. */
  public static final String PNG_CONTENT_TYPE = "image/png";

  /** Upper bound on decoded pixels, to reject decompression bombs. */
  public static final long MAX_DECODED_PIXELS = 16_000_000L;

  private static final byte[] PNG_MAGIC =
      {(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
  private static final byte[] PNG_ACTL = "acTL".getBytes(StandardCharsets.US_ASCII);

  static {
    // Keep decoding in memory; uploads are at most 1 MiB.
    ImageIO.setUseCache(false);
  }

  /**
   * Normalizes an upload by center-cropping the largest centered square.
   *
   * @param raw the uploaded bytes; must not be {@code null}
   * @return the normalized square PNG derivative
   * @throws ImageNormalizationException if the upload is rejected for any of the guarded reasons
   */
  public NormalizedImage normalize(byte[] raw) {
    return normalize(raw, null);
  }

  /**
   * Normalizes an upload using an optional explicit square crop frame.
   *
   * @param raw       the uploaded bytes; must not be {@code null}
   * @param cropFrame the requested square region in source pixels, or {@code null} to center-crop
   * @return the normalized square PNG derivative
   * @throws ImageNormalizationException if the upload is rejected for any of the guarded reasons
   */
  public NormalizedImage normalize(byte[] raw, CropFrame cropFrame) {
    validateSize(raw);
    validateFormat(raw);
    rejectAnimatedPng(raw);

    BufferedImage source = decode(raw);
    CropRegion region = resolveCrop(source, cropFrame);
    BufferedImage derivative = renderSquare(source, region);
    byte[] png = encodePng(derivative);
    String hash = sha256Hex(png);
    return new NormalizedImage(png, derivative.getWidth(), derivative.getHeight(), hash);
  }

  private static void validateSize(byte[] raw) {
    if (raw == null || raw.length == 0) {
      throw new ImageNormalizationException(ImageNormalizationException.Reason.CORRUPT,
          "empty upload");
    }
    if (raw.length > MAX_UPLOAD_BYTES) {
      throw new ImageNormalizationException(ImageNormalizationException.Reason.TOO_LARGE,
          "upload of " + raw.length + " bytes exceeds " + MAX_UPLOAD_BYTES);
    }
  }

  private static void validateFormat(byte[] raw) {
    if (!isPng(raw) && !isJpeg(raw)) {
      throw new ImageNormalizationException(ImageNormalizationException.Reason.UNSUPPORTED_FORMAT,
          "not a PNG or JPEG by magic bytes");
    }
  }

  private static void rejectAnimatedPng(byte[] raw) {
    if (isPng(raw) && contains(raw, PNG_ACTL)) {
      throw new ImageNormalizationException(ImageNormalizationException.Reason.ANIMATED,
          "PNG contains an acTL chunk (APNG)");
    }
  }

  private static BufferedImage decode(byte[] raw) {
    try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(raw))) {
      if (input == null) {
        throw new ImageNormalizationException(ImageNormalizationException.Reason.CORRUPT,
            "no image input stream");
      }
      Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) {
        throw new ImageNormalizationException(ImageNormalizationException.Reason.CORRUPT,
            "no image reader for the sniffed format");
      }
      ImageReader reader = readers.next();
      try {
        reader.setInput(input, false, true);
        int frameCount = reader.getNumImages(true);
        if (frameCount > 1) {
          throw new ImageNormalizationException(ImageNormalizationException.Reason.ANIMATED,
              "image contains " + frameCount + " frames");
        }
        long width = reader.getWidth(0);
        long height = reader.getHeight(0);
        if (width <= 0 || height <= 0) {
          throw new ImageNormalizationException(ImageNormalizationException.Reason.CORRUPT,
              "image has zero dimension");
        }
        if (width * height > MAX_DECODED_PIXELS) {
          throw new ImageNormalizationException(ImageNormalizationException.Reason.TOO_MANY_PIXELS,
              width + "x" + height + " exceeds " + MAX_DECODED_PIXELS + " pixels");
        }
        BufferedImage image = reader.read(0);
        if (image == null) {
          throw new ImageNormalizationException(ImageNormalizationException.Reason.CORRUPT,
              "reader returned no image");
        }
        return image;
      } finally {
        reader.dispose();
      }
    } catch (IOException e) {
      throw new ImageNormalizationException(ImageNormalizationException.Reason.CORRUPT,
          "failed to decode image", e);
    }
  }

  private static CropRegion resolveCrop(BufferedImage source, CropFrame cropFrame) {
    int sourceWidth = source.getWidth();
    int sourceHeight = source.getHeight();
    if (cropFrame == null) {
      int side = Math.min(sourceWidth, sourceHeight);
      return new CropRegion((sourceWidth - side) / 2, (sourceHeight - side) / 2, side);
    }
    int x = cropFrame.x();
    int y = cropFrame.y();
    int side = cropFrame.size();
    if (x < 0 || y < 0 || side <= 0
        || (long) x + side > sourceWidth || (long) y + side > sourceHeight) {
      throw new ImageNormalizationException(ImageNormalizationException.Reason.INVALID_CROP,
          "crop frame " + cropFrame + " is outside " + sourceWidth + "x" + sourceHeight);
    }
    return new CropRegion(x, y, side);
  }

  private static BufferedImage renderSquare(BufferedImage source, CropRegion region) {
    BufferedImage cropped = source.getSubimage(region.x(), region.y(), region.side(),
        region.side());
    BufferedImage derivative = new BufferedImage(TARGET_SIZE, TARGET_SIZE,
        BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = derivative.createGraphics();
    try {
      graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
          RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      graphics.setRenderingHint(RenderingHints.KEY_RENDERING,
          RenderingHints.VALUE_RENDER_QUALITY);
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.drawImage(cropped, 0, 0, TARGET_SIZE, TARGET_SIZE, null);
    } finally {
      graphics.dispose();
    }
    return derivative;
  }

  private static byte[] encodePng(BufferedImage image) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      if (!ImageIO.write(image, "png", out)) {
        throw new ImageNormalizationException(ImageNormalizationException.Reason.CORRUPT,
            "no PNG writer available");
      }
      return out.toByteArray();
    } catch (IOException e) {
      throw new ImageNormalizationException(ImageNormalizationException.Reason.CORRUPT,
          "failed to encode PNG", e);
    }
  }

  private static String sha256Hex(byte[] bytes) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 is mandated by the JVM specification; this cannot happen on a conformant JVM.
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

  private static boolean isPng(byte[] raw) {
    if (raw.length < PNG_MAGIC.length) {
      return false;
    }
    for (int i = 0; i < PNG_MAGIC.length; i++) {
      if (raw[i] != PNG_MAGIC[i]) {
        return false;
      }
    }
    return true;
  }

  private static boolean isJpeg(byte[] raw) {
    return raw.length >= 3
        && (raw[0] & 0xFF) == 0xFF
        && (raw[1] & 0xFF) == 0xD8
        && (raw[2] & 0xFF) == 0xFF;
  }

  private static boolean contains(byte[] haystack, byte[] needle) {
    outer:
    for (int i = 0; i <= haystack.length - needle.length; i++) {
      for (int j = 0; j < needle.length; j++) {
        if (haystack[i + j] != needle[j]) {
          continue outer;
        }
      }
      return true;
    }
    return false;
  }

  /**
   * An explicit square crop region in source-image pixel coordinates.
   *
   * @param x    left edge, inclusive; {@code >= 0}
   * @param y    top edge, inclusive; {@code >= 0}
   * @param size edge length of the square; positive, and {@code (x+size, y+size)} must be within
   *             the source image
   */
  public record CropFrame(int x, int y, int size) {

  }

  private record CropRegion(int x, int y, int side) {

  }
}
