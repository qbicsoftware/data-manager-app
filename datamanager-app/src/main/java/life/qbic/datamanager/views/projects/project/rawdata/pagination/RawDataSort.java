package life.qbic.datamanager.views.projects.project.rawdata.pagination;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import life.qbic.application.commons.SortOrder;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RawDataSortingKey;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SortDirection;

/**
 * Sort helpers for the paginated raw dataset lists (FEAT-PAG-LIST-04).
 *
 * <p>The raw data grids are single-sort: the URL carries exactly one {@code sort} parameter
 * ({@code property:asc|desc}) that is translated into the API sort orders consumed by
 * {@link life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetFilter}.
 * All order properties are validated against the {@link RawDataSortingKey} sort keys exposed by
 * the service layer before being passed to the data layer; the data layer (see
 * {@code LocalRawDatasetRepositoryImpl#withMeasurementCodeTieBreaker}) appends the deterministic
 * {@code measurementCode ASC} tie-break so offset/limit pagination never duplicates or drops items
 * across page boundaries.</p>
 *
 * @since 1.19.0
 */
public final class RawDataSort {

  private RawDataSort() {}

  /**
   * The default sort order of the raw dataset lists: measurement id, ascending.
   */
  public static final SortOrder DEFAULT = new SortOrder("measurementId", false);

  /**
   * The raw data sort property that maps to a {@link RawDataSortingKey}.
   */
  private static final Map<String, RawDataSortingKey> PROPERTY_TO_KEY = Map.of(
      "measurementId", RawDataSortingKey.MEASUREMENT_ID,
      "uploadDate", RawDataSortingKey.UPLOAD_DATE,
      "sampleName", RawDataSortingKey.SAMPLE_NAME);

  /**
   * All sort orders selectable for the raw dataset lists, keyed by their sort property.
   */
  public static final List<SortOrder> SORTS = PROPERTY_TO_KEY.keySet().stream()
      .flatMap(property -> List.of(new SortOrder(property, false), new SortOrder(property, true))
          .stream())
      .toList();

  /**
   * Translates a {@link SortOrder} into the API sort order consumed by the raw dataset service,
   * validating the property against the raw data sort keys.
   *
   * @param sortOrder the sort order to translate
   * @return the equivalent API sort order, or {@code null} when the property is not sortable
   */
  public static life.qbic.projectmanagement.application.api.AsyncProjectService.SortOrder<RawDataSortingKey> toApiSortOrder(
      SortOrder sortOrder) {
    RawDataSortingKey key = PROPERTY_TO_KEY.get(sortOrder.propertyName());
    if (key == null) {
      return null;
    }
    SortDirection direction =
        sortOrder.isDescending() ? SortDirection.DESC : SortDirection.ASC;
    return new life.qbic.projectmanagement.application.api.AsyncProjectService.SortOrder<>(
        key, direction);
  }

  /**
   * Translates a {@link SortOrder} into the list of API sort orders passed to a
   * {@link life.qbic.projectmanagement.application.api.AsyncProjectService.RawDatasetFilter}.
   *
   * @param sortOrder the sort order to translate
   * @return the API sort orders; an empty list when the property is not sortable
   */
  public static List<life.qbic.projectmanagement.application.api.AsyncProjectService.SortOrder<RawDataSortingKey>> toApiSortOrders(
      SortOrder sortOrder) {
    Objects.requireNonNull(sortOrder, "sortOrder must not be null");
    life.qbic.projectmanagement.application.api.AsyncProjectService.SortOrder<RawDataSortingKey> order =
        toApiSortOrder(sortOrder);
    if (order == null) {
      return List.of();
    }
    return List.of(order);
  }

  /**
   * Validates the given sort order against the raw data sort keys.
   */
  public static boolean isValid(SortOrder sortOrder) {
    return PROPERTY_TO_KEY.containsKey(sortOrder.propertyName());
  }
}