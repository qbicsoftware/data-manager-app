package life.qbic.datamanager.security;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.server.HttpStatusCode;
import java.util.Locale;
import life.qbic.datamanager.exceptionhandling.routing.ErrorPage;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class AuthorizationErrorPage implements
    ErrorPage<AuthorizationException> {

  @Override
  public void showError(AuthorizationException error, Locale locale) {
    ErrorPage.super.showError(error, locale);
  }

  @Override
  public void logError(AuthorizationException error) {
    ErrorPage.super.logError(error);
  }

  @Override
  public int getStatusCode() {
    return HttpStatusCode.BAD_REQUEST.getCode();
  }

  @Override
  public int setErrorParameter(BeforeEnterEvent event,
      ErrorParameter<AuthorizationException> parameter) {
    return ErrorPage.super.setErrorParameter(event, parameter);
  }
}
