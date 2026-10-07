package life.qbic.projectmanagement.infrastructure.sample;

import java.util.Collection;
import java.util.List;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SampleJpaRepository extends JpaRepository<Sample, SampleId> {

  Collection<Sample> findAllByExperimentId(ExperimentId experimentId);

  List<Sample> findAllByProjectId(ProjectId projectId);

  Sample findBySampleCode(SampleCode sampleCode);

  long countAllByExperimentId(ExperimentId experimentId);
}
