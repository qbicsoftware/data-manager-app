package life.qbic.projectmanagement.application.api;

import java.util.List;
import java.util.Optional;
import life.qbic.projectmanagement.application.pinned.PinnedProject;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * Persistence port for the user-owned pinned-project associations.
 * <p>
 * Deliberately keyed by user id and project id only. In particular {@link #remove(String,
 * ProjectId)} performs <b>no project access check</b>: a user must always be able to free one of
 * their own places, even when the pinned project became unreadable (ADR-0008). Access control is
 * enforced by {@link PinnedProjectService}, not by this port.
 *
 * @since 1.12.0
 */
public interface PinnedProjectStore {

  /**
   * Returns all pins of one user, newest pin first.
   *
   * @param userId the owner of the pins
   * @return the user's pins in pinning order, empty if the user pinned nothing
   */
  List<PinnedProject> findByUserId(String userId);

  /**
   * @param userId the owner of the pins
   * @return the number of pins held by the user, including pins of unreadable projects
   */
  long countByUserId(String userId);

  /**
   * @param userId    the owner of the pins
   * @param projectId the project to look for
   * @return the pin, if the user pinned that project
   */
  Optional<PinnedProject> find(String userId, ProjectId projectId);

  /**
   * Stores a new pin. Implementations must reject a duplicate (userId, projectId) pair rather than
   * overwrite it.
   *
   * @param pin the pin to persist
   */
  void add(PinnedProject pin);

  /**
   * Removes a pin without requiring any permission on the referenced project.
   *
   * @param userId    the owner of the pin
   * @param projectId the project to unpin
   * @return {@code true} if a pin was removed, {@code false} if the user had no such pin
   */
  boolean remove(String userId, ProjectId projectId);
}
