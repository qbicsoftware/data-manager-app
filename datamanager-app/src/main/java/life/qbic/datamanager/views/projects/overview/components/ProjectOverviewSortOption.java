package life.qbic.datamanager.views.projects.overview.components;

import java.util.Arrays;
import java.util.List;
import life.qbic.application.commons.SortOrder;

/**
 * Sort orders selectable for the paginated project overview.
 * <p>
 * The default ordering remains {@code lastModified} descending — the behaviour stewards know today —
 * until the application supports per-user list preferences that survive a re-login (ADR-0007).
 *
 * @since 1.12.0
 */
public enum ProjectOverviewSortOption {

  LAST_MODIFIED_DESC("Last modified (newest first)", "lastModified", true),
  TITLE_ASC("Project title (A–Z)", "projectTitle", false),
  TITLE_DESC("Project title (Z–A)", "projectTitle", true),
  CODE_ASC("Project code (A–Z)", "projectCode", false),
  CODE_DESC("Project code (Z–A)", "projectCode", true);

  private final String label;
  private final String propertyName;
  private final boolean descending;

  ProjectOverviewSortOption(String label, String propertyName, boolean descending) {
    this.label = label;
    this.propertyName = propertyName;
    this.descending = descending;
  }

  /**
   * @return the human-readable label shown in the sort selector
   */
  public String label() {
    return label;
  }

  /**
   * @return the service-consumable sort order
   */
  public SortOrder toSortOrder() {
    return new SortOrder(propertyName, descending);
  }

  /**
   * @return the default sort order for the project overview ({@code lastModified} descending)
   */
  public static SortOrder defaultSort() {
    return LAST_MODIFIED_DESC.toSortOrder();
  }

  /**
   * @return all user-selectable sort orders, used to validate list state parsed from the URL
   */
  public static List<SortOrder> allowedSortOrders() {
    return Arrays.stream(values()).map(ProjectOverviewSortOption::toSortOrder).toList();
  }

  /**
   * Resolves the option matching the given sort order, falling back to the default option for
   * unknown orders.
   *
   * @param sortOrder the sort order to resolve
   * @return the matching option, never {@code null}
   */
  public static ProjectOverviewSortOption fromSortOrder(SortOrder sortOrder) {
    return Arrays.stream(values())
        .filter(option -> option.toSortOrder().equals(sortOrder))
        .findFirst()
        .orElse(LAST_MODIFIED_DESC);
  }
}