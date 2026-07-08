package life.qbic.projectmanagement.infrastructure.dataset;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * <b>Local Raw Dataset IP Entry</b>
 * <p>
 * Database entity that represents the content of a detailed view of an immunopeptidomics raw dataset.
 *
 * @since 1.12.0
 */
@Entity
@Table(name = "v_ip_measurement_sample_json")
public class LocalRawDatasetIpEntry {

  public LocalRawDatasetIpEntry() {
  }

  @Id
  @Column(name = "measurement_id")
  private String id;

  @Column(name = "total_filesize_bytes")
  private long totalFileSizeBytes;

  @Column(name = "measurementCode")
  private String measurementCode;

  @Column(name = "measurementName")
  private String measurementName;

  @Column(name = "registration_at")
  private Instant registrationDate;

  @Column(name = "file_count")
  private int numberOfFiles;

  @Convert(converter = FileTypesConverter.class)
  @Column(name = "file_types")
  private Set<String> fileTypes;

  @Column(name = "experiment_id")
  private String experimentId;

  @Convert(converter = MeasuredSamplesConverter.class)
  @Column(name = "samples_json")
  private List<MeasuredSample> measuredSamples;

  public List<MeasuredSample> getMeasuredSamples() {
    return List.copyOf(measuredSamples);
  }

  public String getId() {
    return id;
  }

  public long getTotalFileSizeBytes() {
    return totalFileSizeBytes;
  }

  public String getMeasurementCode() {
    return measurementCode;
  }

  public String getMeasurementName() {
    return measurementName;
  }

  public Instant getRegistrationDate() {
    return registrationDate;
  }

  public int getNumberOfFiles() {
    return numberOfFiles;
  }

  public Set<String> getFileTypes() {
    return fileTypes;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LocalRawDatasetIpEntry that = (LocalRawDatasetIpEntry) o;
    return totalFileSizeBytes == that.totalFileSizeBytes && numberOfFiles == that.numberOfFiles
        && Objects.equals(id, that.id) && Objects.equals(measurementCode,
        that.measurementCode) && Objects.equals(measurementName,
        that.measurementName) && Objects.equals(registrationDate, that.registrationDate)
        && Objects.equals(fileTypes, that.fileTypes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, totalFileSizeBytes, measurementCode, measurementName, registrationDate, numberOfFiles,
        fileTypes);
  }
}
