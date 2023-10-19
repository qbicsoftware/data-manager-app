package life.qbic.projectmanagement.application.policy.directive;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.ArrayList;
import java.util.List;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.domain.concepts.communication.CommunicationService;
import life.qbic.domain.concepts.communication.Content;
import life.qbic.domain.concepts.communication.Recipient;
import life.qbic.domain.concepts.communication.Subject;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.AppContextProvider;
import life.qbic.projectmanagement.application.Messages;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.event.BatchRegistered;
import life.qbic.identity.api.UserInformationService;
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
  private final AppContextProvider appContextProvider;
  private final JobScheduler jobScheduler;

  public InformUsersAboutBatchRegistration(CommunicationService communicationService,
      ProjectAccessService projectAccessService, UserInformationService userInformationService,
      AppContextProvider appContextProvider, JobScheduler jobScheduler) {
    this.projectAccessService = projectAccessService;
    this.userInformationService = userInformationService;
    this.communicationService = communicationService;
    this.appContextProvider = appContextProvider;
    this.jobScheduler = jobScheduler;
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return BatchRegistered.class;
  }

  @Override
  public void handleEvent(BatchRegistered event) {
    jobScheduler.enqueue(() -> notifyUsersAboutSamples(event.name(), event.projectTitle(),
        event.projectId()));
  }

  @Job(name = "Notify users about batch registration")
  /**
   * Sends an email with attached spreadsheet of newly registered samples to the users of a project.
   * The email contains an explanation about sample identifiers and the spreadsheet the known metadata of the samples
   * @param name - the name of the batch
   * @param projectTitle - the name of the project the batch was added to
   * @param projectId - the id of the project
   */
  public void notifyUsersAboutSamples(String name, String projectTitle, ProjectId projectId) {
    List<RecipientDTO> recipients = getRecipients(projectId);
    String sampleUri = appContextProvider.urlToSamplePage(projectId.value());
    for(RecipientDTO recipient : recipients) {
        notifyRecipient(recipient, projectTitle, name, sampleUri);
    }
  }

  private List<RecipientDTO> getRecipients(ProjectId projectId) {
    List<RecipientDTO> users = new ArrayList<>();
    List<String> userIds = projectAccessService.listActiveUserIds(projectId);
    for(String id : userIds) {
      userInformationService.findById(id).ifPresent(userInfo -> users.add(new RecipientDTO(userInfo)));
    }
    return users;
  }

  private void notifyRecipient(RecipientDTO recipient, String projectTitle, String batchName,
      String sampleUri) {
    String subject = "New samples added to project";

    var message = Messages.samplesAddedToProject(recipient.getFullName(), projectTitle,
        batchName, sampleUri);

    communicationService.send(new Subject(subject),
        new Recipient(recipient.getEmailAddress(), recipient.getFullName()), new Content(message));
  }

}
