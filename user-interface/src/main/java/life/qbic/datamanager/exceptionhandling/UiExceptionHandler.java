package life.qbic.datamanager.exceptionhandling;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.server.ErrorEvent;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.exceptionhandling.ErrorMessageTranslationService.UserFriendlyErrorMessage;
import life.qbic.datamanager.views.notifications.NotificationDialog;
import life.qbic.logging.api.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The exception handler intended to be hooked into every vaadin ui.
 * <p>
 * This exception handler acts as a global fallback exception handler. It is registered to every
 * {@link UI} that is instantiated. When an exception is thrown inside the UI, this exception
 * handler
 * <ol>
 *   <li>catches the exception</li>
 *   <li>translates the exception into a helpful message (if possible)</li>
 *   <li>shows a notification on the ui with the message</li>
 * </ol>
 */
@Component
public class UiExceptionHandler {

  private static final Logger log = logger(UiExceptionHandler.class);
  private final ErrorMessageTranslationService userMessageService;

  public UiExceptionHandler(
      @Autowired ErrorMessageTranslationService userMessageService) {
    this.userMessageService = userMessageService;
  }

  /**
   * Vaadin Bug forces us to make the error handler ui aware
   * <a href="https://github.com/vaadin/flow/issues/10533">Vaadin Issue 10533</a>
   *
   * @param errorEvent the error event
   * @param ui         the UI
   */
  public void error(ErrorEvent errorEvent, UI ui) {
    var throwable = errorEvent.getThrowable();
    log.error(throwable.getMessage(), throwable);
    ApplicationException applicationException = ApplicationException.wrapping(throwable);
    displayUserFriendlyMessage(ui, applicationException);
  }

  private void displayUserFriendlyMessage(UI ui, ApplicationException exception) {
    requireNonNull(ui, "ui must not be null");
    requireNonNull(exception, "exception must not be null");
    UserFriendlyErrorMessage errorMessage = userMessageService.translate(exception, ui.getLocale());
    if (ui.isClosing()) {
      if (nonNull(ui.getSession())) {
        log.error(
            "tried to show message on closing UI ui[%s] vaadin[%s] http[%s]: %s;%s".formatted(
                ui.getUIId(),
                ui.getSession().getPushId(), ui.getSession().getSession().getId(),
                errorMessage.title(), errorMessage.message()));
      } else {
        log.error(
            "tried to show message on closing UI ui[%s] session is null".formatted(ui.getUIId()));
      }

      return;
    }
    if (!ui.isAttached()) {
      if (nonNull(ui.getSession())) {
        log.error(
            "tried to show message on detached UI ui[%s] vaadin[%s] http[%s]: %s;%s".formatted(
                ui.getUIId(),
                ui.getSession().getPushId(), ui.getSession().getSession().getId(), errorMessage.title(), errorMessage.message()));
      } else {
        log.error(
            "tried to show message on detached UI ui[%s] session is null: %s;%s".formatted(
                ui.getUIId(), errorMessage.title(), errorMessage.message()));
      }
      return;
    }
    NotificationDialog dialog = NotificationDialog.errorDialog();
    dialog.setTitle(errorMessage.title());
    dialog.setContent(new Span(errorMessage.message()));
    dialog.open();
  }
}
