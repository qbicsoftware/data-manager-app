package life.qbic.projectmanagement.infrastructure.sync;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Date;

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

  @Id
  @Column(name = "job_name")
  String jobName;

  @Column(name = "sync_offset")
  Integer syncOffset;

  @Column(name = "last_updated_at")
  Date lastUpdatedAt;

  @Column(name = "last_success_at")
  Date lastSuccessAt;

}
