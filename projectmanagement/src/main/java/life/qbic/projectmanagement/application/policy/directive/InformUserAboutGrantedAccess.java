package life.qbic.projectmanagement.application.policy.directive;

import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.domain.concepts.communication.CommunicationService;
import life.qbic.domain.concepts.communication.Content;
import life.qbic.domain.concepts.communication.Recipient;
import life.qbic.domain.concepts.communication.Subject;
import life.qbic.projectmanagement.application.AppContextProvider;
import life.qbic.projectmanagement.application.Messages;
import life.qbic.projectmanagement.domain.project.service.event.ProjectAccessGranted;
import life.qbic.user.api.UserInfo;
import life.qbic.user.api.UserInformationService;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Component;

/**
 * <b>Directive: Inform user about granted access</b>
 * <p>
 * Notifies the user via email about the recently granted project access.
 *
 * @since 1.0.0
 */
@Component
public class InformUserAboutGrantedAccess implements DomainEventSubscriber<ProjectAccessGranted> {

  private final CommunicationService communicationService;

  private final JobScheduler jobScheduler;
  private final UserInformationService userInformationService;

  private final AppContextProvider appContextProvider;

  public InformUserAboutGrantedAccess(CommunicationService communicationService,
      JobScheduler jobScheduler,
      UserInformationService userInformationService, AppContextProvider appContextProvider) {
    this.communicationService = Objects.requireNonNull(communicationService);
    this.jobScheduler = Objects.requireNonNull(jobScheduler);
    this.userInformationService = Objects.requireNonNull(userInformationService);
    this.appContextProvider = Objects.requireNonNull(appContextProvider);
  }

  private String composeMessage(String projectId, UserInfo recipient, String projectTitle) {
    return Messages.projectAccessToUser(recipient.fullName(), projectTitle,
        appContextProvider.urlToProject(projectId));
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return ProjectAccessGranted.class;
  }

  @Override
  public void handleEvent(ProjectAccessGranted event) {
    jobScheduler.enqueue(
        () -> notifyUser(event.forUserId(), event.forProjectId(), event.forProjectTitle()));
  }

  @Job(name = "Notify user about granted project access")
  public void notifyUser(String userId, String projectId, String projectTitle)
      throws RuntimeException {
    var recipient = userInformationService.findById(userId).get();
    var projectUrl = appContextProvider.urlToProject(projectId);
    var message = Messages.projectAccessToUser(recipient.fullName(), projectTitle, projectUrl);
    communicationService.send(new Subject("Project access granted"),
        new Recipient(recipient.emailAddress(), recipient.fullName()), new Content(message));
  }
}
