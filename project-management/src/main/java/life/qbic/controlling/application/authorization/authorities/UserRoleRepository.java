package life.qbic.controlling.application.authorization.authorities;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <b>Project Role Storage Interface</b>
 * <p>
 * Provides access to persistent system roles.
 */
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

  /**
   * Find a user role in the system context.
   *
   * @param userId the user for which to search the project role
   * @return the role of a user
   */
  List<UserRole> findAllByUserId(String userId);

}
