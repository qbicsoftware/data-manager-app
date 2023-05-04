package life.qbic.datamanager.exceptionhandling.routing;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public interface ErrorPage<E extends Exception> extends HasErrorParameter<E> {

  void showError(E error);

  void logError(E error);

  int getStatusCode();

  @Override
  default int setErrorParameter(BeforeEnterEvent event, ErrorParameter<E> parameter) {
    E exception = parameter.getException();
    logError(exception);
    showError(exception);
    return getStatusCode();
  }
}
