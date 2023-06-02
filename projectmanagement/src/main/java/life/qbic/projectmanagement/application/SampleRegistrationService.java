package life.qbic.projectmanagement.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.application.api.SampleCodeService;
import life.qbic.projectmanagement.domain.project.ProjectId;
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

  private final SampleCodeService sampleCodeService;
  private final SampleDomainService sampleDomainService;

  @Autowired
  public SampleRegistrationService(SampleCodeService sampleCodeService,
      SampleDomainService sampleDomainService) {
    this.sampleCodeService = Objects.requireNonNull(sampleCodeService);
    this.sampleDomainService = Objects.requireNonNull(sampleDomainService);
  }

  public Result<List<Sample>, ResponseCode> registerSamples(
      List<SampleRegistrationRequest> sampleRegistrationRequests, ProjectId projectId) {
    Objects.requireNonNull(sampleRegistrationRequests);
    if (sampleRegistrationRequests.size() < 1) {
      return Result.fromError(ResponseCode.NO_SAMPLES_DEFINED);
    }
    List<Sample> registeredSamples = new ArrayList<>();
    for (SampleRegistrationRequest sampleRegistrationRequest : sampleRegistrationRequests) {
      var result = sampleCodeService.generateFor(projectId);
      if (result.isError()) {
        return Result.fromError(ResponseCode.SAMPLE_REGISTRATION_FAILED);
      }
      var registration = sampleDomainService.registerSample(result.getValue(),
          sampleRegistrationRequest);
      if (registration.isValue()) {
        registeredSamples.add(registration.getValue());
      } else {
        return Result.fromError(ResponseCode.SAMPLE_REGISTRATION_FAILED);
      }
    }
    return Result.fromValue(registeredSamples);
  }

  public enum ResponseCode {
    SAMPLE_REGISTRATION_FAILED,
    NO_SAMPLES_DEFINED
  }

}
