package life.qbic.projectmanagement.experiment.persistence;

import java.util.Collection;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.sample.BatchId;
import life.qbic.projectmanagement.domain.project.sample.Sample;
import life.qbic.projectmanagement.domain.project.sample.SampleId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QbicSampleRepository extends JpaRepository<Sample, SampleId> {

    Collection<Sample> findAllByExperimentId(ExperimentId experimentId);

    void deleteSamplesByAssignedBatchEquals(BatchId batchId);
}
