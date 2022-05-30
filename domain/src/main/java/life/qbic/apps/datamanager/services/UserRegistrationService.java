package life.qbic.apps.datamanager.services;

import java.util.Arrays;
import java.util.Optional;
import life.qbic.apps.datamanager.events.EventStore;
import life.qbic.apps.datamanager.notifications.Notification;
import life.qbic.apps.datamanager.notifications.NotificationService;
import life.qbic.domain.events.DomainEvent;
import life.qbic.domain.events.DomainEventPublisher;
import life.qbic.domain.events.DomainEventSubscriber;
import life.qbic.domain.usermanagement.DomainRegistry;
import life.qbic.domain.usermanagement.User;
import life.qbic.domain.usermanagement.UserActivated;
import life.qbic.domain.usermanagement.UserEmailConfirmed;
import life.qbic.domain.usermanagement.registration.UserRegistered;
import life.qbic.domain.usermanagement.repository.UserDataStorage;

/**
 * <b>User Registration Service</b>
 *
 * <p>Application service that can be used to register users for the user management domain
 * context.
 *
 * @since 1.0.0
 */
public final class UserRegistrationService {

  private final NotificationService notificationService;

  private final UserDataStorage userDataStorage;

  private final EventStore eventStore;

  public UserRegistrationService(NotificationService notificationService,
      UserDataStorage userDataStorage, EventStore eventStore) {
    super();
    this.notificationService = notificationService;
    this.userDataStorage = userDataStorage;
    this.eventStore = eventStore;
  }

  /**
   * Registers a user in the user management domain.
   *
   * <p>Note: the raw password will be overwritten before the method returns. This is only
   * guaranteed though, when the method returns without any exceptions. In the later case, it is the
   * client's responsibility to handle the raw password.
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
    // the EventStore
    DomainEventPublisher.instance()
        .subscribe(new StoreEventSubscriber<UserRegistered>() {
          @Override
          public Class<UserRegistered> subscribedToEventType() {
            return UserRegistered.class;
          }
        });
    // We subscribe to the domain event UserRegistered, in order to inform the NotificationService for broadcasting
    DomainEventPublisher.instance().subscribe(new NotifyAboutEventSubscriber<UserRegistered>() {
      @Override
      public Class<UserRegistered> subscribedToEventType() {
        return UserRegistered.class;
      }
    });
    // Trigger the user creation in the domain service
    userDomainService.get().createUser(fullName, email, rawPassword);
    // Overwrite the password
    Arrays.fill(rawPassword, '-');
  }

  /**
   * Activates a user with the userId provided. If no user is matched then this method does
   * nothing.
   *
   * @param userId the id of the user to be activated
   * @since 1.0.0
   */
  public void confirmUserEmail(String userId) {
    DomainEventPublisher.instance().subscribe(new NotifyAboutEventSubscriber<UserActivated>() {
      @Override
      public Class<UserActivated> subscribedToEventType() {
        return UserActivated.class;
      }
    });
    DomainEventPublisher.instance().subscribe(new StoreEventSubscriber<UserActivated>() {
      @Override
      public Class<UserActivated> subscribedToEventType() {
        return UserActivated.class;
      }
    });
    DomainEventPublisher.instance().subscribe(new NotifyAboutEventSubscriber<UserEmailConfirmed>() {
      @Override
      public Class<UserEmailConfirmed> subscribedToEventType() {
        return UserEmailConfirmed.class;
      }
    });
    DomainEventPublisher.instance().subscribe(new StoreEventSubscriber<UserEmailConfirmed>() {
      @Override
      public Class<UserEmailConfirmed> subscribedToEventType() {
        return UserEmailConfirmed.class;
      }
    });
    Optional<User> optionalUser = userDataStorage.findUserById(userId);
    optionalUser.ifPresentOrElse(user -> {
      user.confirmEmail();
      userDataStorage.save(user);
    }, () -> {
      throw new RuntimeException("Unknown user. Could not confirm the email address.");
    });
  }

  /**
   * A subscriber storing the event to the event store
   *
   * @param <T> the event type
   */
  private abstract class StoreEventSubscriber<T extends DomainEvent> implements
      DomainEventSubscriber<T> {

    @Override
    public void handleEvent(T event) {
      eventStore.append(event);
    }
  }

  /**
   * A subscriber sending a notification about the event
   *
   * @param <T> the event type
   */
  private abstract class NotifyAboutEventSubscriber<T extends DomainEvent> implements
      DomainEventSubscriber<T> {

    @Override
    public void handleEvent(T event) {
      var notificationId = notificationService.newNotificationId();
      var notification =
          Notification.create(
              subscribedToEventType().getSimpleName(),
              event.occurredOn(),
              notificationId,
              event);
      notificationService.send(notification);
    }
  }
}
