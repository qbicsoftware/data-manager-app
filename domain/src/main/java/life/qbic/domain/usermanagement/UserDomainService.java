package life.qbic.domain.usermanagement;

import life.qbic.domain.events.DomainEventPublisher;
import life.qbic.domain.usermanagement.User.UserException;
import life.qbic.domain.usermanagement.registration.UserRegistered;
import life.qbic.domain.usermanagement.repository.UserRepository;

/**
 * <b>User Domain Service</b>
 * <p>
 * Domain service within the usermanagement context. Takes over the user creation and publishes a
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
     * <p>
     * Note: this will create a new domain event of type {@link UserRegistered}
     *
     * @param fullName    the full name of the user
     * @param email       a valid email address
     * @param rawPassword the raw password desired by the user
     * @since 1.0.0
     */
    public void createUser(String fullName, String email, char[] rawPassword)
            throws UserException {
        // First check, if a user with the provided email already exists
        if (userRepository.findByEmail(email).isPresent()) {
            throw new UserException("User with email address already exists.");
        }
        var domainEventPublisher = DomainEventPublisher.instance();
        var user = User.create(fullName, email);
        user.setPassword(rawPassword);
        userRepository.addUser(user);

        var userCreatedEvent = UserRegistered.create(user.getId(), user.getFullName(),
                user.getEmail());
        domainEventPublisher.publish(userCreatedEvent);
    }

}
