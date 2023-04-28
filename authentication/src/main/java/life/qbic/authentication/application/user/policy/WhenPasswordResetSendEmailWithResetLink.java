package life.qbic.authentication.application.user.policy;

import life.qbic.authentication.application.communication.UserContactService;
import life.qbic.authentication.domain.user.event.PasswordReset;
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
public class WhenPasswordResetSendEmailWithResetLink implements DomainEventSubscriber<PasswordReset> {

    private final UserContactService userContactService;

    private final JobScheduler jobScheduler;

    public WhenPasswordResetSendEmailWithResetLink(@Autowired UserContactService userContactService,
                                                   @Autowired JobScheduler jobScheduler) {
        this.userContactService = userContactService;
        this.jobScheduler = jobScheduler;
        DomainEventDispatcher.instance().subscribe(this);
    }

    @Override
    public Class<? extends DomainEvent> subscribedToEventType() {
        return PasswordReset.class;
    }

    @Override
    public void handleEvent(PasswordReset event) {
        jobScheduler.enqueue(() -> userContactService.sendResetLink(event.userEmailAddress(), event.userFullName(), event.userId()));
    }
}
