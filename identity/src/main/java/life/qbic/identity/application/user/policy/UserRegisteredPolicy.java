package life.qbic.identity.application.user.policy;

import life.qbic.broadcasting.MessageBusSubmission;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.identity.application.communication.EmailService;
import life.qbic.identity.application.communication.broadcasting.EventHub;
import life.qbic.identity.application.user.policy.directive.WhenUserRegisteredSendConfirmationEmail;
import life.qbic.identity.application.user.policy.directive.WhenUserRegisteredSubmitIntegrationEvent;
import life.qbic.identity.domain.repository.UserRepository;
import org.jobrunr.scheduling.JobScheduler;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class UserRegisteredPolicy {

  public UserRegisteredPolicy(EmailService emailService, JobScheduler jobScheduler,
      UserRepository userRepository, EmailConfirmationLinkSupplier emailConfirmationLinkSupplier,
      EventHub eventHub, MessageBusSubmission messageBusSubmission) {
    var confirmationEmail = new WhenUserRegisteredSendConfirmationEmail(emailService, jobScheduler,
        userRepository, emailConfirmationLinkSupplier);
    var submitIntegrationEvent = new WhenUserRegisteredSubmitIntegrationEvent(messageBusSubmission,
        eventHub, jobScheduler);

    DomainEventDispatcher.instance().subscribe(confirmationEmail);
    DomainEventDispatcher.instance().subscribe(submitIntegrationEvent);
  }
}
