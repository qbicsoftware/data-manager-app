package life.qbic.authentication.application.user.policy;

import life.qbic.authentication.application.communication.UserContactService;
import life.qbic.authentication.domain.user.event.PasswordReset;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.domain.concepts.DomainEventSubscriber;
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

    public WhenPasswordResetSendEmailWithResetLink(@Autowired UserContactService userContactService) {
        this.userContactService = userContactService;
        DomainEventDispatcher.instance().subscribe(this);
    }

    @Override
    public Class<? extends DomainEvent> subscribedToEventType() {
        return PasswordReset.class;
    }

    @Override
    public void handleEvent(PasswordReset event) {
        userContactService.sendResetLink(event.userEmailAddress(), event.userFullName(), event.userId());
    }
}
