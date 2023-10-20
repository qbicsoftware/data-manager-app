package life.qbic.controlling.infrastructure.batch;

import life.qbic.controlling.domain.model.batch.Batch;
import life.qbic.controlling.domain.model.batch.BatchId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QbicBatchRepo extends JpaRepository<Batch, BatchId> {



}
