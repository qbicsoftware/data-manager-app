package life.qbic.projectmanagement.application;

import java.util.Collection;
import java.util.Objects;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.repository.SampleRepository;
import life.qbic.projectmanagement.domain.project.sample.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * SampleInformationService
 * <p>
 * Service that provides an API to query sample information
 */
@Service
public class SampleInformationService {

  private static final Logger log = LoggerFactory.logger(SampleInformationService.class);
  private final SampleRepository sampleRepository;

  public SampleInformationService(@Autowired SampleRepository sampleRepository) {
    Objects.requireNonNull(sampleRepository);
    this.sampleRepository = sampleRepository;
  }

  public Result<Collection<Sample>, ResponseCode> retrieveSamplesForExperiment(
      ExperimentId experimentId) {
    Objects.requireNonNull(experimentId, "experiment id must not be null");
    return sampleRepository.findSamplesByExperimentId(experimentId);
  }

  public enum ResponseCode {
    SAMPLES_NOT_FOUND
  }
}
