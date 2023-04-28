package life.qbic.authentication.application.user.policy;

import life.qbic.authentication.application.communication.UserContactService;
import life.qbic.authentication.domain.user.event.UserRegistered;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.domain.concepts.DomainEventSubscriber;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Component
public class WhenUserRegisteredSendConfirmationEmail implements DomainEventSubscriber<UserRegistered> {

    private final UserContactService userContactService;

    private final JobScheduler jobScheduler;

    public WhenUserRegisteredSendConfirmationEmail(@Autowired UserContactService userContactService, @Autowired JobScheduler jobScheduler) {
        this.userContactService = userContactService;
        DomainEventDispatcher.instance().subscribe(this);
        this.jobScheduler = jobScheduler;
    }

    @Override
    public Class<? extends DomainEvent> subscribedToEventType() {
        return UserRegistered.class;
    }

    @Override
    public void handleEvent(UserRegistered event) {
        this.jobScheduler.enqueue(() -> userContactService.sendEmailConfirmation(event.userId()));
    }
}
