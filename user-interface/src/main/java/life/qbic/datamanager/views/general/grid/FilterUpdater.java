package life.qbic.datamanager.views.general.grid;

/**
 * Implementations handle the creation of a new filter based on a given one and a search term.
 *
 * @since 1.12.0
 */
@FunctionalInterface
public interface FilterUpdater<T> {

  /**
   * Creates a new filter instance based on the old one and a search term.
   *
   * @param oldFilter the previous filter
   * @param term      the search term
   * @return a filter of type {@code T}.
   * @since 1.12.0
   */
  Filter<T> withSearchTerm(Filter<T> oldFilter, String term);

}
