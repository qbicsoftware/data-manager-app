package life.qbic.domain.usermanagement;

import life.qbic.domain.usermanagement.User.UserException;
import life.qbic.domain.usermanagement.repository.UserRepository;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class UserDomainService {

  private final UserRepository userRepository;

  public UserDomainService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Creates a new user in the user management context.
   *
   * @param fullName    the full name of the user
   * @param email       a valid email address
   * @param rawPassword the raw password desired by the user
   * @since 1.0.0
   */
  public void createNewUser(String fullName, String email, char[] rawPassword)
      throws UserException {
    var user = User.create(fullName, email);
    user.setPassword(rawPassword);
    userRepository.addUser(user);
  }

}
