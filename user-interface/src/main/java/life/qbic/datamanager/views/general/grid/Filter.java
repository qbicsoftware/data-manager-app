package life.qbic.datamanager.views.general.grid;


import java.util.Optional;
import life.qbic.datamanager.views.general.grid.component.FilterGrid;

/**
 * Defines search filters for the {@link FilterGrid} that can be used to define search terms and
 * test if a filter condition applies to an object of its type {@code T}.
 *
 * @since 1.12.0
 */
public interface Filter {

  /**
   * Returns the search term if one exists, or returns {@link Optional#empty}
   *
   * @return the search term if it exists, else {@link Optional#empty()}
   * @since 1.12.0
   */
  Optional<String> searchTerm();

}
