package life.qbic.controlling.application;

/**
 * A sort order to be consumed by a data providing service.
 *
 * @since 1.0.0
 */
public record SortOrder(String propertyName, boolean isDescending) {

  public boolean isAscending() {
    return !isDescending();
  }

  public SortOrder ascending() {
    return new SortOrder(propertyName, false);
  }

  public SortOrder descending() {
    return new SortOrder(propertyName, true);
  }

  public static SortOrder of(String propertyName) {
    return new SortOrder(propertyName, true);
  }
}
