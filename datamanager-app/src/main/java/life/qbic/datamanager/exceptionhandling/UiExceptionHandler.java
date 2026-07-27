package life.qbic.datamanager.exceptionhandling;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ErrorEvent;
import java.net.InetAddress;
import java.net.UnknownHostException;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.exceptionhandling.ErrorMessageTranslationService.UserFriendlyErrorMessage;
import life.qbic.datamanager.views.general.dialog.AlertDialog;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.associated_dataset.AssociatedDatasetServiceException;
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
    try {
      String hostAddress = InetAddress.getLocalHost().getHostAddress();
      log.error("[%s]".formatted(hostAddress) + throwable.getMessage(), throwable);
    } catch (UnknownHostException ignored) {
      log.error(throwable.getMessage(), throwable);
    }

    // Service-level exceptions (e.g. from AssociatedDatasetService) carry
    // a user-friendly message that has already been translated at the
    // application-layer boundary — no infrastructure details leak through.
    // We surface that message directly so the user sees something meaningful
    // instead of the generic "Something went wrong" fallback.
    if (throwable instanceof AssociatedDatasetServiceException serviceException) {
      var friendly = new UserFriendlyErrorMessage(
          "Operation failed",
          serviceException.userMessage());
      displayUserFriendlyMessage(ui, friendly);
      return;
    }

    ApplicationException applicationException = ApplicationException.wrapping(throwable);
    displayUserFriendlyMessage(ui, applicationException);
  }

  private void displayUserFriendlyMessage(UI ui, ApplicationException exception) {
    requireNonNull(ui, "ui must not be null");
    requireNonNull(exception, "exception must not be null");
    if (!isUiReady(ui)) {
      return;
    }
    UserFriendlyErrorMessage errorMessage = userMessageService.translate(exception, ui.getLocale());
    ui.access(() -> showErrorDialog(errorMessage));
  }

  private void displayUserFriendlyMessage(UI ui, UserFriendlyErrorMessage message) {
    requireNonNull(ui, "ui must not be null");
    requireNonNull(message, "message must not be null");
    if (!isUiReady(ui)) {
      return;
    }
    ui.access(() -> showErrorDialog(message));
  }

  /**
   * Verifies the UI is still attached and not closing. Returns
   * {@code false} (and logs the situation) when the UI cannot receive
   * a new message, so callers can simply {@code return} after invoking it.
   */
  private boolean isUiReady(UI ui) {
    if (ui.isClosing()) {
      if (nonNull(ui.getSession())) {
        log.error(
            "tried to show message on closing UI ui[%s] vaadin[%s] http[%s]".formatted(ui.getUIId(),
                ui.getSession().getPushId(), ui.getSession().getSession().getId()));
      } else {
        log.error(
            "tried to show message on closing UI ui[%s] session is null".formatted(ui.getUIId()));
      }
      return false;
    }
    if (!ui.isAttached()) {
      if (nonNull(ui.getSession())) {
        log.error(
            "tried to show message on detached UI ui[%s] vaadin[%s] http[%s]".formatted(
                ui.getUIId(),
                ui.getSession().getPushId(), ui.getSession().getSession().getId()));
      } else {
        log.error(
            "tried to show message on detached UI ui[%s] session is null".formatted(
                ui.getUIId()));
      }
      return false;
    }
    return true;
  }

  private void showErrorDialog(UserFriendlyErrorMessage userFriendlyError) {
    AlertDialog.alert(UI.getCurrent())
        .error()
        .title(userFriendlyError.title())
        .message(userFriendlyError.message())
        .confirmButton("Okay", () -> {})
        .build()
        .open();
  }
}
