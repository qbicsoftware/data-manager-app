package life.qbic.projectmanagement.infrastructure.authorization.authority;

import java.util.List;
import life.qbic.projectmanagement.application.authorization.authorities.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <b>Project Role Storage Interface</b>
 * <p>
 * Provides access to persistent system roles.
 */
public interface UserRoleJpa extends JpaRepository<UserRole, Long> {

  /**
   * Find a user role in the system context.
   *
   * @param userId the user for which to search the project role
   * @return the role of a user
   */
  List<UserRole> findAllByUserId(String userId);

}
