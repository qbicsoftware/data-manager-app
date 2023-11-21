package life.qbic.projectmanagement.infrastructure.sample;

import java.util.Collection;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import org.springframework.data.jpa.repository.JpaRepository;
public interface QbicSampleRepository extends JpaRepository<Sample, SampleId> {

    Collection<Sample> findAllByExperimentId(ExperimentId experimentId);
    void deleteSamplesByAssignedBatchEquals(BatchId batchId);
}
