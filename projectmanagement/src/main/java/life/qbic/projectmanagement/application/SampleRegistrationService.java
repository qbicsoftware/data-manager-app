package life.qbic.projectmanagement.application;

import java.util.Objects;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.application.api.SampleCodeService;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.repository.SampleRepository;
import life.qbic.projectmanagement.domain.project.sample.Sample;
import life.qbic.projectmanagement.domain.project.sample.SampleRegistrationRequest;
import life.qbic.projectmanagement.domain.project.service.SampleDomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Sample Registration Service
 * <p>
 * Application service allowing for retrieving the information necessary for sample registration
 */
@Service
public class SampleRegistrationService {

  private final SampleRepository sampleRepository;

  private final SampleCodeService sampleCodeService;

  private final SampleDomainService sampleDomainService;

  @Autowired
  public SampleRegistrationService(SampleRepository sampleRepository,
      SampleCodeService sampleCodeService, SampleDomainService sampleDomainService) {
    this.sampleRepository = Objects.requireNonNull(sampleRepository);
    this.sampleCodeService = Objects.requireNonNull(sampleCodeService);
    this.sampleDomainService = Objects.requireNonNull(sampleDomainService);
  }

  public Result<Sample, ResponseCode> registerSample(
      SampleRegistrationRequest sampleRegistrationRequest, ProjectId projectId) {
    var result = sampleCodeService.generateFor(projectId);
    if (result.isError()) {
      return Result.fromError(ResponseCode.SAMPLE_REGISTRATION_FAILED);
    }
    var registration = sampleDomainService.registerSample(result.getValue(),
        sampleRegistrationRequest);
    if (registration.isValue()) {
      return Result.fromValue(registration.getValue());
    }
    return Result.fromError(ResponseCode.SAMPLE_REGISTRATION_FAILED);
  }

  public enum ResponseCode {
    SAMPLE_REGISTRATION_FAILED
  }

}
