package life.qbic.identity.api;

import java.util.Optional;

/**
 * <b>User information service</b>
 *
 * <p>This Java interface serves as an access facade, offering clients from other QBiC domains
 * to query user information, without exposing any internal domain model details outside the context
 * boundary of the user domain.</p>
 *
 * @since 1.0.0
 */
public interface UserInformationService {

  /**
   * Looks up a user based on a given email address. Since the email address is always beloning to
   * one user and one user only, the search result is either successful or empty.
   *
   * @param emailAddress the email address of a user to lookup
   * @return user information if a user with the given email address exists, else empty
   * @since 1.0.0
   */
  Optional<UserInfo> findByEmail(String emailAddress);

  /**
   * Looks up a user by their user id. The user id is guaranteed to be unique, so either it is
   * successful or empty.
   *
   * @param userId the user id to look up the corresponding user entry
   * @return user information if a user with the given email exists, else empty
   * @since 1.0.0
   */
  Optional<UserInfo> findById(String userId);

}
