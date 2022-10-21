package life.qbic.projectmanagement.application;

/**
 * A sort order to be consumed by a data providing service.
 *
 * @since 1.0.0
 */
public record SortOrder(String propertyName, boolean isDescending) {

  public boolean isAscending() {
    return !isDescending();
  }

  public static SortOrder of(String propertyName) {
    return new SortOrder(propertyName, true);
  }
}
