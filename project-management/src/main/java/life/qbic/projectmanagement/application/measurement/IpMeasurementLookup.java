package life.qbic.projectmanagement.application.measurement;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import life.qbic.application.commons.time.DateTimeFormat;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;

public interface IpMeasurementLookup {

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
      @NonNull String sampleLabel
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
  enum IpSortKey {
    MEASUREMENT_ID("measurementCode"),
    MEASUREMENT_NAME("measurementName"),
    FACILITY("facility"),
    MHC_ANTIBODY("mhcAntibody"),
    MHC_TYPING_METHOD("mhcTypingMethod"),
    ENRICHMENT_METHOD("enrichmentMethod"),
    LCMS_METHOD("lcmsMethod"),
    LC_COLUMN("lcColumn"),
    DATA_ACQUISITION("dataAcquisition"),
    MASS_RANGE("massRange"),
    RETENTION_TIME_RANGE("retentionTimeRange"),
    CHARGE_RANGE("chargeRange"),
    ION_MOBILITY_RANGE("ionMobilityRange"),
    SAMPLE_MASS("sampleMass"),
    SAMPLE_VOLUME("sampleVolume"),
    CYCLE_FRACTION_NAME("cycleFractionName"),
    PREP_DATE("prepDate"),
    MS_RUN_DATE("msRunDate"),
    REGISTRATION_DATE("registeredAt");

    private final String sortKey;

    IpSortKey(String sortKey) {
      this.sortKey = sortKey;
    }

    public String sortKey() {
      return sortKey;
    }

    public static boolean isValidSortKey(String property) {
      if (Objects.isNull(property) || property.isBlank()) {
        return false;
      }
      return Arrays.stream(IpSortKey.values())
          .map(IpSortKey::sortKey)
          .anyMatch(sortKey -> sortKey.equals(property));
    }
  }

  record MeasurementInfo(
      @NonNull String measurementId,
      @NonNull String projectId,
      @NonNull String experimentId,
      @NonNull String measurementCode,
      String measurementName,
      String facility,
      Organisation organisation,
      Instrument instrument,
      String samplePool,
      @NonNull Instant registeredAt,
      String mhcAntibody,
      String mhcTypingMethod,
      String enrichmentMethod,
      String lcmsMethod,
      String lcColumn,
      String dataAcquisition,
      String massRange,
      Integer retentionTimeRange,
      String chargeRange,
      String ionMobilityRange,
      Double sampleMass,
      Double sampleVolume,
      String cycleFractionName,
      String prepDate,
      String msRunDate,
      String comment,
      @NonNull List<SampleInfo> sampleInfos
  ) {

    public MeasurementInfo {
      Objects.requireNonNull(measurementId);
      Objects.requireNonNull(projectId);
      Objects.requireNonNull(experimentId);
      Objects.requireNonNull(measurementCode);
      Objects.requireNonNull(registeredAt);
      Objects.requireNonNull(sampleInfos);
    }
  }

  record MeasurementFilter(
      String experimentId,
      String searchTerm,
      int timeZoneOffsetMillis,
      DateTimeFormat dateTimeFormat,
      Set<String> includedSamples,
      Set<String> excludedSamples
  ) {

    public MeasurementFilter {
      Objects.requireNonNull(experimentId);
      Objects.requireNonNull(searchTerm);
      includedSamples = new HashSet<>(includedSamples);
      excludedSamples = new HashSet<>(excludedSamples);
    }

    public static MeasurementFilter forExperiment(String experimentId) {
      Objects.requireNonNull(experimentId);
      return new MeasurementFilter(experimentId, "", 0,
          DateTimeFormat.ISO_LOCAL_DATE_TIME,
          Set.of(), Set.of());
    }

    public MeasurementFilter withSearch(String searchTerm, int timeZoneOffsetMillis,
        DateTimeFormat dateTimeFormat) {
      Objects.requireNonNull(searchTerm);
      return new MeasurementFilter(experimentId, searchTerm, timeZoneOffsetMillis, dateTimeFormat,
          includedSamples, excludedSamples);
    }

    public MeasurementFilter includingSamples(Collection<String> sampleIds) {
      var combinedSamples = new HashSet<String>();
      combinedSamples.addAll(includedSamples);
      combinedSamples.addAll(sampleIds);
      return new MeasurementFilter(experimentId, searchTerm, timeZoneOffsetMillis, dateTimeFormat,
          combinedSamples, excludedSamples);
    }

    public MeasurementFilter excludingSamples(Collection<String> sampleIds) {
      var combinedSamples = new HashSet<String>();
      combinedSamples.addAll(excludedSamples);
      combinedSamples.addAll(sampleIds);
      return new MeasurementFilter(experimentId, searchTerm, timeZoneOffsetMillis, dateTimeFormat,
          includedSamples, combinedSamples);
    }
  }

  @NonNull
  Stream<MeasurementInfo> lookupIpMeasurements(@NonNull String projectId, int offset, int limit,
      @NonNull Sort sort,
      @NonNull MeasurementFilter measurementFilter) throws SortKeyException;

  int countIpMeasurements(@NonNull String projectId, @NonNull MeasurementFilter measurementFilter);
}
