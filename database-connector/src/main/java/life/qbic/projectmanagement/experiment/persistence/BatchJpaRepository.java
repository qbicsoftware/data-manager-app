package life.qbic.projectmanagement.experiment.persistence;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.repository.BatchRepository;
import life.qbic.projectmanagement.domain.project.sample.Batch;
import life.qbic.projectmanagement.domain.project.sample.BatchId;
import life.qbic.projectmanagement.domain.project.sample.Sample;
import life.qbic.projectmanagement.domain.project.service.BatchDomainService.ResponseCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

  private final QbicSampleRepository qbicSampleRepository;

  @Autowired
  public BatchJpaRepository(QbicBatchRepo qbicBatchRepo,
      QbicSampleRepository qbicSampleRepository) {
    this.qbicBatchRepo = qbicBatchRepo;
    this.qbicSampleRepository = qbicSampleRepository;
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

  @Override
  @Transactional
  public Result<BatchId, ResponseCode> deleteById(BatchId batchId) {
    try {
      qbicBatchRepo.deleteById(batchId);
      qbicSampleRepository.deleteSamplesByAssignedBatchEquals(batchId);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      return Result.fromError(ResponseCode.BATCH_DELETION_FAILED);
    }
    return Result.fromValue(batchId);
  }

  @Override
  public Result<Collection<Batch>, life.qbic.projectmanagement.application.batch.BatchInformationService.ResponseCode> findBatchesByExperimentId(
      ExperimentId experimentId) {
    Objects.requireNonNull(experimentId);
    Collection<Batch> batches;
    try {
      Collection<Sample> samples = qbicSampleRepository.findAllByExperimentId(experimentId);
      Collection<BatchId> batchIds = samples.stream().map(Sample::assignedBatch)
          .collect(Collectors.toSet());
      batches = batchIds.stream().map(qbicBatchRepo::findById).filter(
          Optional::isPresent).map(Optional::get).toList();
    } catch (Exception e) {
      log.error(
          "Retrieving Batches for experiment with id " + experimentId.value() + " failed: " + e);
      return Result.fromError(
          life.qbic.projectmanagement.application.batch.BatchInformationService.ResponseCode.BATCHES_NOT_FOUND);
    }
    return Result.fromValue(batches);
  }

}
