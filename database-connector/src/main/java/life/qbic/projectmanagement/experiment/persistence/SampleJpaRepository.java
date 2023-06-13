package life.qbic.projectmanagement.experiment.persistence;

import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.SampleInformationService;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.repository.SampleRepository;
import life.qbic.projectmanagement.domain.project.sample.Sample;
import life.qbic.projectmanagement.domain.project.sample.SampleId;
import life.qbic.projectmanagement.domain.project.service.SampleDomainService.ResponseCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import static life.qbic.logging.service.LoggerFactory.logger;

/**
 * <b>Sample JPA Repository</b>
 *
 * <p>Implementation of the {@link SampleRepository} interface</p>
 *
 * @since 1.0.0
 */
@Repository
public class SampleJpaRepository implements SampleRepository {

  private static final Logger log = logger(SampleJpaRepository.class);
  private final QbicSampleRepository qbicSampleRepository;

  @Autowired
  public SampleJpaRepository(QbicSampleRepository qbicSampleRepository) {
    this.qbicSampleRepository = qbicSampleRepository;
  }

  @Override
  public Result<Sample, ResponseCode> add(Sample sample) {
    try {
      this.qbicSampleRepository.save(sample);
    } catch (Exception e) {
      log.error("Saving sample with id failed:" + sample.sampleId(), e);
      return Result.fromError(ResponseCode.REGISTRATION_FAILED);
    }
    return Result.fromValue(sample);
  }

  @Override
  public Result<Collection<Sample>, ResponseCode> addAll(Collection<Sample> samples) {
    try {
      this.qbicSampleRepository.saveAll(samples);
    } catch (Exception e) {
      Collection<SampleId> failedSamples = new ArrayList<>();
      samples.forEach(sample -> failedSamples.add(sample.sampleId()));
      String commaSeperatedSampleIds = failedSamples.stream().map(Object::toString).collect(Collectors.joining(", "));
      log.error("The samples:" + commaSeperatedSampleIds + "could not be saved", e);
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
          "Retrieving Samples for experiment with id " + experimentId.value() + " failed: " + e);
      return Result.fromError(SampleInformationService.ResponseCode.SAMPLES_NOT_FOUND);
    }
    return Result.fromValue(samples);
  }
}
