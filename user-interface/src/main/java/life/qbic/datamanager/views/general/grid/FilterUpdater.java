package life.qbic.datamanager.views.general.grid;

import org.apache.poi.ss.formula.functions.T;

/**
 * Implementations handle the creation of a new filter based on a given one and a search term.
 *
 * @since 1.12.0
 */
@FunctionalInterface
public interface FilterUpdater {

  /**
   * Creates a new filter instance based on the old one and a search term.
   *
   * @param oldFilter the previous filter
   * @param term      the search term
   * @return a filter updated based on the old filter.
   * @since 1.12.0
   */
  Filter withSearchTerm(Filter oldFilter, String term);

}
