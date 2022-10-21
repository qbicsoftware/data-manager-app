package life.qbic.projectmanagement.application;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public record SortOrder(String propertyName, boolean isDescending) {

  public boolean isAscending() {
    return !isDescending();
  }

  public static SortOrder of(String propertyName) {
    return new SortOrder(propertyName, true);
  }
}
