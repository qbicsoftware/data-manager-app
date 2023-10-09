package life.qbic.projectmanagement.application.policy.directive;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.domain.concepts.communication.Attachment;
import life.qbic.domain.concepts.communication.CommunicationService;
import life.qbic.domain.concepts.communication.Content;
import life.qbic.domain.concepts.communication.Recipient;
import life.qbic.domain.concepts.communication.Subject;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.Messages;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.sample.Sample;
import life.qbic.projectmanagement.domain.project.sample.event.SampleBatchRegistered;
import life.qbic.user.api.UserInformationService;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Component;

/**
 * <b>Directive: Send email to project users about new sample batch</b>
 * <p>
 * After a sample batch has been registered, we need to inform users about
 * the newly registered samples.
 *
 * @since 1.0.0
 */
@Component
public class InformUsersAboutBatchRegistration implements DomainEventSubscriber<SampleBatchRegistered> {

  private static final Logger log = logger(InformUsersAboutBatchRegistration.class);
  private final CommunicationService communicationService;
  private final ProjectAccessService projectAccessService;
  private final UserInformationService userInformationService;
  private final JobScheduler jobScheduler;

  public InformUsersAboutBatchRegistration(CommunicationService communicationService,
      ProjectAccessService projectAccessService, UserInformationService userInformationService, JobScheduler jobScheduler) {
    this.projectAccessService = projectAccessService;
    this.userInformationService = userInformationService;
    this.communicationService = communicationService;
    this.jobScheduler = jobScheduler;
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return SampleBatchRegistered.class;
  }

  @Override
  public void handleEvent(SampleBatchRegistered event) {
    jobScheduler.enqueue(() -> notifyUsersAboutSamples(event.project(), event.samples()));
  }

  @Job(name = "Notify users about batch registration")
  /**
   * Sends an email with attached spreadsheet of newly registered samples to the users of a project.
   * The email contains an explanation about sample identifiers and the spreadsheet the known metadata of the samples
   * @param project - the project the sample batch was added to
   * @param sampleBatch - the samples that were registered
   */
  public void notifyUsersAboutSamples(Project project, Collection<Sample> sampleBatch) {
    List<RecipientDTO> recipientsWithAccess = getUsersWithAccess(project.getId());
    String attachmentContent = prepareSpreadsheetContent(sampleBatch);
    for(RecipientDTO recipient : recipientsWithAccess) {
        notifyRecipient(recipient, project, attachmentContent);
    }
  }

  private List<RecipientDTO> getUsersWithAccess(ProjectId projectId) {
    List<RecipientDTO> users = new ArrayList<>();
    List<String> userIds = projectAccessService.listActiveUsers(projectId);
    for(String id : userIds) {
      userInformationService.findById(id).ifPresent(userInfo -> users.add(new RecipientDTO(userInfo)));
    }
    return users;
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
      row.add(sample.analysisMethod().description());
      row.add(sample.comment().orElse(""));
      builder.append(String.join("\t", row));
      builder.append("\n");
    }
    return builder.toString();
  }

  private void notifyRecipient(RecipientDTO recipient, Project project, String attachmentContent) {
    String subject = "New samples added to project";
    String projectUri = project.getId().toString();
    String projectTitle = project.getProjectIntent().projectTitle().title();

    var message = Messages.samplesAddedToProject(recipient.getFullName(), projectTitle, projectUri);

    communicationService.send(new Subject(subject),
        new Recipient(recipient.getEmailAddress(), recipient.getFullName()), new Content(message),
        new Attachment(attachmentContent, "sample_sheet.tsv"));
  }

}
