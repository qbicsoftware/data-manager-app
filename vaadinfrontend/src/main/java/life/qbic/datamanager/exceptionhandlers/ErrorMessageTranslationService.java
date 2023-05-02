package life.qbic.datamanager.exceptionhandlers;

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

  public UserFriendlyErrorMessage getUserFriendlyMessage(ApplicationException applicationException,
      Locale locale) {
    return getUserFriendlyMessage(applicationException.errorCode(),
        applicationException.errorParameters(), locale);
  }

  public UserFriendlyErrorMessage getUserFriendlyMessage(ErrorCode errorCode,
      ErrorParameters errorParameters, Locale locale) {

    Optional<UserFriendlyErrorMessage> userFriendlyMessage = getUserFriendlyMessage(
        errorCode.name(),
        errorParameters.value(), locale);
    String message;
    return userFriendlyMessage.orElse(new UserFriendlyErrorMessage(
        messageSource.getMessage(ErrorCode.GENERAL.name(), new Object[]{}, locale), ""));
  }

  public Optional<UserFriendlyErrorMessage> getUserFriendlyMessage(String errorCode,
      Object[] params,
      Locale locale) {
    Optional<String> message = Optional.ofNullable(
        messageSource.getMessage(errorCode, params, null, locale));
    return message
        .map(m -> m.split(MESSAGE_SEPARATOR))
        .map(tuple -> new UserFriendlyErrorMessage(tuple[0].strip(),
            tuple.length > 1 ? tuple[1].strip() : ""));
  }

  public UserFriendlyErrorMessage getDefaultMessage(Locale locale) {
    Result<UserFriendlyErrorMessage, Object> message = Result.fromValue(
            messageSource.getMessage(ErrorCode.GENERAL.name(), new Object[]{}, locale))
        .map(m -> m.split(MESSAGE_SEPARATOR))
        .map(tuple -> new UserFriendlyErrorMessage(tuple[0].strip(),
            tuple.length > 1 ? tuple[1].strip() : ""));
    return message.getValue();
  }
}
