package life.qbic.projectmanagement.application.sample;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import life.qbic.application.commons.Result;
import life.qbic.domain.concepts.communication.CommunicationService;
import life.qbic.domain.concepts.communication.Content;
import life.qbic.domain.concepts.communication.Recipient;
import life.qbic.domain.concepts.communication.Subject;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.api.SampleCodeService;
import life.qbic.projectmanagement.domain.project.Contact;
import life.qbic.projectmanagement.domain.project.Project;
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
            notifyContacts(project.get(), result.getValue());
        }
        return result.onValue(Result::fromValue).flatMapError(responseCode -> Result.fromError(ResponseCode.SAMPLE_REGISTRATION_FAILED));
    }

    /**
     * Sends an email with attached spreadsheet to the contacts (PI, manager and contact person) of this project.
     * The email contains an explanation about sample identifiers and the spreadsheet the known metadata of the samples
     * @param project - the current project
     * @param samples - the samples that were registered
     */
    private void notifyContacts(Project project, Collection<Sample> samples) {
        String attachmentContent = prepareSpreadsheetContent(samples);

        notifyContact(project.getPrincipalInvestigator(), project, attachmentContent);
        notifyContact(project.getProjectManager(), project, attachmentContent);
        project.getResponsiblePerson().ifPresent(person -> notifyContact(person, project, attachmentContent));
    }

    private String prepareSpreadsheetContent(Collection<Sample> samples) {
        StringBuilder builder = new StringBuilder();
        List<String> header = Arrays.asList("Label", "Sample Code", "Replicate ID", "Origin",
            "Analysis Type", "Comment");
        builder.append(String.join("\t", header));
        builder.append("\n");
        for(Sample sample : samples) {
            List<String> row = new ArrayList<>();
            row.add(sample.label());
            row.add(sample.sampleCode().code());
            row.add(sample.biologicalReplicateId().toString());
            row.add(sample.sampleOrigin().toString());
            row.add(sample.analysisType().orElse(""));
            row.add(sample.comment().orElse(""));
            builder.append(String.join("\t", row));
            builder.append("\n");
        }
        return builder.toString();
    }

    private void notifyContact(Contact contact, Project project, String attachmentContent) {
        String subject = "New samples added to project";
        String projectUri = project.getId().toString();
        String projectTitle = project.getProjectIntent().projectTitle().title();

        var message = Messages.samplesAddedToProject(contact.fullName(), projectTitle, projectUri);

        communicationService.send(new Subject(subject),
            new Recipient(contact.emailAddress(), contact.fullName()), new Content(message));
    }

    public enum ResponseCode {
        SAMPLE_REGISTRATION_FAILED,
        NO_SAMPLES_DEFINED
    }

}
