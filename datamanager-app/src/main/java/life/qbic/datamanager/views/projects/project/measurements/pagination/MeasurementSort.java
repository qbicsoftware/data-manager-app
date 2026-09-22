package life.qbic.datamanager.views.projects.project.measurements.pagination;

import java.util.Objects;
import life.qbic.application.commons.SortOrder;
import life.qbic.projectmanagement.application.measurement.IpMeasurementLookup;
import life.qbic.projectmanagement.application.measurement.NgsMeasurementLookup;
import life.qbic.projectmanagement.application.measurement.PxpMeasurementLookup;
import org.springframework.data.domain.Sort;

/**
 * Sort helpers for the paginated measurement lists (ADR-0008).
 *
 * <p>The measurement grids are single-sort: the URL carries exactly one {@code sort} parameter
 * ({@code property:asc|desc}) that is translated into a Spring Data {@link Sort}. All order
 * properties are validated against the per-domain sort keys of the corresponding lookup before
 * being passed to the data layer; the data layer (see
 * {@code MeasurementLookup#withMeasurementCodeTieBreaker(Sort)}) appends the deterministic
 * {@code measurementCode ASC} tie-break so offset/limit pagination never duplicates or drops
 * items across page boundaries.</p>
 *
 * @since 1.19.0
 */
public final class MeasurementSort {

  private MeasurementSort() {}

  /**
   * The default sort order of the measurement lists: registration date, newest first.
   */
  public static final SortOrder DEFAULT = new SortOrder("registeredAt", true);

  /**
   * All sort orders selectable for the NGS measurement list, keyed by their sort property.
   */
  public static final java.util.List<SortOrder> NGS_SORTS = java.util.List.of(
      new SortOrder("measurementCode", true),
      new SortOrder("measurementCode", false),
      new SortOrder("measurementName", true),
      new SortOrder("measurementName", false),
      new SortOrder("facility", true),
      new SortOrder("facility", false),
      new SortOrder("sequencingReadType", true),
      new SortOrder("sequencingReadType", false),
      new SortOrder("libraryKit", true),
      new SortOrder("libraryKit", false),
      new SortOrder("flowCell", true),
      new SortOrder("flowCell", false),
      new SortOrder("sequencingRunProtocol", true),
      new SortOrder("sequencingRunProtocol", false),
      new SortOrder("registeredAt", true),
      new SortOrder("registeredAt", false));

  /**
   * All sort orders selectable for the Proteomics (PxP) measurement list.
   */
  public static final java.util.List<SortOrder> PXP_SORTS = java.util.List.of(
      new SortOrder("measurementCode", true),
      new SortOrder("measurementCode", false),
      new SortOrder("measurementName", true),
      new SortOrder("measurementName", false),
      new SortOrder("facility", true),
      new SortOrder("facility", false),
      new SortOrder("technicalReplicateName", true),
      new SortOrder("technicalReplicateName", false),
      new SortOrder("digestionEnzyme", true),
      new SortOrder("digestionEnzyme", false),
      new SortOrder("digestionMethod", true),
      new SortOrder("digestionMethod", false),
      new SortOrder("injectionVolume", true),
      new SortOrder("injectionVolume", false),
      new SortOrder("lcmsMethod", true),
      new SortOrder("lcmsMethod", false),
      new SortOrder("lcColumn", true),
      new SortOrder("lcColumn", false),
      new SortOrder("enrichmentMethod", true),
      new SortOrder("enrichmentMethod", false),
      new SortOrder("registeredAt", true),
      new SortOrder("registeredAt", false));

  /**
   * All sort orders selectable for the Immunopeptidomics (IP) measurement list.
   */
  public static final java.util.List<SortOrder> IP_SORTS = java.util.List.of(
      new SortOrder("measurementCode", true),
      new SortOrder("measurementCode", false),
      new SortOrder("measurementName", true),
      new SortOrder("measurementName", false),
      new SortOrder("facility", true),
      new SortOrder("facility", false),
      new SortOrder("mhcAntibody", true),
      new SortOrder("mhcAntibody", false),
      new SortOrder("mhcTypingMethod", true),
      new SortOrder("mhcTypingMethod", false),
      new SortOrder("enrichmentMethod", true),
      new SortOrder("enrichmentMethod", false),
      new SortOrder("lcmsMethod", true),
      new SortOrder("lcmsMethod", false),
      new SortOrder("lcColumn", true),
      new SortOrder("lcColumn", false),
      new SortOrder("dataAcquisition", true),
      new SortOrder("dataAcquisition", false),
      new SortOrder("massRange", true),
      new SortOrder("massRange", false),
      new SortOrder("retentionTimeRange", true),
      new SortOrder("retentionTimeRange", false),
      new SortOrder("chargeRange", true),
      new SortOrder("chargeRange", false),
      new SortOrder("ionMobilityRange", true),
      new SortOrder("ionMobilityRange", false),
      new SortOrder("sampleMass", true),
      new SortOrder("sampleMass", false),
      new SortOrder("sampleVolume", true),
      new SortOrder("sampleVolume", false),
      new SortOrder("cycleFractionName", true),
      new SortOrder("cycleFractionName", false),
      new SortOrder("prepDate", true),
      new SortOrder("prepDate", false),
      new SortOrder("msRunDate", true),
      new SortOrder("msRunDate", false),
      new SortOrder("registeredAt", true),
      new SortOrder("registeredAt", false));

  /**
   * Translates a {@link SortOrder} into a Spring Data {@link Sort}, validating the property
   * against the sort keys of the given domain.
   *
   * @param sortOrder the sort order to translate
   * @param domain    the measurement domain whose sort keys apply
   * @return the equivalent Spring Data sort
   * @throws IllegalArgumentException if the sort property is not a valid sort key for the domain
   */
  public static Sort toSpringDataSort(SortOrder sortOrder, MeasurementDomain domain) {
    String property = sortOrder.propertyName();
    Objects.requireNonNull(domain, "domain must not be null");
    boolean valid = switch (domain) {
      case NGS -> NgsMeasurementLookup.NgsSortKey.isValidSortKey(property);
      case PXP -> PxpMeasurementLookup.PxpSortKey.isValidSortKey(property);
      case IP -> IpMeasurementLookup.IpSortKey.isValidSortKey(property);
    };
    if (!valid) {
      throw new IllegalArgumentException(
          "Invalid sort property '%s' for domain %s".formatted(property, domain));
    }
    return Sort.by(sortOrder.isDescending() ? Sort.Direction.DESC : Sort.Direction.ASC, property);
  }

  /**
   * Returns the allowed sort orders for the given domain.
   */
  public static java.util.List<SortOrder> allowedSortOrders(MeasurementDomain domain) {
    return switch (domain) {
      case NGS -> NGS_SORTS;
      case PXP -> PXP_SORTS;
      case IP -> IP_SORTS;
    };
  }
}