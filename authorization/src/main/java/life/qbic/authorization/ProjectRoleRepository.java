package life.qbic.authorization;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
public interface ProjectRoleRepository extends JpaRepository<ProjectRole, Integer> {

  /**
   * Find a project role given a user and a project
   *
   * @param userId    the user for which to search the project role
   * @param projectId the project in which the user has the role
   * @return the role of a user given a project
   */
  Optional<ProjectRole> findByUserIdAndProjectId(String userId, String projectId);
}
