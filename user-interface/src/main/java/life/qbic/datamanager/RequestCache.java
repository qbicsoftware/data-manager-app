package life.qbic.datamanager;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.SpringComponent;
import java.util.Optional;
import life.qbic.projectmanagement.application.api.AsyncProjectService.CacheableRequest;

/**
 * A cache for {@link CacheableRequest} that sets the most recent request as attribute in the
 * current {@link VaadinSession}, if available.
 *
 * @since 1.9.0
 */
@SpringComponent
public class RequestCache {

  /**
   * Stores a {@link CacheableRequest} in the current {@link VaadinSession}.
   * <p>
   * This method overwrites an existing cache entry of the same class or creates a new one, if none
   * is present.
   *
   * @param request the request to store in the cache
   * @throws CacheException if there is no VaadinSession available
   *                        ({@link VaadinSession#getSession()} returned <code>null</code>)
   * @since 1.9.0
   */
  public void store(CacheableRequest request) throws CacheException {
    VaadinSession session = VaadinSession.getCurrent();
    if (session != null) {
      session.setAttribute(CacheableRequest.class.getName(), request);
      return;
    }
    throw new CacheException("No Vaadin session found");
  }

  /**
   * Retrieves the stored request in the cache associated with the current Vaadin session.
   *
   * @return the {@link CacheableRequest} wrapped in an {@link Optional} if available.
   * @throws CacheException if there is no VaadinSession available *
   *                        ({@link VaadinSession#getSession()} returned <code>null</code>)
   * @since 1.9.0
   */
  public Optional<CacheableRequest> get() throws CacheException {
    VaadinSession session = VaadinSession.getCurrent();
    if (session != null) {
      return Optional.ofNullable(
          (CacheableRequest) session.getAttribute(CacheableRequest.class.getName()));
    }
    throw new CacheException("No Vaadin session found");
  }

  public static class CacheException extends RuntimeException {

    public CacheException(String message) {
      super(message);
    }
  }

}
