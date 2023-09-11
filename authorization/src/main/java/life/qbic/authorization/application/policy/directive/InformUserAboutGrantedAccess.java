package life.qbic.authorization.application.policy.directive;

import java.util.Objects;
import life.qbic.authentication.domain.user.concept.User;
import life.qbic.authentication.domain.user.concept.UserId;
import life.qbic.authentication.domain.user.repository.UserRepository;
import life.qbic.authorization.application.AppContextProvider;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.domain.concepts.communication.EmailService;
import life.qbic.projectmanagement.domain.project.service.event.ProjectAccessGranted;
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
  private final UserRepository userRepository;

  private final AppContextProvider appContextProvider;

  public InformUserAboutGrantedAccess(EmailService emailService, JobScheduler jobScheduler,
      UserRepository userRepository, AppContextProvider appContextProvider) {
    this.emailService = Objects.requireNonNull(emailService);
    this.jobScheduler = Objects.requireNonNull(jobScheduler);
    this.userRepository = Objects.requireNonNull(userRepository);
    this.appContextProvider = Objects.requireNonNull(appContextProvider);
  }

  private String composeMessage(String projectId, User recipient, String projectTitle) {
    return String.format("""
        Dear %s,
                       
        you have been granted access to project:
                
        '%s'
             
        Please click the link below to access the project after login:
             
        %s
              
        Need help? Contact us for further questions at support@qbic.zendesk.com
              
        Best regards,\
        The QBiC team
        """, recipient.fullName(), projectTitle, appContextProvider.urlToProject(projectId));
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return ProjectAccessGranted.class;
  }

  @Override
  public void handleEvent(ProjectAccessGranted event) {
    jobScheduler.enqueue(() -> notifyUser(event.forUserId(), event.forProjectId(), event.forProjectTitle()));
  }

  @Job(name = "Notify user about granted project access")
  public void notifyUser(String userId, String projectId, String projectTitle)
      throws RuntimeException {
    var recipient = userRepository.findById(UserId.from(userId)).get();
    emailService.send(recipient.emailAddress().get(), recipient.fullName().get(),
        "Project access granted", composeMessage(projectId, recipient, projectTitle));
  }
}
