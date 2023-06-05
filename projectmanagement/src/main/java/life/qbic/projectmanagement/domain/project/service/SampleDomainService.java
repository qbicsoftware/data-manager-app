package life.qbic.projectmanagement.domain.project.service;

import java.util.Objects;
import life.qbic.application.commons.Result;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.projectmanagement.domain.project.repository.SampleRepository;
import life.qbic.projectmanagement.domain.project.sample.Sample;
import life.qbic.projectmanagement.domain.project.sample.SampleCode;
import life.qbic.projectmanagement.domain.project.sample.SampleRegistrationRequest;
import life.qbic.projectmanagement.domain.project.sample.event.SampleRegistered;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <b>Sample Domain Service</b>
 * <p>
 * Service that handles {@link Sample} creation and deletion events, that need to dispatch domain
 * events.
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
  public Result<Sample, ResponseCode> registerSample(SampleCode sampleCode,
      SampleRegistrationRequest sampleRegistrationRequest) {
    Objects.requireNonNull(sampleCode);
    Objects.requireNonNull(sampleRegistrationRequest);
    var sample = Sample.create(sampleCode, sampleRegistrationRequest);
    Result<Sample, ResponseCode> result = this.sampleRepository.add(sample);

    // For successful registration transactions we dispatch the event
    result.onValue(createdSample -> DomainEventDispatcher.instance()
        .dispatch(
            SampleRegistered.create(createdSample.assignedBatch(), createdSample.sampleId())));

    return result;
  }

  /**
   * Response error codes for the sample registration
   *
   * @since 1.0.0
   */
  public enum ResponseCode {
    REGISTRATION_FAILED
  }

}
