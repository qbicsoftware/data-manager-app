package life.qbic.datamanager.views.projects.project.dataset;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import life.qbic.application.commons.SortOrder;
import life.qbic.projectmanagement.application.api.ProjectQueryService.MeasurementDataSortKey;
import life.qbic.projectmanagement.application.api.ProjectQueryService.SortDirection;

/**
 * Sort helpers for the paginated measurement data list.
 *
 * <p>Single sort, like every other paginated list in the app (ADR-0009): the URL carries exactly
 * one {@code sort=property:asc|desc} parameter. Each sort property is validated against {@link
 * MeasurementDataSortKey} before it reaches the data layer, and a deterministic {@code
 * measurementCode ASC} tie-break is appended by the read model so offset/limit pagination cannot
 * duplicate or drop rows across page boundaries.</p>
 *
 * <p>The default is registration date descending: the entry a reader most often wants is the one
 * just registered, and — because metadata is registered before its data arrives — the newest rows
 * are also the ones most likely to be waiting.</p>
 *
 * @since 1.21.0
 */
public final class MeasurementDataSort {

  private MeasurementDataSort() {
  }

  /** The default sort order: most recently registered first. */
  public static final SortOrder DEFAULT = new SortOrder("registrationDate", true);

  /**
   * All sort orders selectable for the measurement data list, keyed by their sort property.
   *
   * <p>Data status is sortable because it is a list column: a user looking for the outstanding
   * work sorts by it rather than scanning pages.</p>
   */
  public static final List<SortOrder> ALLOWED_SORTS = List.of(
      new SortOrder("measurementId", false),
      new SortOrder("measurementId", true),
      new SortOrder("measurementName", false),
      new SortOrder("measurementName", true),
      new SortOrder("registrationDate", false),
      new SortOrder("registrationDate", true),
      new SortOrder("dataStatus", false),
      new SortOrder("dataStatus", true),
      new SortOrder("samplePool", false),
      new SortOrder("samplePool", true));

  private static final Map<String, MeasurementDataSortKey> SORT_KEY_BY_PROPERTY = new HashMap<>();

  static {
    SORT_KEY_BY_PROPERTY.put("measurementId", MeasurementDataSortKey.MEASUREMENT_ID);
    SORT_KEY_BY_PROPERTY.put("measurementName", MeasurementDataSortKey.MEASUREMENT_NAME);
    SORT_KEY_BY_PROPERTY.put("registrationDate", MeasurementDataSortKey.REGISTRATION_DATE);
    SORT_KEY_BY_PROPERTY.put("dataStatus", MeasurementDataSortKey.DATA_STATUS);
    SORT_KEY_BY_PROPERTY.put("samplePool", MeasurementDataSortKey.SAMPLE_POOL);
  }

  /**
   * Translates a {@link SortOrder} into the API sort order the query service expects, validating
   * the property first.
   *
   * @param sortOrder the sort order to translate
   * @return the equivalent API sort order
   * @throws IllegalArgumentException if the sort property is not a valid measurement data sort key
   */
  public static life.qbic.projectmanagement.application.api.ProjectQueryService.SortOrder<MeasurementDataSortKey> toApiSortOrder(
      SortOrder sortOrder) {
    MeasurementDataSortKey key = SORT_KEY_BY_PROPERTY.get(sortOrder.propertyName());
    if (key == null) {
      throw new IllegalArgumentException(
          "Invalid sort property '%s' for the measurement data list".formatted(
              sortOrder.propertyName()));
    }
    SortDirection direction = sortOrder.isDescending() ? SortDirection.DESC : SortDirection.ASC;
    return new life.qbic.projectmanagement.application.api.ProjectQueryService.SortOrder<>(key,
        direction);
  }

  /** @return the allowed sort orders for the measurement data list */
  public static List<SortOrder> allowedSortOrders() {
    return ALLOWED_SORTS;
  }
}
