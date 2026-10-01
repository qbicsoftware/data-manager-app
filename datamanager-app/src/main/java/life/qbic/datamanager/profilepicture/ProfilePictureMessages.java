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
    return switch (reason) {
      case "TOO_LARGE" -> "The image is too large. The maximum size is 1 MB.";
      case "UNSUPPORTED_FORMAT" -> "Only PNG and JPEG images are supported.";
      case "ANIMATED" -> "Animated images are not supported.";
      case "TOO_MANY_PIXELS" -> "The image dimensions are too large.";
      default -> switch (error.errorCode()) {
        case ACCESS_DENIED -> "You are not allowed to change this picture.";
        default -> "The image could not be processed. Please choose another one.";
      };
    };
  }
}
