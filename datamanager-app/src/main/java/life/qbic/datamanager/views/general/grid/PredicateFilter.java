package life.qbic.datamanager.views.general.grid;

/**
 * A simple extension of the {@link Filter} interface to enable on the fly testing of data with type
 * {@code T}.
 *
 * @since 1.12.0
 */
public interface PredicateFilter<T> extends Filter {

  /**
   * Tests if an object of type {@code T} matches the condition defined in the filter.
   *
   * @param data the object to test
   * @return {@code true}, if the condition applies to the object, else returns {@code false}
   * @since 1.12.0
   */
  boolean test(T data);

}
