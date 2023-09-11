package life.qbic.projectmanagement.application.sample;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import life.qbic.application.commons.Result;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.api.SampleCodeService;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.sample.Sample;
import life.qbic.projectmanagement.domain.project.sample.SampleCode;
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
    private final ProjectInformationService projectInformationService;

    @Autowired
    public SampleRegistrationService(SampleCodeService sampleCodeService,
                                     SampleDomainService sampleDomainService,
        ProjectInformationService projectInformationService) {
        this.sampleCodeService = Objects.requireNonNull(sampleCodeService);
        this.sampleDomainService = Objects.requireNonNull(sampleDomainService);
        this.projectInformationService = Objects.requireNonNull(projectInformationService);
    }

    public Result<Collection<Sample>, ResponseCode> registerSamples(
            Collection<SampleRegistrationRequest> sampleRegistrationRequests, ProjectId projectId) {
        Objects.requireNonNull(sampleRegistrationRequests);
        Objects.requireNonNull(projectId);
        var project = projectInformationService.find(projectId);
        if (project.isEmpty()) {
            return Result.fromError(ResponseCode.SAMPLE_REGISTRATION_FAILED);
        }
        if (sampleRegistrationRequests.isEmpty()) {
            return Result.fromError(ResponseCode.NO_SAMPLES_DEFINED);
        }
        Map<SampleCode, SampleRegistrationRequest> sampleCodesToRegistrationRequests = new HashMap<>();
        sampleRegistrationRequests.forEach(sampleRegistrationRequest -> sampleCodeService.generateFor(projectId)
                .onValue(sampleCode -> sampleCodesToRegistrationRequests.put(sampleCode, sampleRegistrationRequest))
                .onError(responseCode -> Result.fromError(ResponseCode.SAMPLE_REGISTRATION_FAILED)));
        var result = sampleDomainService.registerSamples(project.get(), sampleCodesToRegistrationRequests);
        return result.onValue(Result::fromValue).flatMapError(responseCode -> Result.fromError(ResponseCode.SAMPLE_REGISTRATION_FAILED));
    }

    public enum ResponseCode {
        SAMPLE_REGISTRATION_FAILED,
        NO_SAMPLES_DEFINED
    }

}
