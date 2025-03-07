package life.qbic.projectmanagement.application.policy.directive;

import java.util.ArrayList;
import java.util.List;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.identity.api.UserInformationService;
import life.qbic.logging.api.Logger;
import static life.qbic.logging.service.LoggerFactory.logger;
import life.qbic.projectmanagement.application.AppContextProvider;
import life.qbic.projectmanagement.application.Messages;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectCollaborator;
import life.qbic.projectmanagement.application.communication.Content;
import life.qbic.projectmanagement.application.communication.EmailService;
import life.qbic.projectmanagement.application.communication.Recipient;
import life.qbic.projectmanagement.application.communication.Subject;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.event.BatchRegistered;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * <b>Directive: Send email to project users about new sample batch</b>
 * <p>
 * After a sample batch has been registered, we need to inform users about the newly registered
 * samples.
 *
 * @since 1.0.0
 */
@Component
public class InformUsersAboutBatchRegistration implements DomainEventSubscriber<BatchRegistered> {

  private static final Logger log = logger(InformUsersAboutBatchRegistration.class);
  private final EmailService emailService;
  private final ProjectAccessService projectAccessService;
  private final UserInformationService userInformationService;
  private final AppContextProvider appContextProvider;
  private final JobScheduler jobScheduler;

  public InformUsersAboutBatchRegistration(EmailService emailService,
      ProjectAccessService projectAccessService, UserInformationService userInformationService,
      AppContextProvider appContextProvider, JobScheduler jobScheduler) {
    this.projectAccessService = projectAccessService;
    this.userInformationService = userInformationService;
    this.emailService = emailService;
    this.appContextProvider = appContextProvider;
    this.jobScheduler = jobScheduler;
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return BatchRegistered.class;
  }

  @Override
  @PreAuthorize("hasPermission(#projectId, 'life.qbic.projectmanagement.domain.model.project.Project', 'WRITE')")
  public void handleEvent(BatchRegistered event) {
    List<RecipientDTO> recipients = getRecipients(event.projectId());
    String sampleUri = appContextProvider.urlToSamplePage(event.projectId().value(),
        event.experimentId().value());
    notifyAllRecipients(recipients, event.projectTitle(), event.name(), sampleUri);
  }

  public void notifyAllRecipients(List<RecipientDTO> recipients, String projectTitle,
      String batchName, String sampleUri) {
    for (RecipientDTO recipient : recipients) {
      jobScheduler.enqueue(
          () -> notifyRecipient(recipient.getEmailAddress(), recipient.getFullName(), projectTitle,
              batchName, sampleUri));
    }
  }

  private List<RecipientDTO> getRecipients(ProjectId projectId) {
    List<RecipientDTO> users = new ArrayList<>();
    var userIds = projectAccessService.listCollaborators(projectId).stream()
        .map(ProjectCollaborator::userId)
        .toList();
    for (String id : userIds) {
      userInformationService.findById(id)
          .ifPresent(userInfo -> users.add(new RecipientDTO(userInfo)));
    }
    return users;
  }

  @Job(name = "Notify users about batch registration of batch %3 in project %2")
  public void notifyRecipient(String emailAddress, String fullName, String projectTitle, String batchName,

      String sampleUri) {
    String subject = "New samples added to project";

    var message = Messages.samplesAddedToProject(fullName, projectTitle,
        batchName, sampleUri);

    emailService.send(new Subject(subject),
        new Recipient(emailAddress, fullName), new Content(message));
  }

}
