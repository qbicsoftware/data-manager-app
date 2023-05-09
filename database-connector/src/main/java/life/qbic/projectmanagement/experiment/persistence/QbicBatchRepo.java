package life.qbic.projectmanagement.experiment.persistence;

import life.qbic.projectmanagement.domain.project.sample.Batch;
import life.qbic.projectmanagement.domain.project.sample.BatchId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QbicBatchRepo extends JpaRepository<Batch, BatchId> {



}
