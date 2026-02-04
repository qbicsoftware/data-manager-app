package life.qbic.datamanager.views.general.grid.component;

/**
 * <interface short description>
 *
 * @since <version tag>
 */
@FunctionalInterface
public interface Filterable<F> {

  void setFilter(F filter);

}
