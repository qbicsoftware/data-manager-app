package life.qbic.projectmanagement.experiment.persistence;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import life.qbic.projectmanagement.domain.project.ProjectCode;
import life.qbic.projectmanagement.domain.project.ProjectId;

@Entity
@Table(name = "sample_statistics_entry")
public class SampleStatisticEntry {

  @Id
  private Long id;
  @Embedded
  private ProjectCode projectCode;
  @Embedded
  private ProjectId projectId;
  private int sampleCounter;

  public SampleStatisticEntry(ProjectId projectId, ProjectCode projectCode, int sampleCounter) {
    this.projectId = Objects.requireNonNull(projectId);
    this.projectCode = Objects.requireNonNull(projectCode);
    this.sampleCounter = sampleCounter;
  }

  public static SampleStatisticEntry create(ProjectId projectId, ProjectCode projectCode) {
    return new SampleStatisticEntry(projectId, projectCode, 0);
  }

  protected SampleStatisticEntry() {

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
