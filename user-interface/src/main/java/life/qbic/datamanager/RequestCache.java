package life.qbic.datamanager;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.SpringComponent;
import java.util.Optional;
import life.qbic.projectmanagement.application.api.AsyncProjectService.CacheableRequest;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@SpringComponent
public class RequestCache {

  public void addRequest(CacheableRequest request) throws CacheException{
    VaadinSession session = VaadinSession.getCurrent();
    if (session != null) {
      session.setAttribute(CacheableRequest.class.getName(), request);
      return;
    }
    throw new CacheException("No Vaadin session found");
  }

  public Optional<CacheableRequest> get() throws CacheException {
    VaadinSession session = VaadinSession.getCurrent();
    if (session != null) {
      return Optional.ofNullable((CacheableRequest) session.getAttribute(CacheableRequest.class.getName()));
    }
    throw new CacheException("No Vaadin session found");
  }

  public static class CacheException extends RuntimeException {
    public CacheException(String message) {
      super(message);
    }
  }

}
