package life.qbic.projectmanagement.infrastructure.ontology;

import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Thread-safe implementation of {@link RequestCache}.
 *
 * @since 1.11.0
 */
@Component
public class SynchronizedRequestCache {

  private final RequestCache cache;

  private final Object lock = new Object();

  public SynchronizedRequestCache(@Value("${terminology.service.cache.size}") String cacheSize) {
    cache = new RequestCache(Integer.parseInt(cacheSize));
  }

  /**
   * Adds a {@link TibTerm} to the in-memory cache.
   * <p>
   * If the cache max size is reached, the oldest entry will be replaced with the one passed to the
   * function.
   * <p>
   * This function is thread-safe.
   *
   * @param term the term to store in the cache
   * @since 1.11.0
   */
  public void add(TibTerm term) {
    synchronized (lock) {
      cache.add(term);
    }
  }

  /**
   * Searches for a matching {@link TibTerm} in the cache.
   * <p>
   * This function is thread-safe.
   *
   * @param curie the CURIE to search for
   * @return the search result, {@link Optional#empty()} if no match was found
   * @since 1.11.0
   */
  Optional<TibTerm> findByCurie(String curie) {
    Objects.requireNonNull(curie);
    synchronized (lock) {
      return cache.findByCurie(curie);
    }
  }
}
