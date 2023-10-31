package life.qbic.projectmanagement.application.policy.directive;

import java.util.Objects;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.identity.api.UserInformationService;
import life.qbic.projectmanagement.application.AppContextProvider;
import life.qbic.projectmanagement.application.Messages;
import life.qbic.projectmanagement.application.communication.Content;
import life.qbic.projectmanagement.application.communication.EmailService;
import life.qbic.projectmanagement.application.communication.Recipient;
import life.qbic.projectmanagement.application.communication.Subject;
import life.qbic.projectmanagement.domain.service.event.ProjectAccessGranted;
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

  private final EmailService emailService;

  private final JobScheduler jobScheduler;
  private final UserInformationService userInformationService;

  private final AppContextProvider appContextProvider;

  public InformUserAboutGrantedAccess(EmailService emailService,
      JobScheduler jobScheduler,
      UserInformationService userInformationService, AppContextProvider appContextProvider) {
    this.emailService = Objects.requireNonNull(emailService);
    this.jobScheduler = Objects.requireNonNull(jobScheduler);
    this.userInformationService = Objects.requireNonNull(userInformationService);
    this.appContextProvider = Objects.requireNonNull(appContextProvider);
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
    emailService.send(new Subject("Project access granted"),
        new Recipient(recipient.emailAddress(), recipient.fullName()), new Content(message));
  }
}
