package life.qbic.projectmanagement.domain.project.service;

import java.util.Objects;
import life.qbic.application.commons.Result;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.domain.project.experiment.BiologicalReplicateId;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.repository.SampleRepository;
import life.qbic.projectmanagement.domain.project.sample.BatchId;
import life.qbic.projectmanagement.domain.project.sample.Sample;
import life.qbic.projectmanagement.domain.project.sample.SampleOrigin;
import life.qbic.projectmanagement.domain.project.sample.event.SampleRegistered;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b>Sample Domain Service</b>
 * <p>
 * Service that handles entity creation and deletion events, that need to dispatch domain events.
 *
 * @since 1.0.0
 */
@Service
public class SampleDomainService {

  private final SampleRepository sampleRepository;

  @Autowired
  public SampleDomainService(SampleRepository sampleRepository) {
    this.sampleRepository = Objects.requireNonNull(sampleRepository);
  }

  /**
   * Registers a new sample for measurement.
   *
   * @param label                 the end-user determined sample label
   * @param assignedBatch         the assigned logical sample batch, the sample will be prepared
   *                              within
   * @param experimentId          the associated experiment
   * @param experimentalGroupId   the associated experiment group
   * @param biologicalReplicateId the associated biological replicate
   * @param sampleOrigin          sample origin information
   * @return A {@link Result} object with the created {@link Sample} or an error with
   * {@link ResponseCode}
   * @since 1.0.0
   */
  public Result<Sample, ResponseCode> registerSample(String label, BatchId assignedBatch,
      ExperimentId experimentId,
      long experimentalGroupId, BiologicalReplicateId biologicalReplicateId,
      SampleOrigin sampleOrigin) {
    var sample = Sample.create(label, assignedBatch, experimentId, experimentalGroupId,
        biologicalReplicateId,
        sampleOrigin);
    Result<Sample, ResponseCode> result = this.sampleRepository.add(sample);

    // For successful registration transactions we dispatch the event
    result.onValue(createdSample -> DomainEventDispatcher.instance()
        .dispatch(
            SampleRegistered.create(createdSample.assignedBatch(), createdSample.sampleId())));

    return result;
  }

  public enum ResponseCode {
    REGISTRATION_FAILED
  }

}
