package life.qbic.projectmanagement.application.query;

import static java.util.Objects.requireNonNull;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * One <b>measurement data</b> entry: a registered measurement together with the datum produced by
 * it, as the Measurement Dataset page renders it.
 *
 * <p>A measurement is the metadata describing how a result was produced; the datum is the result.
 * They belong together for the reader but are two ordered process steps: the second is keyed by the
 * measurement code the first mints, and the files arrive asynchronously from the facility through
 * an external system. This row therefore carries <em>both</em> halves and the status that states
 * which step the entry is in.</p>
 *
 * <p><b>Flat by contract.</b> This is a read-model record with no associations, fully materialised
 * by its query, so it is safe to hand to another thread after the reactive scheduler hop
 * (implementation strategy §4.1, decision D6). Do not add lazy-loaded members.</p>
 *
 * <p><b>The datum is 1:1 with the measurement</b> (project owner, 2026-10-26): at most one datum
 * exists per measurement code, so the data fields are always singular and no "latest of several
 * uploads" logic exists anywhere.</p>
 *
 * @see <a href="../../../../../../../../docs/plans/measurement-dataset-ui-refactor.md">Measurement
 * Dataset UI rework</a>
 * @see life.qbic.projectmanagement.application.api.ProjectQueryService
 * @since 1.21.0
 */
public record MeasurementDataRow(
    String measurementId,
    String experimentId,
    String measurementType,
    String measurementCode,
    String measurementName,
    String facility,
    String samplePool,
    int sampleCount,
    Instant registeredAt,
    DataStatus dataStatus,
    Long dataFileCount,
    Long dataSizeBytes,
    Set<String> dataFileTypes,
    Instant dataUploadedAt,
    List<String> methods,
    String instrumentLabel) {

  public MeasurementDataRow {
    requireNonNull(measurementId, "measurementId must not be null");
    requireNonNull(measurementType, "measurementType must not be null");
    requireNonNull(dataStatus, "dataStatus must not be null");
    dataFileTypes = dataFileTypes == null ? Set.of() : Set.copyOf(dataFileTypes);
    methods = methods == null ? List.of() : List.copyOf(methods);
  }

  /**
   * The dataset identity of this row as one line: {@code RNA-SEQ · Illumina}.
   *
   * <p>This is the fact that tells two rows of one omics domain apart. Without it a list filtered to
   * one dataset cannot show that it is filtered, and an experiment measured as both WGS and RNA-Seq
   * renders rows the reader has no way to attribute to either.</p>
   *
   * <p>Plural by nature: a measurement can measure samples annotated with different methods, in which
   * case it is attributed to each of them (ADR 0010 decision 4). The methods are then listed, joined
   * by a comma, rather than one being picked -- picking one would misattribute the row.</p>
   *
   * <p>The instrument is the label the vocabulary term carries, resolved by the query. It is never
   * substituted from the method: a measurement that records no instrument has none, and an empty cell
   * is the honest rendering.</p>
   */
  public String datasetIdentity() {
    String methodPart = methods.isEmpty() ? "" : String.join(", ", methods);
    boolean hasInstrument = instrumentLabel != null && !instrumentLabel.isBlank();
    if (methodPart.isEmpty()) {
      return hasInstrument ? instrumentLabel : "";
    }
    return hasInstrument ? methodPart + " \u00b7 " + instrumentLabel : methodPart;
  }

  /**
   * The status of the second process step for this entry.
   *
   * <p>Only two states are exposed. A {@code deleted} datum row is reported as {@link #WAITING}:
   * upstream removal leaves no data to hand out, which for this page is the same statement as "not
   * available". An entry whose datum exists but whose measurement is gone is <em>not</em>
   * representable here — it cannot be attributed to an experiment at all, and is escalated
   * separately (see the rework plan, §9.2).</p>
   */
  public enum DataStatus {
    /** Registered, data not available yet. The normal state after registration. */
    WAITING,
    /** Data is available for download. */
    AVAILABLE
  }

  /** Whether the datum for this measurement is available. */
  public boolean hasData() {
    return dataStatus == DataStatus.AVAILABLE;
  }

  /**
   * Whether the recorded size is usable.
   *
   * <p>{@code 0} bytes is ambiguous: it means either no data or data whose size the external sync
   * never recorded, which is common in practice. Callers must not render it as "0 B".</p>
   */
  public boolean hasRecordedSize() {
    return dataSizeBytes != null && dataSizeBytes > 0;
  }

  /**
   * Number of files the datum consists of, or {@code 0} when the size was never recorded.
   *
   * <p>{@code remote_measurement_data.file_count} is nullable in the schema; absent means the
   * external sync did not report it, not that the datum is empty.</p>
   */
  public int fileCountOrZero() {
    return dataFileCount == null ? 0 : Math.toIntExact(dataFileCount);
  }
}
