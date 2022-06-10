package life.qbic.apps.datamanager.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import life.qbic.apps.datamanager.ApplicationException;
import life.qbic.apps.datamanager.events.EventStore;
import life.qbic.apps.datamanager.notifications.Notification;
import life.qbic.apps.datamanager.notifications.NotificationService;
import life.qbic.domain.events.DomainEventPublisher;
import life.qbic.domain.events.DomainEventSubscriber;
import life.qbic.domain.user.EmailAddress;
import life.qbic.domain.user.EmailAddress.EmailValidationException;
import life.qbic.domain.user.EncryptedPassword;
import life.qbic.domain.user.EncryptedPassword.PasswordValidationException;
import life.qbic.domain.user.FullName;
import life.qbic.domain.user.FullName.InvalidFullNameException;
import life.qbic.domain.usermanagement.DomainRegistry;
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

    var userEmail = EmailAddress.from(email);
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
      EmailAddress.from(email);
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

    return RegistrationResponse.failureResponse(failures.toArray(RuntimeException[]::new));
  }

  public static class RegistrationResponse {

    private enum Type {SUCCESSFUL, FAILED}
    private Type type;

    private List<RuntimeException> exceptions;

    public static RegistrationResponse successResponse() {
      var successResponse = new RegistrationResponse();
      successResponse.setType(Type.SUCCESSFUL);
      return successResponse;
    }

    public static RegistrationResponse failureResponse(RuntimeException... exceptions) {
      if (exceptions == null) {
        throw new IllegalArgumentException("Null references are not allowed.");
      }
      var failureResponse = new RegistrationResponse();
      failureResponse.setType(Type.FAILED);
      failureResponse.setExceptions(exceptions);
      return failureResponse;
    }

    private RegistrationResponse() {
      super();
    }

    private void setType(Type type) {
      this.type = type;
    }

    public Type getType() {
      return type;
    }

    private void setException(RuntimeException e) {
      this.exceptions = Collections.singletonList(e);
    }

    private void setExceptions(RuntimeException... exceptions) {
      this.exceptions = Arrays.stream(exceptions).toList();
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
