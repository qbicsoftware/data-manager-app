package life.qbic.authorization;

import java.util.List;
import java.util.Optional;
import life.qbic.authentication.domain.user.concept.UserId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * <b>Project Role Storage Interface</b>
 * <p>
 * Provides access to persistent project roles.
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

  /**
   * Returns all users associated with the provided ProjectId
   *
   * @param projectId the project to which the users of interested are associated with
   * @return List of {@link UserId} which are associated with the provided project
   */
  List<ProjectRole> findAllByProjectId(String projectId);
}
