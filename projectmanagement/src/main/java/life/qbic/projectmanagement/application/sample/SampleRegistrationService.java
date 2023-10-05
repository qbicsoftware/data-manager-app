package life.qbic.projectmanagement.application.sample;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import life.qbic.application.commons.Result;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.domain.concepts.communication.CommunicationService;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.api.SampleCodeService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.sample.Sample;
import life.qbic.projectmanagement.domain.project.sample.SampleCode;
import life.qbic.projectmanagement.domain.project.sample.SampleRegistrationRequest;
import life.qbic.projectmanagement.domain.project.sample.event.SampleBatchRegistered;
import life.qbic.projectmanagement.domain.project.sample.event.SampleRegistered;
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
    private final CommunicationService communicationService;
    private static final Logger log = logger(SampleRegistrationService.class);

    @Autowired
    public SampleRegistrationService(SampleCodeService sampleCodeService,
                                     SampleDomainService sampleDomainService,
        ProjectInformationService projectInformationService, CommunicationService communicationService) {
        this.sampleCodeService = Objects.requireNonNull(sampleCodeService);
        this.sampleDomainService = Objects.requireNonNull(sampleDomainService);
        this.projectInformationService = Objects.requireNonNull(projectInformationService);
        this.communicationService = Objects.requireNonNull(communicationService);
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
        var result = sampleDomainService.registerSamples(project.get(), sampleCodesToRegistrationRequests);
        if(result.isValue()) {
            dispatchSuccessfulSampleBatchRegistration(project.get(), result.getValue());
        }
        return result.onValue(Result::fromValue).flatMapError(responseCode -> Result.fromError(ResponseCode.SAMPLE_REGISTRATION_FAILED));
    }

    private void dispatchSuccessfulSampleBatchRegistration(Project project, Collection<Sample> samples) {
        SampleBatchRegistered sampleBatchRegistered = SampleBatchRegistered.create(project, samples);
        DomainEventDispatcher.instance().dispatch(sampleBatchRegistered);
    }

    public enum ResponseCode {
        SAMPLE_REGISTRATION_FAILED,
        NO_SAMPLES_DEFINED
    }

}
