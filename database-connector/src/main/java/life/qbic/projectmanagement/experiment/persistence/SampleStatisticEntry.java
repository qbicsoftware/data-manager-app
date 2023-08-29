package life.qbic.projectmanagement.experiment.persistence;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import life.qbic.projectmanagement.domain.project.ProjectCode;
import life.qbic.projectmanagement.domain.project.ProjectId;

/**
 * Sample Statistics Entry
 * <p>
 * A sample statistics entry describes the accumulative sample registrations for a specific
 * project.
 * <p>
 * The accumulative sample registration count enables the automated QBiC sample code generation,
 * since the counter is only increased, never decreased.
 *
 * @since 1.0.0
 */
@Entity
@Table(name = "sample_statistics_entry")
public class SampleStatisticEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;
  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "projectCode"))
  private ProjectCode projectCode;
  @Embedded
  private ProjectId projectId;

  private int sampleCounter;

  private SampleStatisticEntry(ProjectId projectId, ProjectCode projectCode, int sampleCounter) {
    this.projectId = Objects.requireNonNull(projectId);
    this.projectCode = Objects.requireNonNull(projectCode);
    this.sampleCounter = sampleCounter;
  }

  protected SampleStatisticEntry() {

  }

  public static SampleStatisticEntry create(ProjectId projectId, ProjectCode projectCode) {
    return new SampleStatisticEntry(projectId, projectCode, 0);
  }

  public int drawNextSampleNumber() {
    sampleCounter++;
    return sampleCounter;
  }

  public ProjectCode projectCode() {
    return projectCode;
  }

  public ProjectId projectId() {
    return projectId;
  }

  public int sampleCounter() {
    return sampleCounter;
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectId, projectCode, sampleCounter);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || obj.getClass() != this.getClass()) {
      return false;
    }
    var that = (SampleStatisticEntry) obj;
    return Objects.equals(this.projectId, that.projectId) &&
        Objects.equals(this.projectCode, that.projectCode) &&
        this.sampleCounter == that.sampleCounter;
  }

  @Override
  public String toString() {
    return "SampleStatisticEntry[" +
        "projectId=" + projectId + ", " +
        "projectCode=" + projectCode + ", " +
        "sampleCounter=" + sampleCounter + ']';
  }

}
