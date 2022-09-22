package life.qbic.datamanager.exceptionhandlers;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandler;
import java.util.Locale;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.datamanager.views.components.ErrorMessage;
import life.qbic.logging.api.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

public class CustomErrorHandler implements ErrorHandler,
    ApplicationExceptionHandler {

  private static final Logger log = logger(CustomErrorHandler.class);

  private final MessageSource messageSource;
  private static final String MESSAGE_SEPARATOR = "->";


  public CustomErrorHandler(@Autowired MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  private record UserFriendlyErrorMessage(String title, String message) {

  }


  @Override
  public void error(ErrorEvent errorEvent) {
    var throwable = errorEvent.getThrowable();
    ApplicationException applicationException = new ApplicationException(ErrorCode.GENERAL,
        ErrorParameters.create()) {
    };
    if (throwable instanceof ApplicationException) {
      applicationException = (ApplicationException) throwable;
    } else {
      log.error(throwable.getMessage(), throwable);
    }
    handle(UI.getCurrent(), applicationException);
  }

  private UserFriendlyErrorMessage getUserFriendlyMessage(ApplicationException applicationException,
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

  private void showErrorDialog(UserFriendlyErrorMessage userFriendlyError) {
    ErrorMessage errorMessage = new ErrorMessage(userFriendlyError.title(),
        userFriendlyError.message());
    Notification notification = new Notification(errorMessage);
    notification.setDuration(2000);
    notification.open();
  }

  @Override
  public void handle(UI ui, ApplicationException exception) {
    requireNonNull(ui);
    requireNonNull(exception);

    UserFriendlyErrorMessage errorMessage = getUserFriendlyMessage(exception, ui.getLocale());
    UI.getCurrent().access(() -> showErrorDialog(errorMessage));
  }
}
