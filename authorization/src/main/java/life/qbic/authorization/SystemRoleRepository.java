package life.qbic.authorization;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * <b>Project Role Storage Interface</b>
 * <p>
 * Provides access to persistent system roles.
 */
public interface SystemRoleRepository extends JpaRepository<SystemRole, Integer> {

  /**
   * Find a user role in the system context.
   *
   * @param userId the user for which to search the project role
   * @return the role of a user
   */
  List<SystemRole> findAllByUserId(String userId);

}
