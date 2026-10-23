package life.qbic.projectmanagement.application.query;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Immutable;

/**
 * Flat, read-only projection of the {@code v_project_sample_overview} view: one row per sample of a
 * project, with experiment attribution, condition and the sample's measured status. It backs the
 * project-level, paginated, read-only sample list and its overview statistics.
 *
 * <p><b>Flat by contract.</b> As with {@link ExperimentProgress}, this mapping carries no JPA
 * associations so it is fully materialised before any reactive scheduler hop (implementation
 * strategy §4.1, decision D6). Do not add associations to this entity.</p>
 *
 * @see life.qbic.projectmanagement.application.api.ProjectQueryService
 * @since 1.19.0
 */
@Immutable
@org.springframework.data.annotation.Immutable
@Entity
@Table(name = "v_project_sample_overview")
public class ProjectSampleOverview {

  @Id
  @Column(name = "sample_id")
  private String sampleId;

  @Column(name = "project_id")
  private String projectId;

  @Column(name = "experiment_id")
  private String experimentId;

  @Column(name = "experiment_name")
  private String experimentName;

  @Column(name = "sample_code")
  private String sampleCode;

  @Column(name = "sample_name")
  private String sampleName;

  @Column(name = "batch")
  private String batch;

  @Column(name = "experimental_group_id")
  private Long experimentalGroupId;

  @Column(name = "experimental_group_name")
  private String experimentalGroupName;

  @Column(name = "analysis_method")
  private String analysisMethod;

  @Column(name = "species")
  private String species;

  @Column(name = "specimen")
  private String specimen;

  @Column(name = "analyte")
  private String analyte;

  @Column(name = "registration_time")
  private Instant registrationTime;

  @Column(name = "last_modified")
  private Instant lastModified;

  @Column(name = "measured")
  private boolean measured;

  protected ProjectSampleOverview() {
    // for JPA
  }

  public String sampleId() {
    return sampleId;
  }

  public String projectId() {
    return projectId;
  }

  public String experimentId() {
    return experimentId;
  }

  public String experimentName() {
    return experimentName;
  }

  public String sampleCode() {
    return sampleCode;
  }

  public String sampleName() {
    return sampleName;
  }

  public String batch() {
    return batch;
  }

  public Long experimentalGroupId() {
    return experimentalGroupId;
  }

  public String experimentalGroupName() {
    return experimentalGroupName;
  }

  public String analysisMethod() {
    return analysisMethod;
  }

  public String species() {
    return species;
  }

  public String specimen() {
    return specimen;
  }

  public String analyte() {
    return analyte;
  }

  public Instant registrationTime() {
    return registrationTime;
  }

  public Instant lastModified() {
    return lastModified;
  }

  /** Whether this sample is referenced by at least one measurement. */
  public boolean measured() {
    return measured;
  }
}
