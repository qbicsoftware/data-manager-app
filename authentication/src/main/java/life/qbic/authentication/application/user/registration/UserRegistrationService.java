package life.qbic.authentication.application.user.registration;

import java.io.Serial;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationResponse;
import life.qbic.authentication.application.ServiceException;
import life.qbic.authentication.application.notification.Notification;
import life.qbic.authentication.application.notification.NotificationService;

import life.qbic.authentication.domain.registry.DomainRegistry;
import life.qbic.authentication.domain.user.concept.EmailAddress;
import life.qbic.authentication.domain.user.concept.EmailAddress.EmailValidationException;
import life.qbic.authentication.domain.user.concept.EncryptedPassword;
import life.qbic.authentication.domain.user.concept.EncryptedPassword.PasswordValidationException;
import life.qbic.authentication.domain.user.concept.FullName;
import life.qbic.authentication.domain.user.concept.FullName.FullNameValidationException;
import life.qbic.authentication.domain.user.concept.User;
import life.qbic.authentication.domain.user.concept.UserId;
import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.domain.concepts.DomainEventPublisher;
import life.qbic.domain.concepts.DomainEventSubscriber;
import life.qbic.domain.concepts.event.EventStore;
import life.qbic.authentication.domain.user.event.PasswordReset;
import life.qbic.authentication.domain.user.event.UserActivated;
import life.qbic.authentication.domain.user.event.UserEmailConfirmed;
import life.qbic.authentication.domain.user.event.UserRegistered;
import life.qbic.authentication.domain.user.repository.UserNotFoundException;
import life.qbic.authentication.domain.user.repository.UserRepository;
import org.jobrunr.scheduling.JobScheduler;

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

  private final JobScheduler jobScheduler;

  public UserRegistrationService(NotificationService notificationService,
      UserRepository userRepository, EventStore eventStore, JobScheduler jobScheduler) {
    super();
    this.notificationService = notificationService;
    this.userRepository = userRepository;
    this.eventStore = eventStore;
    this.jobScheduler = jobScheduler;
  }

  /**
   * Registers a user in the user management domain.
   *
   * <p>Note: the raw password will be overwritten before the method returns. This is only
   * guaranteed though, when the method returns without any exceptions. In the later case, it is the
   * client's responsibility to handle the raw password.
   *
   * @param fullName    the full name of the user
   * @param email       the mail address of the user
   * @param rawPassword the raw password provided by the user
   * @return a registration response with information about if the registration was successful or
   * not.
   * @since 1.0.0
   */
  public ApplicationResponse registerUser(final String fullName, final String email,
      final char[] rawPassword) {

    var registrationResponse = validateInput(fullName, email, rawPassword);
    if (registrationResponse.hasFailures()) {
      return registrationResponse;
    }

    var userDomainService = DomainRegistry.instance().userDomainService();
    if (userDomainService.isEmpty()) {
      throw new RuntimeException("User registration failed.");
    }

    DomainEventDispatcher dispatcher = DomainEventDispatcher.instance();

    dispatcher.subscribe(new DomainEventSubscriber<UserRegistered>() {
      @Override
      public Class<? extends DomainEvent> subscribedToEventType() {
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
        jobScheduler.<NotificationService>enqueue(myservice -> myservice.send(notification));
      }
    });

    var userEmail = EmailAddress.from(email);
    var userFullName = FullName.from(fullName);
    var userPassword = EncryptedPassword.from(rawPassword);

    if (userRepository.findByEmail(userEmail).isPresent()) {
      return ApplicationResponse.failureResponse(new UserExistsException());
    }

    // Trigger the user creation in the domain service
    userDomainService.get().createUser(userFullName, userEmail, userPassword);

    // Overwrite the password
    Arrays.fill(rawPassword, '-');

    return ApplicationResponse.successResponse();
  }

  private ApplicationResponse validateInput(String fullName, String email, char[] rawPassword) {
    List<RuntimeException> failures = new ArrayList<>();

    try {
      EmailAddress.from(email);
    } catch (EmailValidationException e) {
      failures.add(e);
    }
    try {
      FullName.from(fullName);
    } catch (FullNameValidationException e) {
      failures.add(e);
    }
    try {
      EncryptedPassword.from(rawPassword);
    } catch (PasswordValidationException e) {
      failures.add(e);
    }

    if (failures.isEmpty()) {
      return ApplicationResponse.successResponse();
    }

    return ApplicationResponse.failureResponse(failures.toArray(RuntimeException[]::new));
  }

  /**
   * Requests a password reset for a user.
   *
   * @param userEmailAddress the user's email address for whom the password reset shall be issued
   * @return application response with success or failure information
   * @since 1.0.0
   */
  public ApplicationResponse requestPasswordReset(String userEmailAddress) {
    EmailAddress emailAddress;
    try {
      emailAddress = EmailAddress.from(userEmailAddress);
    } catch (EmailValidationException e) {
      return ApplicationResponse.failureResponse(e);
    }
    // fetch user
    var optionalUser = userRepository.findByEmail(emailAddress);
    if (optionalUser.isEmpty()) {
      return ApplicationResponse.failureResponse(new UserNotFoundException("User not found"));
    }

    // get user
    var user = optionalUser.get();

    // We only allow password reset for users with confirmed email address
    if (!user.isActive()) {
      return ApplicationResponse.failureResponse(new UserNotActivatedException("User not active"));
    }

    DomainEventPublisher domainEventPublisher = DomainEventPublisher.instance();
    while (!domainEventPublisher.clear()) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    domainEventPublisher.subscribe(new DomainEventSubscriber<PasswordReset>() {
      @Override
      public Class<? extends DomainEvent> subscribedToEventType() {
        return PasswordReset.class;
      }

      @Override
      public void handleEvent(PasswordReset event) {
        eventStore.append(event);
        sendNotification(event, notificationService);
      }
    });

    user.resetPassword();
    return ApplicationResponse.successResponse();
  }

  private static void sendNotification(DomainEvent event, NotificationService notificationService) {
    var notificationId = notificationService.newNotificationId();
    var notification =
        Notification.create(
            event.getClass().getSimpleName(),
            event.occurredOn(),
            notificationId,
            event);
    notificationService.send(notification);
  }

  /**
   * Sets a new password for a given user.
   * <p>
   * Success or failures of the request need to be evaluated by the client via the
   * {@link ApplicationResponse}.
   *
   * @param userId         the user's id for whom the new password shall be set
   * @param newRawPassword the user's request new password
   * @return an application response. In the case of a password validation failure, the
   * {@link ApplicationResponse#failures()} will contain an exception with type
   * {@link PasswordValidationException}.
   * @since 1.0.0
   */
  public ApplicationResponse newUserPassword(String userId, char[] newRawPassword) {
    UserId id = UserId.from(userId);
    EncryptedPassword encryptedPassword;
    try {
      encryptedPassword = EncryptedPassword.from(newRawPassword);
    } catch (PasswordValidationException e) {
      return ApplicationResponse.failureResponse(e);
    }

    var optionalUser = userRepository.findById(id);

    if (optionalUser.isEmpty()) {
      return ApplicationResponse.failureResponse(new ServiceException("Unknown user id"));
    }

    optionalUser.ifPresent(user -> {
      user.setNewPassword(encryptedPassword);
      userRepository.updateUser(user);
    });

    return ApplicationResponse.successResponse();
  }

  public static class UserExistsException extends ApplicationException {

    @Serial
    private static final long serialVersionUID = 3147229431249844901L;

    public UserExistsException() {
      super();
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
    DomainEventPublisher domainEventPublisher = DomainEventPublisher.instance();
    while (!domainEventPublisher.clear()) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    domainEventPublisher.subscribe(new DomainEventSubscriber<UserActivated>() {
      @Override
      public Class<UserActivated> subscribedToEventType() {
        return UserActivated.class;
      }

      @Override
      public void handleEvent(UserActivated event) {
        eventStore.append(event);
        sendNotification(event, notificationService);
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

    Optional<User> optionalUser = userRepository.findById(UserId.from(userId));
    optionalUser.ifPresentOrElse(user -> {
      user.confirmEmail();
      userRepository.updateUser(user);
    }, () -> {
      throw new UserNotFoundException("Unknown user. Could not confirm the email address.");
    });
  }

  /**
   * <p>
   * An exception to be thrown if a user is not activated. This implies that the user cannot log in
   * to the application
   * </p>
   */
  public class UserNotActivatedException extends ApplicationException {

    @Serial
    private static final long serialVersionUID = -4253849498611530692L;

    UserNotActivatedException(String message) {
      super(message);
    }
  }
}
