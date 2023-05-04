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
 * <b>short description</b>
 * TODO
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Service
public class ErrorMessageTranslationService {

  private final MessageSource messageSource;
  private static final String MESSAGE_SEPARATOR = "->";

  public record UserFriendlyErrorMessage(String title, String message) {

  }

  public ErrorMessageTranslationService(
      @Autowired MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  public <E extends Exception> UserFriendlyErrorMessage translate(E e) {
    return translate(e, Locale.ENGLISH);
  }

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
