package life.qbic.datamanager.exceptionhandlers;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.datamanager.exceptionhandlers.ExceptionMessageTranslationService.UserFriendlyErrorMessage;
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
@ParentLayout(ExceptionParentLayout.class)
public class CustomErrorView extends VerticalLayout implements HasErrorParameter<Exception> {

  private final ExceptionMessageTranslationService exceptionMessageTranslationService;
  private static final Logger log = logger(CustomErrorView.class);

  private final Span message;
  private final H3 title;

  public CustomErrorView(
      @Autowired ExceptionMessageTranslationService exceptionMessageTranslationService) {
    this.exceptionMessageTranslationService = exceptionMessageTranslationService;
    message = new Span();
    title = new H3();
    add(title, message);
  }

  @Override
  public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<Exception> parameter) {
    var throwable = parameter.getException();
    ApplicationException applicationException = new ApplicationException(ErrorCode.GENERAL,
        ErrorParameters.create()) {
    };
    if (throwable instanceof ApplicationException) {
      applicationException = (ApplicationException) throwable;
    }
    log.error(throwable.getMessage(), throwable);
    showUserFriendlyMessage(applicationException);
    return HttpStatusCode.INTERNAL_SERVER_ERROR.getCode();
  }

  private void showUserFriendlyMessage(ApplicationException applicationException) {
    UserFriendlyErrorMessage userFriendlyMessage = exceptionMessageTranslationService.getUserFriendlyMessage(
        applicationException, getLocale());
    title.setText(userFriendlyMessage.title());
    message.setText(userFriendlyMessage.message());
  }
}
