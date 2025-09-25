package life.qbic.projectmanagement.infrastructure.dataset;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * <b>Local Raw Dataset Entry</b>
 * <p>
 * Database entity that represents the content.
 *
 * @since 1.11.0
 */
@Entity
@Table(name = "remote_measurement_data")
public class LocalRawDatasetEntry {

  @Id
  @Column(name = "measurement_id")
  private String measurementId;

  @Column(name = "file_count")
  private int fileCount;

  @Column(name = "file_types")
  @Convert(converter = FileTypesConverter.class)
  private Set<String> fileTypes;

  @Column(name = "registration_at")
  private Instant registrationDate;

  @Column(name = "total_filesize_bytes")
  private long totalFileSizeBytes;

  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "last_sync_at")
  private Instant lastSyncAt;

  @Column(name = "deleted")
  private boolean deleted;

  public String getMeasurementId() {
    return measurementId;
  }

  public void setMeasurementId(String measurementId) {
    this.measurementId = measurementId;
  }

  public int getFileCount() {
    return fileCount;
  }

  public void setFileCount(int fileCount) {
    this.fileCount = fileCount;
  }

  public Set<String> getFileTypes() {
    return fileTypes;
  }

  public void setFileTypes(Set<String> fileTypes) {
    this.fileTypes = fileTypes;
  }

  public Instant getRegistrationDate() {
    return registrationDate;
  }

  public void setRegistrationDate(Instant registrationDate) {
    this.registrationDate = registrationDate;
  }

  public long getTotalFileSizeBytes() {
    return totalFileSizeBytes;
  }

  public void setTotalFileSizeBytes(long totalFileSizeBytes) {
    this.totalFileSizeBytes = totalFileSizeBytes;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Instant getLastSyncAt() {
    return lastSyncAt;
  }

  public void setLastSyncAt(Instant lastSyncAt) {
    this.lastSyncAt = lastSyncAt;
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean deleted) {
    this.deleted = deleted;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    LocalRawDatasetEntry that = (LocalRawDatasetEntry) o;
    return Objects.equals(measurementId, that.measurementId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(measurementId);
  }
}
