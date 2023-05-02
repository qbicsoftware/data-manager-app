package life.qbic.datamanager.exceptionhandlers;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ErrorEvent;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.exceptionhandlers.ErrorMessageTranslationService.UserFriendlyErrorMessage;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.logging.api.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CustomErrorHandler implements UiAwareErrorHandler {

  private static final Logger log = logger(CustomErrorHandler.class);
  private final ErrorMessageTranslationService userMessageService;

  public CustomErrorHandler(
      @Autowired ErrorMessageTranslationService userMessageService) {
    this.userMessageService = userMessageService;
  }

  /**
   * Vaadin Bug forces us to make the error handler ui aware
   * https://github.com/vaadin/flow/issues/10533
   *
   * @param errorEvent the error event
   * @param ui         the UI
   */
  @Override
  public void error(ErrorEvent errorEvent, UI ui) {
    var throwable = errorEvent.getThrowable();
    log.error(throwable.getMessage(), throwable);
    ApplicationException applicationException = ApplicationException.wrapping(throwable);
    displayUserFriendlyMessage(ui, applicationException);
  }

  private void displayUserFriendlyMessage(UI ui, ApplicationException exception) {
    requireNonNull(ui);
    requireNonNull(exception);

    UserFriendlyErrorMessage errorMessage = userMessageService.getUserFriendlyMessage(exception,
        ui.getLocale());
    ui.access(() -> showErrorDialog(errorMessage));
  }

  private void showErrorDialog(UserFriendlyErrorMessage userFriendlyError) {
    ErrorMessage errorMessage = new ErrorMessage(userFriendlyError.title(),
        userFriendlyError.message());
    StyledNotification styledNotification = new StyledNotification(errorMessage);
    styledNotification.open();
  }
}
