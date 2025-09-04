package life.qbic.projectmanagement.infrastructure.dataset;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.units.qual.C;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Entity
@Table(name = "v_pxp_measurement_sample_json")
public class LocalRawDatasetPxpEntry {

  @Id
  @Column(name = "measurement_id")
  private String id;

  @Column(name = "total_filesize_bytes")
  private long totalFileSizeBytes;

  @Column(name = "measurementCode")
  private String measurementCode;

  @Column(name = "registration_at")
  private Date registrationDate;

  @Column(name = "file_count")
  private int numberOfFiles;

  @Convert(converter = FileTypesConverter.class)
  @Column(name = "file_types")
  private Set<String> fileTypes;

  @Column(name = "experiment_id")
  private String experimentId;

  public String getId() {
    return id;
  }

  public long getTotalFileSizeBytes() {
    return totalFileSizeBytes;
  }

  public String getMeasurementCode() {
    return measurementCode;
  }

  public Date getRegistrationDate() {
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
    LocalRawDatasetPxpEntry that = (LocalRawDatasetPxpEntry) o;
    return totalFileSizeBytes == that.totalFileSizeBytes && numberOfFiles == that.numberOfFiles
        && Objects.equals(id, that.id) && Objects.equals(measurementCode,
        that.measurementCode) && Objects.equals(registrationDate, that.registrationDate)
        && Objects.equals(fileTypes, that.fileTypes);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, totalFileSizeBytes, measurementCode, registrationDate, numberOfFiles,
        fileTypes);
  }

}
