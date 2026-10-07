package life.qbic.datamanager.profilepicture;

/**
 * A validated, normalized profile picture derivative.
 *
 * <p>Only normalized derivatives are ever persisted: square PNG bytes at
 * {@link ImageNormalizer#TARGET_SIZE} pixels. The {@link #contentHash} is the SHA-256 hex digest
 * of {@link #png} and is used to build the immutable delivery URL and to detect changes.</p>
 *
 * @param png         the normalized PNG bytes; never {@code null} or empty
 * @param width       the derivative width in pixels; always positive
 * @param height      the derivative height in pixels; always positive
 * @param contentHash the lower-case SHA-256 hex digest of {@code png}; never {@code null}
 * @since 1.19.0
 */
public record NormalizedImage(byte[] png, int width, int height, String contentHash) {

  public NormalizedImage {
    if (png == null || png.length == 0) {
      throw new IllegalArgumentException("png must not be null or empty");
    }
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("width and height must be positive");
    }
    if (contentHash == null || contentHash.isBlank()) {
      throw new IllegalArgumentException("contentHash must not be null or blank");
    }
  }
}
