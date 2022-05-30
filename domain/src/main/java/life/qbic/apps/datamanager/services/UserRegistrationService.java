package life.qbic.apps.datamanager.services;

import java.util.Arrays;
import java.util.Optional;
import life.qbic.apps.datamanager.events.EventStore;
import life.qbic.apps.datamanager.notifications.Notification;
import life.qbic.apps.datamanager.notifications.NotificationService;
import life.qbic.domain.events.DomainEventPublisher;
import life.qbic.domain.events.DomainEventSubscriber;
import life.qbic.domain.usermanagement.DomainRegistry;
import life.qbic.domain.usermanagement.User;
import life.qbic.domain.usermanagement.UserActivated;
import life.qbic.domain.usermanagement.registration.UserRegistered;
import life.qbic.domain.usermanagement.repository.UserDataStorage;
import life.qbic.domain.usermanagement.repository.UserRepository;

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
    // the EventStore and to the NotificationService for broadcasting
    DomainEventPublisher.instance()
        .subscribe(
            new DomainEventSubscriber<UserRegistered>() {
              @Override
              public Class<UserRegistered> subscribedToEventType() {
                return UserRegistered.class;
              }

              @Override
              public void handleEvent(UserRegistered event) {
                sendNotification(event);
                eventStore.append(event);
              }

              private void sendNotification(UserRegistered event) {
                var notificationId = notificationService.newNotificationId();
                var notification =
                    Notification.create(
                        UserRegistered.class.getSimpleName(),
                        event.occurredOn(),
                        notificationId,
                        event);
                notificationService.send(notification);
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
    DomainEventPublisher.instance().subscribe(new DomainEventSubscriber<UserActivated>() {

      @Override
      public Class<UserActivated> subscribedToEventType() {
        return UserActivated.class;
      }

      @Override
      public void handleEvent(UserActivated event) {
        storeEvent(event);
        sendNotification(event);
        //TODO unsubscribe
      }

      private void sendNotification(UserActivated event) {
        var notificationId = notificationService.newNotificationId();
        var notification =
            Notification.create(
                UserActivated.class.getSimpleName(),
                event.occurredOn(),
                notificationId,
                event);
        notificationService.send(notification);
      }

      private void storeEvent(UserActivated event) {
        eventStore.append(event);
      }
    });
    UserRepository userRepository = UserRepository.getInstance(userDataStorage);
    Optional<User> optionalUser = userRepository.findById(userId);
    optionalUser.ifPresentOrElse(User::confirmEmail, () -> {
      throw new RuntimeException("Unknown user. Could not confirm the email address.");
    });

  }
}
