package life.qbic.datamanager.exceptionhandlers;

import java.util.Locale;
import life.qbic.application.commons.ApplicationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

/**
 * <b>short description</b>
 * TODO
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Service
public class ExceptionMessageTranslationService {

  private final MessageSource messageSource;
  private static final String MESSAGE_SEPARATOR = "->";

  public record UserFriendlyErrorMessage(String title, String message) {

  }

  public ExceptionMessageTranslationService(
      @Autowired MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  public UserFriendlyErrorMessage getUserFriendlyMessage(
      ApplicationException applicationException,
      Locale locale) {
    String message = messageSource.getMessage(applicationException.errorCode().name(),
        applicationException.errorParameters().value(), locale);
    if (message.contains(MESSAGE_SEPARATOR)) {
      String[] split = message.split(MESSAGE_SEPARATOR);
      return new UserFriendlyErrorMessage(split[0].strip(), split[1].strip());
    } else {
      return new UserFriendlyErrorMessage(message, "");
    }
  }
}
