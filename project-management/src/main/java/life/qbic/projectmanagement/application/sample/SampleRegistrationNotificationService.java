package life.qbic.projectmanagement.application.sample;

import java.util.ArrayList;
import java.util.List;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.projectmanagement.application.AppContextProvider;
import life.qbic.projectmanagement.application.Messages;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ProjectOverview;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService;
import life.qbic.projectmanagement.application.authorization.acl.ProjectAccessService.ProjectCollaborator;
import life.qbic.projectmanagement.application.communication.Content;
import life.qbic.projectmanagement.application.communication.EmailService;
import life.qbic.projectmanagement.application.communication.Recipient;
import life.qbic.projectmanagement.application.communication.Subject;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Component;

/**
 * <b>Sample Registration Notification Service</b>
 * <p>
 * Notifies the collaborators of a project about newly registered samples via email. One email is
 * sent per project collaborator, scheduled asynchronously through JobRunr.
 *
 * @since 1.14.0
 */
@Component
public class SampleRegistrationNotificationService {

  private static final String SUBJECT = "New samples added to project";
  private final EmailService emailService;
  private final ProjectAccessService projectAccessService;
  private final UserInformationService userInformationService;
  private final ProjectInformationService projectInformationService;
  private final AppContextProvider appContextProvider;
  private final JobScheduler jobScheduler;

  public SampleRegistrationNotificationService(EmailService emailService,
      ProjectAccessService projectAccessService, UserInformationService userInformationService,
      ProjectInformationService projectInformationService,
      AppContextProvider appContextProvider, JobScheduler jobScheduler) {
    this.emailService = emailService;
    this.projectAccessService = projectAccessService;
    this.userInformationService = userInformationService;
    this.projectInformationService = projectInformationService;
    this.appContextProvider = appContextProvider;
    this.jobScheduler = jobScheduler;
  }

  /**
   * Sends an email to all collaborators of the project informing them about newly registered
   * samples. The emails are enqueued asynchronously via JobRunr and do not block the registration
   * transaction.
   *
   * @param projectId    the project the samples were registered in
   * @param experimentId the experiment the samples were registered in
   * @param sampleCount  the number of samples that were registered
   * @since 1.14.0
   */
  public void notifyCollaborators(ProjectId projectId, String experimentId,
      int sampleCount) {
    String projectTitle = projectInformationService.findOverview(projectId)
        .map(ProjectOverview::projectTitle)
        .orElse("");
    String sampleUri = appContextProvider.urlToSamplePage(projectId.value(),
        experimentId);
    for (RecipientDTO recipient : collectRecipients(projectId)) {
      jobScheduler.enqueue(() -> notifyRecipient(recipient.emailAddress(), recipient.fullName(),
          projectTitle, sampleCount, sampleUri));
    }
  }

  private List<RecipientDTO> collectRecipients(ProjectId projectId) {
    List<RecipientDTO> recipients = new ArrayList<>();
    var userIds = projectAccessService.listCollaborators(projectId).stream()
        .map(ProjectCollaborator::userId)
        .toList();
    for (String userId : userIds) {
      userInformationService.findById(userId)
          .ifPresent(userInfo -> recipients.add(new RecipientDTO(userInfo)));
    }
    return recipients;
  }

  @Job(name = "Notify users about sample registration in project")
  public void notifyRecipient(String emailAddress, String fullName, String projectTitle,
      int sampleCount, String sampleUri) {
    var message = Messages.samplesAddedToProject(fullName, projectTitle, sampleCount, sampleUri);
    emailService.send(new Subject(SUBJECT),
        new Recipient(emailAddress, fullName), new Content(message));
  }

  private record RecipientDTO(String fullName, String emailAddress) {

    private RecipientDTO(UserInfo userInfo) {
      this(userInfo.fullName(), userInfo.emailAddress());
    }
  }
}