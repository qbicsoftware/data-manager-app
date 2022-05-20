package life.qbic.apps.datamanager.services;

import java.util.Arrays;
import life.qbic.domain.events.DomainEventPublisher;
import life.qbic.domain.events.DomainEventSubscriber;
import life.qbic.domain.usermanagement.DomainRegistry;
import life.qbic.domain.usermanagement.registration.UserRegistered;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class UserRegistrationService {

  public UserRegistrationService() {
    super();
  }

  public void registerNewUser(final String fullName, final String email, final char[] password) throws ServiceException {
    var userDomainService = DomainRegistry.instance().userDomainService();
    DomainEventPublisher.instance().subscribe(new DomainEventSubscriber<UserRegistered>() {
      @Override
      public Class<UserRegistered> subscribedToEventType() {
        return UserRegistered.class;
      }

      @Override
      public void handleEvent(UserRegistered event) {
        System.out.println("New user registered event: " + event.occurredOn().toString());
        System.out.println(event.userId() + ": " + event.userEmail());
      }
    });

    userDomainService.createUser(fullName, email, password);
    Arrays.fill(password, '-');
  }

}
