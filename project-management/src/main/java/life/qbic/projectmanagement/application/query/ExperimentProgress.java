package life.qbic.projectmanagement.application.query;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

/**
 * Flat, read-only projection of the {@code experiment_progress} view: exactly one row per
 * experiment, carrying the counts the project page's progress band and the project-level dataset
 * overview render.
 *
 * <p><b>Flat by contract.</b> This view mapping deliberately carries <em>no JPA associations</em>.
 * That keeps it fully materialised by Hibernate, so it is safe to hand to another thread after a
 * reactive scheduler hop (implementation strategy §4.1, decision D6). Do not add associations to
 * this entity — a lazily-loaded proxy would break the reactive boundary.</p>
 *
 * @see life.qbic.projectmanagement.application.api.ProjectQueryService
 * @since 1.19.0
 */
@Immutable
@org.springframework.data.annotation.Immutable
@Entity
@Table(name = "experiment_progress")
public class ExperimentProgress {

  @Id
  @Column(name = "experiment_id")
  private String experimentId;

  @Column(name = "project_id")
  private String projectId;

  @Column(name = "experiment_name")
  private String experimentName;

  @Column(name = "sample_count")
  private int sampleCount;

  @Column(name = "measured_sample_count")
  private int measuredSampleCount;

  @Column(name = "measurement_count")
  private int measurementCount;

  @Column(name = "ngs_measurement_count")
  private int ngsMeasurementCount;

  @Column(name = "pxp_measurement_count")
  private int pxpMeasurementCount;

  @Column(name = "ip_measurement_count")
  private int ipMeasurementCount;

  @Column(name = "measurements_with_primary_data")
  private int measurementsWithPrimaryData;

  @Column(name = "primary_data_files")
  private long primaryDataFiles;

  @Column(name = "primary_data_bytes")
  private long primaryDataBytes;

  protected ExperimentProgress() {
    // for JPA
  }

  public String experimentId() {
    return experimentId;
  }

  public String projectId() {
    return projectId;
  }

  public String experimentName() {
    return experimentName;
  }

  /** Number of samples registered in this experiment. */
  public int sampleCount() {
    return sampleCount;
  }

  /** Number of samples that are referenced by at least one measurement. */
  public int measuredSampleCount() {
    return measuredSampleCount;
  }

  /** Number of measurements registered for this experiment, across all types. */
  public int measurementCount() {
    return measurementCount;
  }

  public int ngsMeasurementCount() {
    return ngsMeasurementCount;
  }

  public int pxpMeasurementCount() {
    return pxpMeasurementCount;
  }

  public int ipMeasurementCount() {
    return ipMeasurementCount;
  }

  /** Number of measurements that have primary data attached (deleted data excluded). */
  public int measurementsWithPrimaryData() {
    return measurementsWithPrimaryData;
  }

  /**
   * Number of measurements that still lack primary data.
   *
   * <p>This is the "outstanding work" count the summary states as a gap; it is never negative.</p>
   */
  public int measurementsWithoutPrimaryData() {
    return Math.max(0, measurementCount - measurementsWithPrimaryData);
  }

  /** Total number of primary-data files across all measurements of this experiment. */
  public long primaryDataFiles() {
    return primaryDataFiles;
  }

  /** Total size of primary data across all measurements of this experiment, in bytes. */
  public long primaryDataBytes() {
    return primaryDataBytes;
  }
}
