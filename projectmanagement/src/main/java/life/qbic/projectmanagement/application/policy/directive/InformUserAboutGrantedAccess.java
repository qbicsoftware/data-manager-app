package life.qbic.projectmanagement.application.policy.directive;

import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.domain.concepts.communication.Email;
import life.qbic.domain.concepts.communication.EmailService;
import life.qbic.domain.concepts.communication.Recipient;
import life.qbic.projectmanagement.domain.project.service.event.ProjectAccessGranted;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class InformUserAboutGrantedAccess implements
    DomainEventSubscriber<ProjectAccessGranted> {

  private final EmailService emailService;

  public InformUserAboutGrantedAccess(EmailService emailService) {
    this.emailService = emailService;
  }

  @Override
  public Class<? extends DomainEvent> subscribedToEventType() {
    return ProjectAccessGranted.class;
  }

  @Override
  public void handleEvent(ProjectAccessGranted event) {
    var email = create(event.forProject(), event.forUser());
    emailService.send(email);
  }

  private static Email create(String projectId, String userId) {
   return new Email("You have been granted with access to project " + projectId, "Access granted", "no-reply@qbic.life",
        new Recipient(userId, ""), "text/plain");
  }

}
