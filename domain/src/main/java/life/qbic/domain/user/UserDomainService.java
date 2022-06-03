package life.qbic.domain.user;

import life.qbic.apps.datamanager.ApplicationException;
import life.qbic.domain.events.DomainEventPublisher;
import life.qbic.domain.usermanagement.registration.UserRegistered;
import life.qbic.domain.usermanagement.repository.UserRepository;

/**
 * <b>User Domain Service</b>
 *
 * <p>Domain service within the usermanagement context. Takes over the user creation and publishes
 * a
 * {@link life.qbic.domain.events.DomainEvent} of type {@link UserRegistered} once the user has been
 * successfully registered in the domain.
 *
 * @since 1.0.0
 */
public class UserDomainService {

  private final UserRepository userRepository;

  public UserDomainService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Creates a new user in the user management context.
   *
   * <p>Note: this will create a new domain event of type {@link UserRegistered}
   *
   * @param fullName    the full name of the user
   * @param email       a valid email address
   * @param password the raw password desired by the user
   * @since 1.0.0
   */
  public void createUser(FullName fullName, Email email, EncryptedPassword password) throws UserExistsException {
    // First check, if a user with the provided email already exists
    if (userRepository.findByEmail(email.address()).isPresent()) {
      throw UserExistsException.create();
    }
    var domainEventPublisher = DomainEventPublisher.instance();
    var user = User.of(password, fullName, email);
    userRepository.addUser(user);

    var userCreatedEvent = UserRegistered.create(user.getId(), user.getFullName().name(),
        user.getEmail().address());
    domainEventPublisher.publish(userCreatedEvent);
  }

  public static class UserExistsException extends ApplicationException {

    private static UserExistsException create() {
      return new UserExistsException();
    }

    private UserExistsException() {
      super();
    }
  }
}
