package life.qbic.datamanager.profilepicture;

/**
 * Thrown by {@link ImageNormalizer} when an uploaded image cannot be accepted.
 *
 * <p>This is an expected failure path: the application layer maps the {@link Reason} to a
 * user-facing {@code Result} error. The message is for logs and diagnostics only and is never
 * shown to the user.</p>
 *
 * @since 1.22.0
 */
public class ImageNormalizationException extends RuntimeException {

  /**
   * Why an upload was rejected. Each reason is a stable, user-facing distinguishable case.
   */
  public enum Reason {
    /** The upload exceeds {@link ImageNormalizer#MAX_UPLOAD_BYTES}. */
    TOO_LARGE,
    /** The content is not a PNG or JPEG by magic-byte sniffing. */
    UNSUPPORTED_FORMAT,
    /** The image is animated (contains multiple frames). */
    ANIMATED,
    /** The decoded pixel count exceeds {@link ImageNormalizer#MAX_DECODED_PIXELS}. */
    TOO_MANY_PIXELS,
    /** The bytes could not be decoded as an image. */
    CORRUPT,
    /** The requested crop frame is not a valid region of the source image. */
    INVALID_CROP
  }

  private final Reason reason;

  public ImageNormalizationException(Reason reason, String message) {
    super(message);
    this.reason = reason;
  }

  public ImageNormalizationException(Reason reason, String message, Throwable cause) {
    super(message, cause);
    this.reason = reason;
  }

  public Reason reason() {
    return reason;
  }
}
