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
 * <b>User Registered Policy</b>
 * <p>
 * This policy executes the integration of remote domains the user confirmation, both needs to be
 * done after a user has registered.
 * <p>
 * On a newly registered user tasks to be done are:
 *
 * <ol>
 *   <li>Send out confirmation link to the provided email address</li>
 *   <li>Broadcast registration event outside of the identity domain</li>
 * </ol>
 *
 * @since 1.0.0
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
