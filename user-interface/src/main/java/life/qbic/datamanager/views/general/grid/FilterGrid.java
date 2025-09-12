package life.qbic.datamanager.views.general.grid;

import java.util.Objects;
import java.util.Set;
import life.qbic.datamanager.views.general.MultiSelectLazyLoadingGrid;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class FilterGrid<T> {

  private final MultiSelectLazyLoadingGrid<T> grid;

  public FilterGrid(MultiSelectLazyLoadingGrid<T> grid) {
    this.grid = Objects.requireNonNull(grid);
  }

  public Set<T> selectedElements() {
    return grid.getSelectedItems();
    grid.getDataProvider().withConfigurableFilter().setFilter();
  }



}
