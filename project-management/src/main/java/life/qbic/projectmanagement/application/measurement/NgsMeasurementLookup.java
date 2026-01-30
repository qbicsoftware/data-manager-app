package life.qbic.projectmanagement.application.measurement;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

/**
 * Provides methods to count and read {@link MeasurementInfo} measurement metadata.
 */
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

  /**
   * An organisation
   *
   * @param label the displayed name / label of the organisation
   * @param iri   an internationalized resource identifier for the organization
   */
  record Organisation(@NonNull String label, @NonNull String iri) {

    public Organisation {
      Objects.requireNonNull(label);
      Objects.requireNonNull(iri);
    }
  }

  /**
   * An instrucment used for the measurement
   * @param label the displayed name/label of the instrument
   * @param oboId the obo identifier of the instrument
   * @param iri the inernationalized resource identifier for the instrument
   */
  record Instrument(@NonNull String label, @NonNull String oboId, @NonNull String iri) {

    public Instrument {
      Objects.requireNonNull(label);
      Objects.requireNonNull(oboId);
      Objects.requireNonNull(iri);
    }
  }

  /**
   * Information about a measured sample
   * @param sampleId the database identifier of the sample
   * @param sampleCode the displayed identifier of the sample
   * @param sampleLabel the name of the sample
   * @param indexI5 the i5 index used in sequencing if any
   * @param indexI7 the i7 index used in sequencing if any
   * @param comment an optional comment on the measured sample
   */
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

  /**
   * Measurement metadata for NGS measurements
   * @param measurementId the measurement database identifier
   * @param projectId the project database identifier
   * @param experimentId the experiment identifier
   * @param measurementCode the displayed measurement identifier
   * @param measurementName the name of the measurement
   * @param facility the facility that recorded the measurement
   * @param organisation the organisation that measured the samples
   * @param instrument the instrument used for measurement
   * @param samplePool the pool name in case samples were pooled in this measurement
   * @param registeredAt the timepoint of measurement metadata registration
   * @param readType the read type
   * @param libraryKit the library kit used for the measurement
   * @param flowCell the flow cell used for the measurement
   * @param runProtocol the run protocol used for the measurement
   * @param sampleInfos a collection of measurement metadata for the measured samples
   */
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

  /**
   * A filter allowing to filter measurements. It is encouraged to create new filters using {@link #forExperiment(String)}.}
   * @param experimentId the experiment identifier for this filter
   * @param searchTerm the term for which to search, can be blank to include everything
   * @param timeZoneOffsetMillis the time zone offset in milliseconds of the client. By default server time zone (value 0) is assumed.
   * @param includedSamples a list of samples that must be measured by a measurement for the filter to accept it
   * @param excludedSamples a list of samples that must not be measured by a measurement for the filter to accept it
   */
  record MeasurementFilter(String experimentId, String searchTerm, int timeZoneOffsetMillis,
                           Set<String> includedSamples,
                           Set<String> excludedSamples) {


    public MeasurementFilter {
      Objects.requireNonNull(experimentId);
      Objects.requireNonNull(searchTerm);
      includedSamples = new HashSet<>(includedSamples);
      excludedSamples = new HashSet<>(excludedSamples);
    }


    /**
     * Creates a new filter configured to filter for measurements belonging to the experiment.
     * @param experimentId the identifier of the experiment
     * @return a configured filter
     */
    public static MeasurementFilter forExperiment(String experimentId) {
      Objects.requireNonNull(experimentId);
      return new MeasurementFilter(experimentId, "", 0, Set.of(), Set.of());
    }

    /**
     * Configures the filter to only accept measurements if they contain the provided search term in a searchable field.
     * @param searchTerm the search term that must be contained, note case-sensitive
     * @param timeZoneOffsetMillis the client timezone offset milliseconds for date rendering
     * @return a configured filter
     */
    public MeasurementFilter withSearch(String searchTerm, int timeZoneOffsetMillis) {
      Objects.requireNonNull(searchTerm);
      return new MeasurementFilter(experimentId, searchTerm, timeZoneOffsetMillis, includedSamples,
          excludedSamples);
    }

    /**
     * Adds samples to the whitelist. Measurements measuring at least one of the whitelisted samples are accepted by this filter.
     * @param sampleIds
     * @return a configured filter
     */
    public MeasurementFilter includingSamples(Collection<String> sampleIds) {
      var combinedSamples = new HashSet<String>();
      combinedSamples.addAll(includedSamples);
      combinedSamples.addAll(sampleIds);
      return new MeasurementFilter(experimentId,
          searchTerm,
          timeZoneOffsetMillis,
          combinedSamples,
          excludedSamples);
    }

    /**
     * Adds samples to the blacklist. Measurements measuring at least one of the whitelisted samples are not accepted by this filter.
     * @param sampleIds
     * @return a configured filter
     */
    public MeasurementFilter excludingSamples(Collection<String> sampleIds) {
      var combinedSamples = new HashSet<String>();
      combinedSamples.addAll(excludedSamples);
      combinedSamples.addAll(sampleIds);
      return new MeasurementFilter(experimentId,
          searchTerm,
          timeZoneOffsetMillis,
          includedSamples,
          combinedSamples);
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
