package life.qbic.projectmanagement.infrastructure.sample;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import life.qbic.projectmanagement.domain.repository.ProjectRepository;
import life.qbic.projectmanagement.domain.repository.SampleRepository;
import life.qbic.projectmanagement.domain.service.SampleDomainService.ResponseCode;
import life.qbic.projectmanagement.infrastructure.sample.openbis.OpenbisConnector.SampleNotDeletedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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
  private final ProjectRepository projectRepository;

  @Autowired
  public SampleRepositoryImpl(QbicSampleRepository qbicSampleRepository,
      QbicSampleDataRepo sampleDataRepo, ProjectRepository projectRepository) {
    this.qbicSampleRepository = Objects.requireNonNull(qbicSampleRepository);
    this.sampleDataRepo = Objects.requireNonNull(sampleDataRepo);
    this.projectRepository = Objects.requireNonNull(projectRepository);
  }

  @Override
  public Result<Collection<Sample>, ResponseCode> addAll(Project project,
      Collection<Sample> samples) {
    String commaSeperatedSampleIds = buildCommaSeparatedSampleIds(
        samples.stream().map(Sample::sampleId).toList());
    List<Sample> savedSamples;
    try {
      savedSamples = this.qbicSampleRepository.saveAll(samples);
    } catch (Exception e) {
      log.error("The samples:" + commaSeperatedSampleIds + "could not be saved", e);
      return Result.fromError(ResponseCode.REGISTRATION_FAILED);
    }
    try {
      sampleDataRepo.addSamplesToProject(project, savedSamples);
    } catch (Exception e) {
      log.error("The samples:" + commaSeperatedSampleIds + "could not be stored in openBIS", e);
      log.error("Removing samples from repository, as well.");
      qbicSampleRepository.deleteAll(savedSamples);
      return Result.fromError(ResponseCode.REGISTRATION_FAILED);
    }
    return Result.fromValue(savedSamples);
  }

  @Override
  public Result<Collection<Sample>, ResponseCode> addAll(ProjectId projectId, Collection<Sample> samples) {
    var projectQuery = projectRepository.find(projectId);
    if (projectQuery.isPresent()) {
      return addAll(projectQuery.get(), samples);
    }
    return Result.fromError(ResponseCode.REGISTRATION_FAILED);
  }

  private String buildCommaSeparatedSampleIds(Collection<SampleId> sampleIds) {
    return sampleIds.stream().map(SampleId::toString).collect(Collectors.joining(", "));
  }

  @Transactional
  @Override
  public void deleteAll(Project project,
      Collection<SampleId> samples) {
    List<SampleCode> sampleCodes = qbicSampleRepository.findAllById(samples)
        .stream().map(Sample::sampleCode).toList();
    this.qbicSampleRepository.deleteAllById(samples);
    try {
      sampleDataRepo.deleteAll(project.getProjectCode(), sampleCodes);
    } catch (SampleNotDeletedException sampleNotDeletedException) {
      throw new ApplicationException("Could not delete " + buildCommaSeparatedSampleIds(samples),
          sampleNotDeletedException,
          ErrorCode.DATA_ATTACHED_TO_SAMPLES, ErrorParameters.empty());
    }
  }

  @Override
  public boolean isSampleRemovable(SampleId sampleId) {
    SampleCode sampleCode = qbicSampleRepository.findById(sampleId).orElseThrow().sampleCode();
    return sampleDataRepo.canDeleteSample(sampleCode);
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
  public List<Sample> findSamplesByBatchId(
      BatchId batchId) {
    Objects.requireNonNull(batchId, "batchId must not be null");
    return qbicSampleRepository.findAllByAssignedBatch(batchId);
  }

  @Transactional
  @Override
  public void updateAll(Project project,
      Collection<Sample> updatedSamples) {
    qbicSampleRepository.saveAll(updatedSamples);
    sampleDataRepo.updateAll(project, updatedSamples);
  }

  @Transactional
  @Override
  public void updateAll(ProjectId projectId, Collection<Sample> updatedSamples) {
    var projectQuery = projectRepository.find(projectId);
    if (projectQuery.isPresent()) {
      updateAll(projectQuery.get(), updatedSamples);
    } else {
      throw new SampleRepositoryException("Could not find project with id " + projectId.value());
    }
  }

  @Override
  public List<Sample> findSamplesBySampleId(List<SampleId> sampleId) {
    return qbicSampleRepository.findAllById(sampleId);
  }

  @Override
  public Optional<Sample> findSample(SampleCode sampleCode) {
    return Optional.ofNullable(qbicSampleRepository.findBySampleCode(sampleCode));
  }

  @Override
  public Optional<Sample> findSample(SampleId sampleId) {
    return qbicSampleRepository.findById(sampleId);
  }

  @Override
  public long countSamplesWithExperimentId(ExperimentId experimentId) {
    return qbicSampleRepository.countAllByExperimentId(experimentId);
  }


}
