package life.qbic.projectmanagement.application.query;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Immutable;

/**
 * Flat, read-only projection of the {@code experiment_recent_measurement} view: the newest
 * measurements of an experiment with their primary-data status, for the Dataset snapshot panel.
 *
 * <p>Flat by contract — no JPA associations, so it is safe across the reactive scheduler hop
 * (implementation strategy §4.1). Do not add associations.</p>
 *
 * @since 1.19.0
 */
@Immutable
@org.springframework.data.annotation.Immutable
@Entity
@Table(name = "experiment_recent_measurement")
public class RecentMeasurement {

  @Id
  @Column(name = "id")
  private String id;

  @Column(name = "project_id")
  private String projectId;

  @Column(name = "experiment_id")
  private String experimentId;

  @Column(name = "measurement_type")
  private String measurementType;

  @Column(name = "measurement_id")
  private String measurementId;

  @Column(name = "measurement_code")
  private String measurementCode;

  @Column(name = "measurement_name")
  private String measurementName;

  @Column(name = "primary_data_files")
  private Long primaryDataFiles;

  @Column(name = "primary_data_bytes")
  private Long primaryDataBytes;

  @Column(name = "changed")
  private Instant changed;

  protected RecentMeasurement() {
    // for JPA
  }

  public String id() {
    return id;
  }

  public String projectId() {
    return projectId;
  }

  public String experimentId() {
    return experimentId;
  }

  public String measurementType() {
    return measurementType;
  }

  public String measurementId() {
    return measurementId;
  }

  public String measurementCode() {
    return measurementCode;
  }

  public String measurementName() {
    return measurementName;
  }

  /** Number of primary-data files attached, or {@code null} when none are. */
  public Long primaryDataFiles() {
    return primaryDataFiles;
  }

  /** Size of attached primary data in bytes, or {@code null} when none are. */
  public Long primaryDataBytes() {
    return primaryDataBytes;
  }

  /** Whether primary data is attached to this measurement. */
  public boolean hasPrimaryData() {
    return primaryDataFiles != null;
  }

  /** Registration time (measurements carry no modification timestamp). */
  public Instant changed() {
    return changed;
  }
}
