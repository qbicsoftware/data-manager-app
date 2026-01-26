package life.qbic.projectmanagement.application.measurement;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public interface NgsMeasurementLookup {

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

  record Instrument(@NonNull String label, @NonNull String oboId, @NonNull String iri) {

    public Instrument {
      Objects.requireNonNull(label);
      Objects.requireNonNull(oboId);
      Objects.requireNonNull(iri);
    }
  }

  record SampleInfo(
      @NonNull String sampleId,
      @NonNull String sampleCode,
      @NonNull String sampleLabel,
      String indexI5,
      String indexI7,
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
  enum NgsSortKey {
    MEASUREMENT_ID("measurementCode"),
    MEASUREMENT_NAME("measurementName"),
    FACILITY("facility"),
    READ_TYPE("sequencingReadType"),
    LIBRARY_KIT("libraryKit"),
    FLOW_CELL("flowCell"),
    RUN_PROTOCOL("sequencingRunProtocol"),
    SAMPLE_POOL("samplePool"),
    REGISTRATION_DATE("registeredAt");

    private final String sortKey;

    NgsSortKey(String sortKey) {
      this.sortKey = sortKey;
    }

    public String sortKey() {
      return sortKey;
    }

    public static boolean isValidSortKey(String property) {
      if (Objects.isNull(property) || property.isBlank()) {
        return false;
      }
      return Arrays.stream(NgsSortKey.values())
          .map(NgsSortKey::sortKey)
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
                         Instrument instrument,
                         String samplePool,
                         @NonNull Instant registeredAt,
                         String readType,
                         String libraryKit,
                         String flowCell,
                         String runProtocol,
                         @NonNull List<SampleInfo> sampleInfos) {

    public MeasurementInfo {
      Objects.requireNonNull(measurementId);
      Objects.requireNonNull(projectId);
      Objects.requireNonNull(experimentId);
      Objects.requireNonNull(measurementCode);
      Objects.requireNonNull(registeredAt);
      Objects.requireNonNull(sampleInfos);
    }

  }

  record MeasurementFilter(String experimentId, String searchTerm, int timeZoneOffsetMillis) {

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
  Stream<MeasurementInfo> lookupNgsMeasurements(@NonNull String projectId, int offset, int limit,
      @NonNull Sort sort,
      @NonNull MeasurementFilter measurementFilter) throws SortKeyException;

  int countNgsMeasurements(@NonNull String projectId, @NonNull MeasurementFilter measurementFilter);
}
