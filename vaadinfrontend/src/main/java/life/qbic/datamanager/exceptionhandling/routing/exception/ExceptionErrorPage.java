package life.qbic.datamanager.exceptionhandling.routing.exception;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import life.qbic.datamanager.exceptionhandling.ErrorMessageTranslationService;
import life.qbic.datamanager.exceptionhandling.ErrorMessageTranslationService.UserFriendlyErrorMessage;
import life.qbic.datamanager.exceptionhandling.routing.ErrorPage;
import life.qbic.logging.api.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b>short description</b>
 * TODO
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@AnonymousAllowed
public class ExceptionErrorPage extends VerticalLayout implements ErrorPage<Exception> {

  private final ErrorMessageTranslationService errorMessageTranslationService;
  private static final Logger log = logger(ExceptionErrorPage.class);

  private final Span message;
  private final H3 title;

  public ExceptionErrorPage(
      @Autowired ErrorMessageTranslationService errorMessageTranslationService) {
    this.errorMessageTranslationService = errorMessageTranslationService;
    message = new Span();
    title = new H3();
    add(title, message);
  }

  @Override
  public void showError(Exception error) {
    UserFriendlyErrorMessage message = errorMessageTranslationService.translate(error);
    showUserFriendlyMessage(message);
  }

  @Override
  public void logError(Exception error) {
    log.error(error.getMessage(), error);
  }


  private void showUserFriendlyMessage(UserFriendlyErrorMessage message) {
    this.title.setText(message.title());
    this.message.setText(message.message());
  }

  @Override
  public int getStatusCode() {
    return HttpStatusCode.INTERNAL_SERVER_ERROR.getCode();
  }
}
