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
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.domain.model.batch.BatchId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleCode;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import life.qbic.projectmanagement.domain.repository.ProjectRepository;
import life.qbic.projectmanagement.domain.repository.SampleRepository;
import life.qbic.projectmanagement.infrastructure.sample.openbis.OpenbisConnector.SampleNotDeletedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
 * <p>The actual JPA implementation is done by {@link SampleJpaRepository}, which is injected as
 * dependency upon creation.
 * <p>
 * Also handles project storage in openBIS through {@link SampleDataRepository}
 *
 * @since 1.0.0
 */
@Service
public class SampleRepositoryImpl implements SampleRepository {

  private static final Logger log = logger(SampleRepositoryImpl.class);
  private final SampleJpaRepository sampleJpaRepository;
  private final SampleDataRepository sampleDataRepository;
  private final ProjectRepository projectRepository;

  private SampleRepository selfProxy;

  @Autowired
  public SampleRepositoryImpl(SampleJpaRepository sampleJpaRepository,
      SampleDataRepository sampleDataRepository, ProjectRepository projectRepository,
      @Lazy SampleRepository selfProxy) {
    this.selfProxy = selfProxy;
    this.sampleJpaRepository = Objects.requireNonNull(sampleJpaRepository);
    this.sampleDataRepository = Objects.requireNonNull(sampleDataRepository);
    this.projectRepository = Objects.requireNonNull(projectRepository);
  }

  @Transactional(propagation = Propagation.REQUIRED)
  @Override
  public Collection<Sample> addAll(Project project,
      Collection<Sample> samples) {
    String commaSeperatedSampleIds = buildCommaSeparatedSampleIds(
        samples.stream().map(Sample::sampleId).toList());
    Object savepoint = TransactionAspectSupport.currentTransactionStatus().createSavepoint();
    List<Sample> savedSamples = this.sampleJpaRepository.saveAll(samples);
    try {
      sampleDataRepository.addSamplesToProject(project, savedSamples);
    } catch (Exception e) {
      TransactionAspectSupport.currentTransactionStatus().rollbackToSavepoint(savepoint);
      throw new ApplicationException(
          "The samples:" + commaSeperatedSampleIds + "could not be stored in openBIS", e);
    }
    return savedSamples;
  }

  @Transactional
  @Override
  public Collection<Sample> addAll(ProjectId projectId, Collection<Sample> samples) {
    var projectQuery = projectRepository.find(projectId);
    if (projectQuery.isEmpty()) {
      throw new IllegalArgumentException("Project not found: " + projectId);
    }
    return selfProxy.addAll(projectQuery.get(), samples);
  }

  private String buildCommaSeparatedSampleIds(Collection<SampleId> sampleIds) {
    return sampleIds.stream().map(SampleId::toString).collect(Collectors.joining(", "));
  }

  @Transactional
  @Override
  public void deleteAll(Project project,
      Collection<SampleId> samples) {
    Object savepoint = TransactionAspectSupport.currentTransactionStatus().createSavepoint();
    List<SampleCode> sampleCodes = sampleJpaRepository.findAllById(samples)
        .stream().map(Sample::sampleCode).toList();
    this.sampleJpaRepository.deleteAllById(samples);
    try {
      sampleDataRepository.deleteAll(project.getProjectCode(), sampleCodes);
    } catch (SampleNotDeletedException sampleNotDeletedException) {
      TransactionAspectSupport.currentTransactionStatus().rollbackToSavepoint(savepoint);
      throw new ApplicationException("Could not delete " + buildCommaSeparatedSampleIds(samples),
          sampleNotDeletedException, ErrorCode.DATA_ATTACHED_TO_SAMPLES, ErrorParameters.empty());
    } catch (Exception e) {
      TransactionAspectSupport.currentTransactionStatus().rollbackToSavepoint(savepoint);
      throw new ApplicationException("Could not delete " + buildCommaSeparatedSampleIds(samples),
          e);
    }
  }

  @Transactional(readOnly = true)
  @Override
  public boolean isSampleRemovable(SampleId sampleId) {
    SampleCode sampleCode = sampleJpaRepository.findById(sampleId).orElseThrow().sampleCode();
    return sampleDataRepository.canDeleteSample(sampleCode);
  }

  @Transactional(readOnly = true)
  @Override
  public Collection<Sample> findSamplesByExperimentId(ExperimentId experimentId) {
    Objects.requireNonNull(experimentId);
    Collection<Sample> samples;
    return sampleJpaRepository.findAllByExperimentId(experimentId);
  }

  @Override
  public List<Sample> findSamplesByBatchId(
      BatchId batchId) {
    Objects.requireNonNull(batchId, "batchId must not be null");
    return sampleJpaRepository.findAllByAssignedBatch(batchId);
  }

  @Transactional
  @Override
  public void updateAll(Project project,
      Collection<Sample> updatedSamples) {
    sampleJpaRepository.saveAll(updatedSamples);
    sampleDataRepository.updateAll(project, updatedSamples);
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
    return sampleJpaRepository.findAllById(sampleId);
  }

  @Override
  public Optional<Sample> findSample(SampleCode sampleCode) {
    return Optional.ofNullable(sampleJpaRepository.findBySampleCode(sampleCode));
  }

  @Override
  public Optional<Sample> findSample(SampleId sampleId) {
    return sampleJpaRepository.findById(sampleId);
  }

  @Override
  public long countSamplesWithExperimentId(ExperimentId experimentId) {
    return sampleJpaRepository.countAllByExperimentId(experimentId);
  }


}
