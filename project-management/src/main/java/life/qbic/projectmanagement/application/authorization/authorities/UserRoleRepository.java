package life.qbic.projectmanagement.application.authorization.authorities;

import java.util.List;

/**
 * <b>User Role Repository</b>
 * <p>
 * Enables queries of user roles for a given user.
 *
 * @since 1.0.0
 */
public interface UserRoleRepository {

  List<UserRole> findAllByUserId(String userId);

}
