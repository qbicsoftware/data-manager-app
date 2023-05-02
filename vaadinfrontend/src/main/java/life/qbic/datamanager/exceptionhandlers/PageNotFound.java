package life.qbic.datamanager.exceptionhandlers;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import life.qbic.datamanager.exceptionhandlers.ErrorMessageTranslationService.UserFriendlyErrorMessage;
import life.qbic.logging.api.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@AnonymousAllowed
@ParentLayout(ExceptionParentLayout.class)
public class PageNotFound extends VerticalLayout implements HasErrorParameter<NotFoundException> {

  private static final Logger log = logger(PageNotFound.class);

  public PageNotFound(@Autowired ErrorMessageTranslationService errorMessageTranslationService) {
    UserFriendlyErrorMessage notFoundMessage = errorMessageTranslationService.getUserFriendlyMessage(
            "NOT_FOUND", new Object[]{}, getLocale())
        .orElse(errorMessageTranslationService.getDefaultMessage(getLocale()));
    add(new H2(notFoundMessage.title()));
    add(new Span(notFoundMessage.message()));
  }

  @Override
  public int setErrorParameter(BeforeEnterEvent event,
      ErrorParameter<NotFoundException> parameter) {
    log.error(parameter.getCustomMessage(), parameter.getException());
    return HttpStatusCode.NOT_FOUND.getCode();
  }
}
