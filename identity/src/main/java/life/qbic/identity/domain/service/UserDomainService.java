package life.qbic.identity.domain.service;

import life.qbic.domain.concepts.DomainEvent;
import life.qbic.domain.concepts.DomainEventDispatcher;
import life.qbic.identity.domain.event.UserRegistered;
import life.qbic.identity.domain.model.EmailAddress;
import life.qbic.identity.domain.model.EncryptedPassword;
import life.qbic.identity.domain.model.FullName;
import life.qbic.identity.domain.model.User;
import life.qbic.identity.domain.repository.UserRepository;

/**
 * <b>User Domain Service</b>
 *
 * <p>Domain service within the usermanagement context. Takes over the user creation and publishes
 * a {@link DomainEvent} of type {@link UserRegistered} once the user has been successfully
 * registered in the domain.
 * <p>Domain service within the usermanagement context. Takes over the user creation and publishes
 * a {@link DomainEvent} of type {@link UserRegistered} once the user has been successfully
 * registered in the domain.
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
   * <p>Note: this will create a new domain event of type {@link UserRegistered}.
   * This method is idempotent, if a user already exists with the given emailAddress, no new user
   * will be created.
   *
   * @param fullName     the full name of the user
   * @param userName
   * @param emailAddress a valid mail address
   * @param password     the password desired by the user
   * @since 1.0.0
   */
  public void createUser(FullName fullName, String userName, EmailAddress emailAddress, EncryptedPassword password) {
    // Ensure idempotent behaviour of the service
    if (userRepository.findByEmail(emailAddress).isPresent()) {
      return;
    }
    var user = User.create(fullName, emailAddress, userName, password);
    userRepository.addUser(user);
    var userCreatedEvent = UserRegistered.create(user.id().get(), user.fullName().get(),
        user.emailAddress().get());
    DomainEventDispatcher.instance().dispatch(userCreatedEvent);
  }

  public void createOidcUser(FullName userFullName, String userName, EmailAddress userEmail,
      String oidcIssuer, String oidcId) {
    // Ensure idempotent behaviour of the service
    if (userRepository.findByOidc(oidcId, oidcIssuer).isPresent()) {
      return;
    }
    var user = User.createOidc(userFullName.get(), userEmail.get(), userName, oidcIssuer, oidcId);
    userRepository.addUser(user);
    var userCreatedEvent = UserRegistered.create(user.id().get(), user.fullName().get(),
        user.emailAddress().get());
    DomainEventDispatcher.instance().dispatch(userCreatedEvent);
  }
}
