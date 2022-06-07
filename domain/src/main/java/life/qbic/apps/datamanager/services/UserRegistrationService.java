package life.qbic.apps.datamanager.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import life.qbic.apps.datamanager.ApplicationException;
import life.qbic.apps.datamanager.events.EventStore;
import life.qbic.apps.datamanager.notifications.Notification;
import life.qbic.apps.datamanager.notifications.NotificationService;
import life.qbic.domain.events.DomainEventPublisher;
import life.qbic.domain.events.DomainEventSubscriber;
import life.qbic.domain.user.Email;
import life.qbic.domain.user.Email.EmailValidationException;
import life.qbic.domain.user.EncryptedPassword;
import life.qbic.domain.user.EncryptedPassword.PasswordValidationException;
import life.qbic.domain.user.FullName;
import life.qbic.domain.user.FullName.InvalidFullNameException;
import life.qbic.domain.user.User;
import life.qbic.domain.usermanagement.DomainRegistry;
import life.qbic.domain.usermanagement.UserActivated;
import life.qbic.domain.usermanagement.UserEmailConfirmed;
import life.qbic.domain.usermanagement.registration.UserNotFoundException;
import life.qbic.domain.usermanagement.registration.UserRegistered;
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

  private final UserRepository userRepository;

  private final EventStore eventStore;

  public UserRegistrationService(NotificationService notificationService,
      UserRepository userRepository, EventStore eventStore) {
    super();
    this.notificationService = notificationService;
    this.userRepository = userRepository;
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
   * @return a registration response with information about if the registration was successful or
   * not.
   * @since 1.0.0
   */
  public RegistrationResponse registerUser(final String fullName, final String email,
      final char[] rawPassword) {

    var registrationResponse = validateInput(fullName, email, rawPassword);
    if (registrationResponse.hasFailures()) {
      return registrationResponse;
    }

    var userDomainService = DomainRegistry.instance().userDomainService();
    if (userDomainService.isEmpty()) {
      throw new RuntimeException("User registration failed.");
    }
    DomainEventPublisher.instance().subscribe(new DomainEventSubscriber<UserRegistered>() {
      @Override
      public Class<UserRegistered> subscribedToEventType() {
        return UserRegistered.class;
      }

      @Override
      public void handleEvent(UserRegistered event) {
        eventStore.append(event);
        sendNotification(event);
      }

      private void sendNotification(UserRegistered event) {
        var notificationId = notificationService.newNotificationId();
        var notification =
            Notification.create(
                event.getClass().getSimpleName(),
                event.occurredOn(),
                notificationId,
                event);
        notificationService.send(notification);
      }
    });

    var userEmail = Email.from(email);
    var userFullName = FullName.from(fullName);
    var userPassword = EncryptedPassword.from(rawPassword);

    if (userRepository.findByEmail(userEmail).isPresent()) {
      return RegistrationResponse.failureResponse(new UserExistsException());
    }

    // Trigger the user creation in the domain service
    userDomainService.get().createUser(userFullName, userEmail, userPassword);

    // Overwrite the password
    Arrays.fill(rawPassword, '-');
    return RegistrationResponse.successResponse();
  }

  private RegistrationResponse validateInput(String fullName, String email, char[] rawPassword) {
    List<RuntimeException> failures = new ArrayList<>();

    try {
      Email.from(email);
    } catch (EmailValidationException e) {
      failures.add(e);
    }
    try {
      FullName.from(fullName);
    } catch (InvalidFullNameException e) {
      failures.add(e);
    }
    try {
      EncryptedPassword.from(rawPassword);
    } catch (PasswordValidationException e) {
      failures.add(e);
    }

    if (failures.isEmpty()) {
      return RegistrationResponse.successResponse();
    }

    if (failures.size() > 1) {
      return RegistrationResponse.failureResponse(failures.get(0),
          failures.subList(1, failures.size())
              .toArray(failures.toArray(new RuntimeException[failures.size() - 1])));
    } else {
      return RegistrationResponse.failureResponse(failures.get(0));
    }
  }

  /**
   * Activates a user with the userId provided. If no user is matched then this method does
   * nothing.
   *
   * @param userId the id of the user to be activated
   * @throws UserNotFoundException when no user with the provided user id can be found.
   * @since 1.0.0
   */
  public void confirmUserEmail(String userId) throws UserNotFoundException {
    DomainEventPublisher.instance().subscribe(new DomainEventSubscriber<UserActivated>() {
      @Override
      public Class<UserActivated> subscribedToEventType() {
        return UserActivated.class;
      }

      @Override
      public void handleEvent(UserActivated event) {
        eventStore.append(event);
        sendNotification(event);
      }

      private void sendNotification(UserActivated event) {
        var notificationId = notificationService.newNotificationId();
        var notification =
            Notification.create(
                event.getClass().getSimpleName(),
                event.occurredOn(),
                notificationId,
                event);
        notificationService.send(notification);
      }
    });
    DomainEventPublisher.instance().subscribe(new DomainEventSubscriber<UserEmailConfirmed>() {
      @Override
      public Class<UserEmailConfirmed> subscribedToEventType() {
        return UserEmailConfirmed.class;
      }

      @Override
      public void handleEvent(UserEmailConfirmed event) {
        eventStore.append(event);
        sendNotification(event);
      }

      private void sendNotification(UserEmailConfirmed event) {
        var notificationId = notificationService.newNotificationId();
        var notification =
            Notification.create(
                event.getClass().getSimpleName(),
                event.occurredOn(),
                notificationId,
                event);
        notificationService.send(notification);
      }
    });
    Optional<User> userToActivate = userRepository.findById(userId);
    userToActivate.ifPresentOrElse(user -> {
      user.confirmEmail();
      userRepository.updateUser(user);
    }, () -> {
      throw new UserNotFoundException("Unknown user. Could not confirm the email address.");
    });
  }

  public static class RegistrationResponse {

    private enum Type {SUCCESSFUL, FAILED}

    public Type type;

    private List<RuntimeException> exceptions;

    public static RegistrationResponse successResponse() {
      var successResponse = new RegistrationResponse();
      successResponse.setType(Type.SUCCESSFUL);
      return successResponse;
    }

    public static RegistrationResponse failureResponse(RuntimeException e1, RuntimeException... exceptions) {
      if (e1 == null || exceptions == null) {
        throw new IllegalArgumentException("Null references are not allowed.");
      }
      var failureResponse = new RegistrationResponse();
      failureResponse.setType(Type.FAILED);
      failureResponse.setExceptions(e1, exceptions);
      return failureResponse;
    }

    private RegistrationResponse() {
      super();
    }

    private void setType(Type type) {
      this.type = type;
    }

    private void setExceptions(RuntimeException e1, RuntimeException... exceptions) {
      RuntimeException[] allExceptions;
      allExceptions =
          exceptions.length > 0 ? new RuntimeException[exceptions.length + 1] : new RuntimeException[1];
      allExceptions[0] = e1;
      for (int i = 0; i < exceptions.length; i++) {
        allExceptions[i + 1] = exceptions[i];
      }
      this.exceptions = Arrays.stream(allExceptions).toList();
    }

    public boolean hasFailures() {
      return type == Type.FAILED;
    }

    public List<RuntimeException> failures() {
      return exceptions;
    }
  }

  public static class UserExistsException extends ApplicationException {

    public UserExistsException() {
      super();
    }
  }
}
