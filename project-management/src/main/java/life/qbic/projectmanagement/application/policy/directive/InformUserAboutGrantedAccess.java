package life.qbic.projectmanagement.application.policy.directive;

import static java.util.Objects.requireNonNull;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.identity.api.UserInformationService;
import life.qbic.projectmanagement.application.AppContextProvider;
import life.qbic.projectmanagement.application.Messages;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.communication.Content;
import life.qbic.projectmanagement.application.communication.EmailService;
import life.qbic.projectmanagement.application.communication.Recipient;
import life.qbic.projectmanagement.application.communication.Subject;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectIntent;
import life.qbic.projectmanagement.domain.model.project.ProjectTitle;
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
  private final ProjectInformationService projectInformationService;

  private final AppContextProvider appContextProvider;

  public InformUserAboutGrantedAccess(EmailService emailService,
      JobScheduler jobScheduler,
      UserInformationService userInformationService,
      ProjectInformationService projectInformationService, AppContextProvider appContextProvider) {
    this.emailService = requireNonNull(emailService);
    this.jobScheduler = requireNonNull(jobScheduler);
    this.userInformationService = requireNonNull(userInformationService);
    this.projectInformationService = requireNonNull(projectInformationService,
        "projectInformationService must not be null");
    this.appContextProvider = requireNonNull(appContextProvider);
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return ProjectAccessGranted.class;
  }

  @Override
  public void handleEvent(ProjectAccessGranted event) {
    String projectTitle = projectInformationService.find(event.forProjectId())
        .map(Project::getProjectIntent)
        .map(ProjectIntent::projectTitle)
        .map(ProjectTitle::title).orElseThrow();
    jobScheduler.enqueue(() -> notifyUser(event.forUserId(), event.forProjectId(), projectTitle));
  }

  @Job(name = "Notify user %0 about granted access to project %1")
  public void notifyUser(String userId, String projectId, String projectTitle)
      throws DirectiveExecutionException {
    var recipient = userInformationService.findById(userId).orElseThrow();
    var projectUrl = appContextProvider.urlToProject(projectId);
    var message = Messages.projectAccessToUser(recipient.fullName(), projectTitle, projectUrl);
    emailService.send(new Subject("Project access granted"),
        new Recipient(recipient.emailAddress(), recipient.fullName()), new Content(message));
  }
}
