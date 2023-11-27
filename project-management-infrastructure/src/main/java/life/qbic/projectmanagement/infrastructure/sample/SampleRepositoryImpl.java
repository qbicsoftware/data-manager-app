package life.qbic.projectmanagement.infrastructure.sample;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import life.qbic.projectmanagement.domain.repository.SampleRepository;
import life.qbic.projectmanagement.domain.service.SampleDomainService.ResponseCode;
import life.qbic.projectmanagement.infrastructure.sample.openbis.OpenbisConnector.SampleNotDeletedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;


/**
 * <b>Sample repository implementation</b>
 *
 * <p>Implementation for the {@link SampleRepository} interface.
 *
 * <p>This class serves as an adapter and proxies requests to an JPA implementation to interact
 * with persistent {@link Sample} data in the storage layer.
 *
 * <p>The actual JPA implementation is done by {@link QbicSampleRepository}, which is injected as
 * dependency upon creation.
 * <p>
 * Also handles project storage in openBIS through {@link QbicSampleDataRepo}
 *
 * @since 1.0.0
 */
@Service
public class SampleRepositoryImpl implements SampleRepository {

  private static final Logger log = logger(SampleRepositoryImpl.class);
  private final QbicSampleRepository qbicSampleRepository;
  private final QbicSampleDataRepo sampleDataRepo;

  @Autowired
  public SampleRepositoryImpl(QbicSampleRepository qbicSampleRepository,
      QbicSampleDataRepo sampleDataRepo) {
    this.qbicSampleRepository = qbicSampleRepository;
    this.sampleDataRepo = sampleDataRepo;
  }

  @Override
  public Result<Collection<Sample>, ResponseCode> addAll(Project project,
      Collection<Sample> samples) {
    String commaSeperatedSampleIds = buildCommaSeparatedSampleIds(samples);
    try {
      this.qbicSampleRepository.saveAll(samples);
    } catch (Exception e) {
      log.error("The samples:" + commaSeperatedSampleIds + "could not be saved", e);
      return Result.fromError(ResponseCode.REGISTRATION_FAILED);
    }
    try {
      sampleDataRepo.addSamplesToProject(project, samples.stream().toList());
    } catch (Exception e) {
      log.error("The samples:" + commaSeperatedSampleIds + "could not be stored in openBIS", e);
      log.error("Removing samples from repository, as well.");
      qbicSampleRepository.deleteAll(samples);
      return Result.fromError(ResponseCode.REGISTRATION_FAILED);
    }
    return Result.fromValue(samples);
  }

  private String buildCommaSeparatedSampleIds(Collection<Sample> samples) {
    return samples.stream().map(sample -> sample.sampleId().toString()).collect(
        Collectors.joining(", "));
  }

  @Transactional
  @Override
  public Result<Collection<Sample>, ResponseCode> deleteAll(Project project,
      Collection<Sample> samples) {
    String commaSeperatedSampleIds = buildCommaSeparatedSampleIds(samples);
    try {
      this.qbicSampleRepository.deleteAll(samples);
    } catch (Exception e) {
      log.error("The samples:" + commaSeperatedSampleIds + "could not be deleted", e);
      rollBackCurrentTransaction();
      return Result.fromError(ResponseCode.DELETION_FAILED);
    }
    try {
      sampleDataRepo.deleteAll(project.getProjectCode(), samples);
    } catch (SampleNotDeletedException sampleNotDeletedException) {
      log.error("The samples:" + commaSeperatedSampleIds
              + "could not be deleted from openBis since there is data attached to them",
          sampleNotDeletedException);
      rollBackCurrentTransaction();
      return Result.fromError(ResponseCode.DATA_ATTACHED_TO_SAMPLES);
    } catch (Exception e) {
      log.error("The samples:" + commaSeperatedSampleIds + "could not be deleted from openBIS", e);
      rollBackCurrentTransaction();
      return Result.fromError(ResponseCode.DELETION_FAILED);
    }
    return Result.fromValue(samples);
  }

  @Override
  public Result<Collection<Sample>, SampleInformationService.ResponseCode> findSamplesByExperimentId(
      ExperimentId experimentId) {
    Objects.requireNonNull(experimentId);
    Collection<Sample> samples;
    try {
      samples = qbicSampleRepository.findAllByExperimentId(experimentId);
    } catch (Exception e) {
      log.error(
          "Retrieving Samples for experiment with id " + experimentId.value() + " failed: " + e, e);
      return Result.fromError(SampleInformationService.ResponseCode.QUERY_FAILED);
    }
    return Result.fromValue(samples);
  }

  @Override
  public Result<Collection<Sample>, SampleInformationService.ResponseCode> findSamplesByBatchId(
      BatchId batchId) {
    Objects.requireNonNull(batchId);
    Collection<Sample> samples;
    try {
      samples = qbicSampleRepository.findAllByAssignedBatch(batchId);
    } catch (Exception e) {
      log.error(
          "Retrieving Samples for batch with id " + batchId.value() + " failed: " + e, e);
      return Result.fromError(SampleInformationService.ResponseCode.QUERY_FAILED);
    }
    return Result.fromValue(samples);
  }

  @Transactional
  @Override
  public Result<Collection<Sample>, ResponseCode> updateAll(Project project,
      Collection<Sample> updatedSamples) {
    String commaSeperatedSampleIds = buildCommaSeparatedSampleIds(updatedSamples);
    try {
      this.qbicSampleRepository.saveAll(updatedSamples);
    } catch (Exception e) {
      log.error("The samples:" + commaSeperatedSampleIds + "could not be updated", e);
      rollBackCurrentTransaction();
      return Result.fromError(ResponseCode.UPDATE_FAILED);
    }
    try {
      sampleDataRepo.updateAll(updatedSamples);
    } catch (Exception e) {
      log.error("The samples:" + commaSeperatedSampleIds + "could not be updated in openBIS", e);
      rollBackCurrentTransaction();
      return Result.fromError(ResponseCode.UPDATE_FAILED);
    }
    return Result.fromValue(updatedSamples);
  }


  /**
   * Spring Transactional annotation based rollback only works if the current transaction throws an
   * exception. Since we want to return result objects instead of throwing exceptions we have to
   * programmatically mark the current transaction as rollback to ensure data validity.
   */
  private void rollBackCurrentTransaction() {
    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
  }

}
