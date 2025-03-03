package life.qbic.datamanager;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.spring.annotation.SpringComponent;
import java.util.LinkedList;
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

  private static final int CACHE_SIZE = 10;

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
      if (session.getAttribute(RequestCache.class.getName()) == null) {
        session.setAttribute(RequestCache.class.getName(), new LinkedList<CacheableRequest>());
      }
      var entry = session.getAttribute(RequestCache.class.getName());
      if (entry instanceof LinkedList) {
        var list = (LinkedList<CacheableRequest>) entry;
        store(list, request);
      }
      return;
    }
    throw new CacheException("No Vaadin session found");
  }


  private static void store(LinkedList<CacheableRequest> list, CacheableRequest request) {
    if (list.size() == CACHE_SIZE) {
      list.removeFirst();
    }
    list.add(request);
  }

  /**
   * Retrieves the stored request in the cache associated with the current Vaadin session.
   *
   * @return the {@link CacheableRequest} wrapped in an {@link Optional} if available.
   * @throws CacheException if there is no VaadinSession available *
   *                        ({@link VaadinSession#getSession()} returned <code>null</code>)
   * @since 1.9.0
   */
  public Optional<CacheableRequest> get(String id) throws CacheException {
    VaadinSession session = VaadinSession.getCurrent();
    if (session != null) {
      var cache = session.getAttribute(RequestCache.class.getName());
      if (cache == null) {
        return Optional.empty();
      }
      if (cache instanceof LinkedList) {
        return Optional.ofNullable(findRequest((LinkedList<CacheableRequest>) cache, id));
      }
      throw new CacheException("Unknown cache type");
    }
    throw new CacheException("No Vaadin session found");
  }

  private CacheableRequest findRequest(LinkedList<CacheableRequest> cache, String id) {
    for (CacheableRequest request : cache) {
      if (request.requestId().equals(id)) {
        return request;
      }
    }
    return null;
  }

  public static class CacheException extends RuntimeException {

    public CacheException(String message) {
      super(message);
    }
  }

}
