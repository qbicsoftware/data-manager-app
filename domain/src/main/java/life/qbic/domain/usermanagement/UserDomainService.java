package life.qbic.domain.usermanagement;

import java.util.ArrayList;
import java.util.List;
import life.qbic.domain.events.DomainEventPublisher;
import life.qbic.domain.usermanagement.User.UserException;
import life.qbic.domain.usermanagement.registration.UserRegistered;
import life.qbic.domain.usermanagement.repository.UserRepository;

/**
 * <b>User Domain Service</b>
 *
 * <p>Domain service within the usermanagement context. Takes over the user creation and publishes a
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
   * @param fullName the full name of the user
   * @param email a valid email address
   * @param rawPassword the raw password desired by the user
   * @since 1.0.0
   */
  public void createUser(String fullName, String email, char[] rawPassword) throws UserException {
    // First check, if a user with the provided email already exists
    validateUserCredentials(fullName, email, rawPassword);
    var domainEventPublisher = DomainEventPublisher.instance();
    var user = User.create(fullName, email, rawPassword);
    userRepository.addUser(user);
    var userCreatedEvent = UserRegistered.create(user.getId(), user.getFullName(), user.getEmail());
    domainEventPublisher.publish(userCreatedEvent);
  }

  private boolean isEmailInDatabase(String email) {
    return userRepository.findByEmail(email).isPresent();
  }

  private void validateUserCredentials(String fullName, String email, char[] rawPassword) {
    List<UserException> userExceptions = new ArrayList<>();
    var user = new User();
    if (isEmailInDatabase(email)) {
      userExceptions.add(new UserException("User with email address already exists."));
    }
    try {
      user.setFullName(fullName);
    } catch (UserException e) {
      userExceptions.add(e);
    }
    try {
      user.setEmail(email);
    } catch (UserException e) {
      userExceptions.add(e);
    }
    try {
      user.setPassword(rawPassword);
    } catch (UserException e) {
      userExceptions.add(e);
    }
    throwCorrectUserException(userExceptions);
  }

  private void throwCorrectUserException(List<UserException> userExceptions) {
    if (userExceptions.size() == 1) {
      throw userExceptions.get(0);
    }
    if (userExceptions.size() > 1) {
      UserException groupedUserException = new UserException("Multiple Invalid Credentials");
      userExceptions.forEach(groupedUserException::addSuppressed);
      throw groupedUserException;
    }
  }
}
