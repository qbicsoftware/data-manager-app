package life.qbic.projectmanagement.application.query;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Immutable;

/**
 * Flat, read-only projection of the {@code experiment_recent_sample} view: the newest samples of an
 * experiment, for the Samples snapshot panel.
 *
 * <p>Flat by contract — no JPA associations, so it is safe across the reactive scheduler hop
 * (implementation strategy §4.1). Do not add associations.</p>
 *
 * @since 1.19.0
 */
@Immutable
@org.springframework.data.annotation.Immutable
@Entity
@Table(name = "experiment_recent_sample")
public class RecentSample {

  @Id
  @Column(name = "id")
  private String id;

  @Column(name = "project_id")
  private String projectId;

  @Column(name = "experiment_id")
  private String experimentId;

  @Column(name = "sample_id")
  private String sampleId;

  @Column(name = "sample_code")
  private String sampleCode;

  @Column(name = "sample_name")
  private String sampleName;

  @Column(name = "changed")
  private Instant changed;

  protected RecentSample() {
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

  public String sampleId() {
    return sampleId;
  }

  public String sampleCode() {
    return sampleCode;
  }

  public String sampleName() {
    return sampleName;
  }

  /** Registration or last-modification time, whichever is newer. */
  public Instant changed() {
    return changed;
  }
}
