package life.qbic.projectmanagement.persistence;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.repository.SampleRepository;
import life.qbic.projectmanagement.domain.project.sample.Sample;
import life.qbic.projectmanagement.domain.project.sample.SampleId;
import life.qbic.projectmanagement.domain.project.service.SampleDomainService.ResponseCode;
import life.qbic.projectmanagement.experiment.persistence.QbicSampleDataRepo;
import life.qbic.projectmanagement.experiment.persistence.QbicSampleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


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
    Collection<SampleId> sampleIds = new ArrayList<>();
    samples.forEach(sample -> sampleIds.add(sample.sampleId()));
    String commaSeperatedSampleIds = sampleIds.stream().map(Object::toString).collect(
        Collectors.joining(", "));
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
      samples.forEach(sample -> qbicSampleRepository.delete(sample));
      return Result.fromError(ResponseCode.REGISTRATION_FAILED);
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

}
