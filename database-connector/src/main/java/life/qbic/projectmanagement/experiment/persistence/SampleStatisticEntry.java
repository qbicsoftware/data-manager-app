package life.qbic.projectmanagement.experiment.persistence;

import java.util.Objects;
import life.qbic.projectmanagement.domain.project.ProjectCode;
import life.qbic.projectmanagement.domain.project.ProjectId;

public record SampleStatisticEntry(ProjectId projectId, ProjectCode projectCode, int sampleCounter) {

  public SampleStatisticEntry(ProjectId projectId, ProjectCode projectCode, int sampleCounter) {
    this.projectId = Objects.requireNonNull(projectId);
    this.projectCode = Objects.requireNonNull(projectCode);
    this.sampleCounter = sampleCounter;
  }

  public SampleStatisticEntry addOneSample() {
    return new SampleStatisticEntry(this.projectId, this.projectCode(), sampleCounter + 1);
  }
}
