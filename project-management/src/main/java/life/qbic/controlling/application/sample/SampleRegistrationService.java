package life.qbic.controlling.application.sample;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import life.qbic.application.commons.Result;
import life.qbic.logging.api.Logger;
import life.qbic.controlling.application.ProjectInformationService;
import life.qbic.controlling.application.api.SampleCodeService;
import life.qbic.controlling.domain.model.project.ProjectId;
import life.qbic.controlling.domain.model.sample.Sample;
import life.qbic.controlling.domain.model.sample.SampleCode;
import life.qbic.controlling.domain.model.sample.SampleRegistrationRequest;
import life.qbic.controlling.domain.service.SampleDomainService;
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
    private static final Logger log = logger(SampleRegistrationService.class);

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
            log.error("Sample registration aborted. Reason: project with id:"+projectId+" was not found");
            return Result.fromError(ResponseCode.SAMPLE_REGISTRATION_FAILED);
        }
        if (sampleRegistrationRequests.isEmpty()) {
            log.error("No samples were defined");
            return Result.fromError(ResponseCode.NO_SAMPLES_DEFINED);
        }
        Map<SampleCode, SampleRegistrationRequest> sampleCodesToRegistrationRequests = new HashMap<>();
        sampleRegistrationRequests.forEach(sampleRegistrationRequest -> sampleCodeService.generateFor(projectId)
                .onValue(sampleCode -> sampleCodesToRegistrationRequests.put(sampleCode, sampleRegistrationRequest))
                .onError(responseCode -> Result.fromError(ResponseCode.SAMPLE_REGISTRATION_FAILED)));
        var result = sampleDomainService.registerSamples(project.get(),
            sampleCodesToRegistrationRequests);
        return result.onValue(Result::fromValue).flatMapError(responseCode -> Result.fromError(ResponseCode.SAMPLE_REGISTRATION_FAILED));
    }

    public enum ResponseCode {
        SAMPLE_REGISTRATION_FAILED,
        NO_SAMPLES_DEFINED
    }

}