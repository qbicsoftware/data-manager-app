package life.qbic.projectmanagement.application.measurement;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;

public interface PxpMeasurementLookup {

  /**
   * Indicates a problem with a provided sort key
   */
  class SortKeyException extends IllegalArgumentException {

    public SortKeyException(String s) {
      super(s);
    }
  }

  record Organisation(@NonNull String label, @NonNull String iri) {

    public Organisation {
      Objects.requireNonNull(label);
      Objects.requireNonNull(iri);
    }
  }

  record MsDevice(@NonNull String label, @NonNull String oboId, @NonNull String iri) {

    public MsDevice {
      Objects.requireNonNull(label);
      Objects.requireNonNull(oboId);
      Objects.requireNonNull(iri);
    }
  }

  record SampleInfo(
      @NonNull String sampleId,
      @NonNull String sampleCode,
      @NonNull String sampleLabel,
      String fractionName,
      String measurementLabel,
      String comment
  ) {

    public SampleInfo {
      Objects.requireNonNull(sampleId);
      Objects.requireNonNull(sampleCode);
      Objects.requireNonNull(sampleLabel);
    }
  }

  /**
   * Make sure that your JPA object provides getters for all {@link #sortKey()} values.
   */
  enum PxpSortKey {
    MEASUREMENT_ID("measurementCode"),
    MEASUREMENT_NAME("measurementName"),
    FACILITY("facility"),
    DIGESTION_ENZYME("digestionEnzyme"),
    DIGESTION_METHOD("digestionMethod"),
    ENRICHMENT_METHOD("enrichmentMethod"),
    INJECTION_VOLUME("injectionVolume"),
    LABEL_TYPE("labelType"),
    LABEL("label"),
    TECHNICAL_REPLICATE("technicalReplicateName"),
    LCMS_METHOD("lcmsMethod"),
    LC_COLUMN("lcColumn"),
    REGISTRATION_DATE("registeredAt");

    private final String sortKey;

    PxpSortKey(String sortKey) {
      this.sortKey = sortKey;
    }

    public String sortKey() {
      return sortKey;
    }

    public static boolean isValidSortKey(String property) {
      if (Objects.isNull(property) || property.isBlank()) {
        return false;
      }
      return Arrays.stream(PxpMeasurementLookup.PxpSortKey.values())
          .map(PxpMeasurementLookup.PxpSortKey::sortKey)
          .anyMatch(sortKey -> sortKey.equals(property));
    }

  }

  record MeasurementInfo(@NonNull String measurementId,
                         @NonNull String projectId,
                         @NonNull String experimentId,
                         @NonNull String measurementCode,
                         String measurementName,
                         String facility,
                         Organisation organisation,
                         MsDevice msDevice,
                         String samplePool,
                         @NonNull Instant registeredAt,
                         String digestionEnzyme,
                         String digestionMethod,
                         String enrichmentMethod,
                         double injectionVolume,
                         String labelType,
                         String label,
                         String technicalReplicateName,
                         String lcmsMethod,
                         String lcColumn,
                         @NonNull List<PxpMeasurementLookup.SampleInfo> sampleInfos) {

    public MeasurementInfo {
      Objects.requireNonNull(measurementId);
      Objects.requireNonNull(projectId);
      Objects.requireNonNull(experimentId);
      Objects.requireNonNull(measurementCode);
      Objects.requireNonNull(registeredAt);
      Objects.requireNonNull(sampleInfos);
    }

  }

  record MeasurementFilter(String experimentId, String searchTerm) {

    public MeasurementFilter {
      Objects.requireNonNull(experimentId);
      Objects.requireNonNull(searchTerm);
    }
  }

  /**
   * Provides a stream containing filtered measurement information.
   *
   * @param projectId
   * @param offset
   * @param limit
   * @param measurementFilter
   * @return
   */
  @NonNull
  Stream<MeasurementInfo> lookupPxpMeasurements(@NonNull String projectId, int offset, int limit,
      @NonNull Sort sort,
      @NonNull MeasurementFilter measurementFilter) throws SortKeyException;

  int countPxpMeasurements(@NonNull String projectId, @NonNull MeasurementFilter measurementFilter);

}
