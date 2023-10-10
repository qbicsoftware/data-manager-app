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
import life.qbic.projectmanagement.domain.project.sample.event.BatchRegistered;
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
public class InformUsersAboutBatchRegistration implements DomainEventSubscriber<BatchRegistered> {

  private static final Logger log = logger(InformUsersAboutBatchRegistration.class);
  private final CommunicationService communicationService;
  private final ProjectAccessService projectAccessService;
  private final UserInformationService userInformationService;
  private final JobScheduler jobScheduler;

  public InformUsersAboutBatchRegistration(CommunicationService communicationService,
      ProjectAccessService projectAccessService, UserInformationService userInformationService,
      JobScheduler jobScheduler) {
    this.projectAccessService = projectAccessService;
    this.userInformationService = userInformationService;
    this.communicationService = communicationService;
    this.jobScheduler = jobScheduler;
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return BatchRegistered.class;
  }

  @Override
  public void handleEvent(BatchRegistered event) {
    jobScheduler.enqueue(() -> notifyUsersAboutSamples(event.project(), event.name()));
  }

  @Job(name = "Notify users about batch registration")
  /**
   * Sends an email with attached spreadsheet of newly registered samples to the users of a project.
   * The email contains an explanation about sample identifiers and the spreadsheet the known metadata of the samples
   * @param project - the project the sample batch was added to
   * @param name - the name of the sample batch
   */
  public void notifyUsersAboutSamples(Project project, String name) {
    List<RecipientDTO> recipients = getRecipients(project.getId());
    for(RecipientDTO recipient : recipients) {
        notifyRecipient(recipient, project, name);
    }
  }

  private List<RecipientDTO> getRecipients(ProjectId projectId) {
    List<RecipientDTO> users = new ArrayList<>();
    List<String> userIds = projectAccessService.listActiveUsers(projectId);
    for(String id : userIds) {
      userInformationService.findById(id).ifPresent(userInfo -> users.add(new RecipientDTO(userInfo)));
    }
    return users;
  }

  private void notifyRecipient(RecipientDTO recipient, Project project, String batchName) {
    String subject = "New samples added to project";
    String projectUri = project.getId().toString();
    String projectTitle = project.getProjectIntent().projectTitle().title();

    var message = Messages.samplesAddedToProject(recipient.getFullName(), projectTitle,
        projectUri, batchName);

    communicationService.send(new Subject(subject),
        new Recipient(recipient.getEmailAddress(), recipient.getFullName()), new Content(message));
  }

}
