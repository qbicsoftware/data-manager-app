package life.qbic.datamanager.exceptionhandling;

import java.util.Locale;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

/**
 * Translates an exception into a user-friendly message.
 * <p>
 * For ApplicationExceptions, messages can be defined in error-messages.properties, other exceptions will
 * be mapped to the default message.
 *
 * @since 1.0.0
 */
@Service
public class ErrorMessageTranslationService {

  private final MessageSource messageSource;
  private static final String MESSAGE_SEPARATOR = "->";

  /**
   * A user friendly error message. The error message has a title and a message detailing the error.
   * @param title
   * @param message
   */
  public record UserFriendlyErrorMessage(String title, String message) {

  }

  public ErrorMessageTranslationService(
      @Autowired MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  /**
   * Translates an exception into a user-friendly error message. English language is assumed.
   * @param e the exception to translate
   * @return a {@link UserFriendlyErrorMessage}
   * @param <E> the class of the exception
   */
  public <E extends Exception> UserFriendlyErrorMessage translate(E e) {
    return translate(e, Locale.ENGLISH);
  }

  /**
   *
   * Translates an exception into a user-friendly error message.
   * @param e the exception to translate
   * @param locale the locale for which to provide the message
   * @return a {@link UserFriendlyErrorMessage}
   * @param <E> the class of the exception

   */
  public <E extends Exception> UserFriendlyErrorMessage translate(E e, Locale locale) {
    ApplicationException applicationException = ApplicationException.wrapping(e);
    return getUserFriendlyMessage(applicationException.errorCode(),
        applicationException.errorParameters(), locale);
  }


  private UserFriendlyErrorMessage getUserFriendlyMessage(ErrorCode errorCode,
      ErrorParameters errorParameters, Locale locale) {

    Optional<String> configuredMessage = Optional.ofNullable(
        messageSource.getMessage(errorCode.name(), errorParameters.value(), null, locale));

    return configuredMessage
        .map(this::parseMessage)
        .orElse(getDefaultMessage(locale));
  }

  private UserFriendlyErrorMessage getDefaultMessage(Locale locale) {
    Result<UserFriendlyErrorMessage, Void> message = Result.<String, Void>fromValue(
            messageSource.getMessage(ErrorCode.GENERAL.name(), new Object[]{}, locale))
        .map(this::parseMessage);
    return message.getValue();
  }

  private UserFriendlyErrorMessage parseMessage(String message) {
    String[] tuple = message.split(MESSAGE_SEPARATOR);
    String title = tuple[0];
    String body = tuple.length > 1 ? tuple[1] : "";
    return new UserFriendlyErrorMessage(title, body);
  }
}
