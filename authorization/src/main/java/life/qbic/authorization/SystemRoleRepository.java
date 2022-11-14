package life.qbic.authorization;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
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
