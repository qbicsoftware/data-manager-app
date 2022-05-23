package life.qbic.apps.datamanager.services;

import java.util.Arrays;
import life.qbic.apps.datamanager.events.EventStore;
import life.qbic.apps.datamanager.notifications.Notification;
import life.qbic.apps.datamanager.notifications.NotificationService;
import life.qbic.domain.events.DomainEventPublisher;
import life.qbic.domain.events.DomainEventSubscriber;
import life.qbic.domain.usermanagement.DomainRegistry;
import life.qbic.domain.usermanagement.registration.UserRegistered;

/**
 * <b>User Registration Service</b>
 * <p>
 * Application service that can be used to register users for the user management domain context.
 *
 * @since 1.0.0
 */
public final class UserRegistrationService {

  private final NotificationService notificationService;

  private final EventStore eventStore;

  public UserRegistrationService(NotificationService notificationService, EventStore eventStore) {
    super();
    this.notificationService = notificationService;
    this.eventStore = eventStore;
  }

  /**
   * Registers a user in the user management domain.
   * <p>
   * Note: the raw password will be overwritten before the method returns. This is only guaranteed
   * though, when the method returns without any exceptions. In the later case, it is the client's
   * responsibility to handle the raw password.
   *
   * @param fullName    the full name of the user
   * @param email       the email address of the user
   * @param rawPassword the raw password provided by the user
   * @since 1.0.0
   */
  public void registerUser(final String fullName, final String email, final char[] rawPassword) {
    var userDomainService = DomainRegistry.instance().userDomainService();
    if (userDomainService.isEmpty()) {
      throw new RuntimeException("User registration failed.");
    }
    // We subscribe to the domain event UserRegistered, in order to forward the event to
    // the EventStore and to the NotificationService for broadcasting
    DomainEventPublisher.instance().subscribe(new DomainEventSubscriber<UserRegistered>() {
      @Override
      public Class<UserRegistered> subscribedToEventType() {
        return UserRegistered.class;
      }

      @Override
      public void handleEvent(UserRegistered event) {
        var notificationId = notificationService.newNotificationId();
        var notification = Notification.create(UserRegistered.class.getSimpleName(),
            event.occurredOn(), notificationId, event);
        eventStore.append(event);
        notificationService.send(notification);
      }
    });
    // Trigger the user creation in the domain service
    userDomainService.get().createUser(fullName, email, rawPassword);
    // Overwrite the password
    Arrays.fill(rawPassword, '-');
  }

}
