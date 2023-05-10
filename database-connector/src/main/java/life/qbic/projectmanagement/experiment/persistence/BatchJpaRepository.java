package life.qbic.projectmanagement.experiment.persistence;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Optional;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.project.repository.BatchRepository;
import life.qbic.projectmanagement.domain.project.sample.Batch;
import life.qbic.projectmanagement.domain.project.sample.BatchId;
import life.qbic.projectmanagement.domain.project.service.BatchDomainService.ResponseCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * <b>Batch JPA Repository</b>
 *
 * <p>Implementation of the {@link BatchRepository} interface.</p>
 *
 * @since 1.0.0
 */
@Repository
public class BatchJpaRepository implements BatchRepository {

  private static final Logger log = logger(BatchJpaRepository.class);

  private final QbicBatchRepo qbicBatchRepo;

  @Autowired
  public BatchJpaRepository(QbicBatchRepo qbicBatchRepo) {
    this.qbicBatchRepo = qbicBatchRepo;
  }

  @Override
  public Result<Batch, ResponseCode> add(Batch batch) {
    try {
      qbicBatchRepo.save(batch);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      return Result.fromError(ResponseCode.BATCH_REGISTRATION_FAILED);
    }
    return Result.fromValue(batch);
  }

  @Override
  public Optional<Batch> find(BatchId batchId) {
    return this.qbicBatchRepo.findById(batchId);
  }

  @Override
  public Result<Batch, ResponseCode> update(Batch batch) {
    return Result.fromValue(this.qbicBatchRepo.save(batch));
  }
}
