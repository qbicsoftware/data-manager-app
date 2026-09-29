package life.qbic.datamanager.views.projects.project.samples;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import life.qbic.application.commons.SortOrder;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SamplePreviewSortKey;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SortDirection;

/**
 * Sort helpers for the paginated sample list (FEAT-PAG-LIST-02).
 *
 * <p>The sample grid is single-sort: the URL carries exactly one {@code sort} parameter
 * ({@code property:asc|desc}) that is translated into the API's
 * {@code SortOrder<SamplePreviewSortKey>} expected by the {@code SamplePreviewFilter}. All order
 * properties are validated against the {@link SamplePreviewSortKey} keys before being passed to the
 * data layer. The {@link life.qbic.projectmanagement.infrastructure.sample.SamplePreviewJpaRepository}
 * applies the sort orders as given, so the owning view appends a deterministic tie-break
 * ({@code sampleCode ASC}) so offset/limit pagination never duplicates or drops items across page
 * boundaries.</p>
 *
 * @since 1.19.0
 */
public final class SampleSort {

  private SampleSort() {}

  /**
   * The default sort order of the sample list: sample ID, ascending.
   */
  public static final SortOrder DEFAULT = new SortOrder("sampleId", false);

  /**
   * All sort orders selectable for the sample list, keyed by their sort property.
   */
  public static final List<SortOrder> ALLOWED_SORTS = List.of(
      new SortOrder("sampleId", false),
      new SortOrder("sampleId", true),
      new SortOrder("sampleName", false),
      new SortOrder("sampleName", true),
      new SortOrder("biologicalReplicate", false),
      new SortOrder("biologicalReplicate", true),
      new SortOrder("batch", false),
      new SortOrder("batch", true),
      new SortOrder("condition", false),
      new SortOrder("condition", true),
      new SortOrder("species", false),
      new SortOrder("species", true),
      new SortOrder("specimen", false),
      new SortOrder("specimen", true),
      new SortOrder("analyte", false),
      new SortOrder("analyte", true),
      new SortOrder("analysisMethod", false),
      new SortOrder("analysisMethod", true),
      new SortOrder("comment", false),
      new SortOrder("comment", true),
      new SortOrder("registrationTime", false),
      new SortOrder("registrationTime", true),
      new SortOrder("lastModified", false),
      new SortOrder("lastModified", true));

  private static final Map<String, SamplePreviewSortKey> SORT_KEY_BY_PROPERTY = new java.util.HashMap<>();

  static {
    SORT_KEY_BY_PROPERTY.put("sampleId", SamplePreviewSortKey.SAMPLE_ID);
    SORT_KEY_BY_PROPERTY.put("sampleName", SamplePreviewSortKey.SAMPLE_NAME);
    SORT_KEY_BY_PROPERTY.put("biologicalReplicate", SamplePreviewSortKey.BIOLOGICAL_REPLICATE);
    SORT_KEY_BY_PROPERTY.put("batch", SamplePreviewSortKey.BATCH);
    SORT_KEY_BY_PROPERTY.put("condition", SamplePreviewSortKey.CONDITION);
    SORT_KEY_BY_PROPERTY.put("species", SamplePreviewSortKey.SPECIES);
    SORT_KEY_BY_PROPERTY.put("specimen", SamplePreviewSortKey.SPECIMEN);
    SORT_KEY_BY_PROPERTY.put("analyte", SamplePreviewSortKey.ANALYTE);
    SORT_KEY_BY_PROPERTY.put("analysisMethod", SamplePreviewSortKey.ANALYSIS_METHOD);
    SORT_KEY_BY_PROPERTY.put("comment", SamplePreviewSortKey.COMMENT);
    SORT_KEY_BY_PROPERTY.put("registrationTime", SamplePreviewSortKey.REGISTRATION_TIME);
    SORT_KEY_BY_PROPERTY.put("lastModified", SamplePreviewSortKey.MODIFICATION_TIME);
  }

  /**
   * Translates a {@link SortOrder} into the API sort order expected by the
   * {@code SamplePreviewFilter}, validating the property against the sample sort keys.
   *
   * @param sortOrder the sort order to translate
   * @return the equivalent API sort order
   * @throws IllegalArgumentException if the sort property is not a valid sample sort key
   */
  public static life.qbic.projectmanagement.application.api.AsyncProjectService.SortOrder<SamplePreviewSortKey> toApiSortOrder(
      SortOrder sortOrder) {
    Objects.requireNonNull(sortOrder, "sortOrder must not be null");
    SamplePreviewSortKey key = SORT_KEY_BY_PROPERTY.get(sortOrder.propertyName());
    if (key == null) {
      throw new IllegalArgumentException(
          "Invalid sort property '%s' for sample list".formatted(sortOrder.propertyName()));
    }
    SortDirection direction = sortOrder.isDescending() ? SortDirection.DESC : SortDirection.ASC;
    return new life.qbic.projectmanagement.application.api.AsyncProjectService.SortOrder<>(key,
        direction);
  }

  /**
   * @return the allowed sort orders for the sample list
   */
  public static List<SortOrder> allowedSortOrders() {
    return ALLOWED_SORTS;
  }
}