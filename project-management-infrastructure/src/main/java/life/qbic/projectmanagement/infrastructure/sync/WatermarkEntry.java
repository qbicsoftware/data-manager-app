package life.qbic.projectmanagement.infrastructure.sync;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import org.springframework.lang.NonNull;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Entity
@Table(name = "sync_control")
class WatermarkEntry implements Serializable {

  public WatermarkEntry() {

  }

  public static WatermarkEntry create(String id, int syncOffset, Date updatedSince, Date lastSuccessAt) {
    return new WatermarkEntry(id, syncOffset, updatedSince, lastSuccessAt);
  }

  private WatermarkEntry(String jobName, int syncOffset, Date updatedSince, Date lastSuccessAt) {
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
  Date updatedSince;

  @NonNull
  @Column(name = "last_success_at")
  Date lastSuccessAt;

}
