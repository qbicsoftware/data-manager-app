package life.qbic.projectmanagement.infrastructure.ontology;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <b>Request cache</b>
 *
 * <p>In memory look-up cache for {@link TibTerm}.</p>
 * <p>
 * If you need thread-safety, use {@link SynchronizedRequestCache}.
 *
 * @since 1.11.0
 */
public class RequestCache {

  private final List<TibTerm> cache = new ArrayList<>();
  private final int limit;
  private List<CacheEntryStat> accessFrequency = new ArrayList<>();

  RequestCache(int limit) {
    this.limit = limit;
  }

  /**
   * Adds a {@link TibTerm} to the in-memory cache.
   * <p>
   * If the cache max size is reached, the oldest entry will be replaced with the one passed to the
   * function.
   *
   * @param term the term to store in the cache
   * @since 1.9.0
   */
  void add(TibTerm term) {
    if (cache.contains(term)) {
      return;
    }
    if (cache.size() >= limit) {
      addByReplace(term);
      return;
    }
    cache.add(term);
    addStats(new CacheEntryStat(term));
  }

  // Puts the term with the time of caching into an own list for tracking
  private void addStats(CacheEntryStat cacheEntryStat) {
    if (accessFrequency.contains(cacheEntryStat)) {
      return;
    }
    accessFrequency.add(cacheEntryStat);
  }

  // A special case of adding by looking for the oldest cache entry and replacing it with
  // the provided one
  private void addByReplace(TibTerm term) {
    // We want to be sure that the access statistic list is in natural order
    ensureSorted();
    // We then remove the oldest cache entry
    if (!cache.isEmpty()) {
      cache.set(0, term);
      addStats(new CacheEntryStat(term));
    }
  }

  // Ensures the natural order sorting by datetime, when the cache entry has been created
  // Oldest entry will be the first element, newest the last element of the list
  private void ensureSorted() {
    accessFrequency = accessFrequency.stream()
        .sorted(Comparator.comparing(CacheEntryStat::created, Instant::compareTo))
        .collect(Collectors.toList());
  }

  /**
   * Searches for a matching {@link TibTerm} in the cache.
   *
   * @param curie the CURIE to search for
   * @return the search result, {@link Optional#empty()} if no match was found
   * @since 1.9.0
   */
  Optional<TibTerm> findByCurie(String curie) {
    return cache.stream().filter(term -> term.oboId.equals(curie)).findFirst();
  }


  /**
   * A small container for when a cache entry has been created.
   *
   * @since 1.9.0
   */
  private static class CacheEntryStat {

    private final TibTerm term;
    private final Instant created;

    CacheEntryStat(TibTerm term) {
      this.term = term;
      created = Instant.now();
    }

    /**
     * When the cache entry has been created
     *
     * @return the instant of creation
     * @since 1.9.0
     */
    Instant created() {
      return created;
    }

    @Override
    public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      CacheEntryStat that = (CacheEntryStat) o;
      return Objects.equals(term, that.term);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(term);
    }
  }

}
