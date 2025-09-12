package life.qbic.datamanager.views.general.grid;

import org.apache.poi.ss.formula.functions.T;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@FunctionalInterface
public interface FilterUpdater<T> {

  Filter<T> withSearchTerm(Filter<T> oldFilter, String term);

}
