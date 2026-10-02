package life.qbic.datamanager.profilepicture;

import life.qbic.application.commons.ApplicationException;

/**
 * Maps a profile-picture {@link ApplicationException} to a user-facing message.
 *
 * <p>Validation failures carry the {@link ImageNormalizationException.Reason} name as their single
 * error parameter, which is translated here. Debug messages on the exception are never shown.</p>
 *
 * @since 1.22.0
 */
public final class ProfilePictureMessages {

  private ProfilePictureMessages() {
  }

  /**
   * @param error the application error returned by {@link ProfilePictureService}
   * @return a user-facing message
   */
  public static String userMessage(ApplicationException error) {
    Object[] parameters = error.errorParameters().value();
    String reason = parameters.length > 0 ? String.valueOf(parameters[0]) : "";
    if ("ACCESS_DENIED".equals(error.errorCode().name())) {
      return "You are not allowed to change this picture.";
    }
    return userMessageForReason(reason);
  }

  /**
   * Maps a validation reason (from the client-side checks or the server-side
   * {@link ImageNormalizationException.Reason}) to a user-facing message. Unknown reasons fall back
   * to a generic message.
   *
   * @param reason the reason name, or {@code null}/blank for the generic message
   * @return a user-facing message
   */
  public static String userMessageForReason(String reason) {
    if (reason == null) {
      return GENERIC;
    }
    return switch (reason) {
      case "TOO_LARGE" -> "The image is too large. The maximum size is 1 MB.";
      case "UNSUPPORTED_FORMAT" -> "Unsupported file type. Please use a PNG or JPEG image.";
      case "ANIMATED" -> "Animated images are not supported. Please use a static PNG or JPEG.";
      case "TOO_MANY_PIXELS"
          -> "The image resolution is too high. Please use an image with at most 16 megapixels.";
      default -> GENERIC;
    };
  }

  private static final String GENERIC =
      "The image could not be processed. Please choose another one.";
}
