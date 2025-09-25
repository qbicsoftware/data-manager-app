package life.qbic.projectmanagement.infrastructure.sync;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import org.springframework.lang.NonNull;

/**
 * <b>Watermark Entry</b>
 *
 * <p>Database entity for watermark of synchronisation jobs.</p>
 *
 * @since 1.11.0
 */
@Entity
@Table(name = "sync_control")
class WatermarkEntry implements Serializable {

  public WatermarkEntry() {

  }

  public static WatermarkEntry create(String id, int syncOffset, Instant updatedSince,
      Instant lastSuccessAt) {
    return new WatermarkEntry(id, syncOffset, updatedSince, lastSuccessAt);
  }

  private WatermarkEntry(String jobName, int syncOffset, Instant updatedSince, Instant lastSuccessAt) {
    this.jobName = jobName;
    this.syncOffset = syncOffset;
    this.updatedSince = Objects.requireNonNull(updatedSince);
    this.lastSuccessAt = Objects.requireNonNull(lastSuccessAt);
  }

  @Id
  @Column(name = "job_name")
  String jobName;

  @Column(name = "sync_offset")
  Integer syncOffset;

  @NonNull
  @Column(name = "updated_since")
  Instant updatedSince;

  @NonNull
  @Column(name = "last_success_at")
  Instant lastSuccessAt;

}
