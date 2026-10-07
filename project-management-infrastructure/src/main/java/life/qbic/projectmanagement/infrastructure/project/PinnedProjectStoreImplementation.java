package life.qbic.projectmanagement.infrastructure.project;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.projectmanagement.application.api.PinnedProjectStore;
import life.qbic.projectmanagement.application.pinned.PinnedProject;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed {@link PinnedProjectStore}.
 * <p>
 * Performs no access control of any kind. Callers decide who may pin what; this class only reads and
 * writes the calling user's own rows (ADR-0008).
 *
 * @since 1.19.0
 */
@Component
@Scope("singleton")
public class PinnedProjectStoreImplementation implements PinnedProjectStore {

  private final PinnedProjectRepository pinnedProjectRepository;

  public PinnedProjectStoreImplementation(PinnedProjectRepository pinnedProjectRepository) {
    this.pinnedProjectRepository = Objects.requireNonNull(pinnedProjectRepository,
        "pinnedProjectRepository cannot be null");
  }

  @Override
  public List<PinnedProject> findByUserId(String userId) {
    if (userId == null || userId.isBlank()) {
      return List.of();
    }
    return pinnedProjectRepository.findAllOfUser(userId);
  }

  @Override
  public long countByUserId(String userId) {
    if (userId == null || userId.isBlank()) {
      return 0;
    }
    return pinnedProjectRepository.countAllOfUser(userId);
  }

  @Override
  public Optional<PinnedProject> find(String userId, ProjectId projectId) {
    Objects.requireNonNull(projectId, "projectId cannot be null");
    if (userId == null || userId.isBlank()) {
      return Optional.empty();
    }
    return pinnedProjectRepository.findPin(userId, projectId.value()).stream().findFirst();
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void add(PinnedProject pin) {
    Objects.requireNonNull(pin, "pin cannot be null");
    if (pinnedProjectRepository.isPinned(pin.userId(), pin.projectId().value())) {
      // Reached only when two pin requests for the same project race; the service checks before
      // writing. Overwriting would silently replace the pin time and the captured label.
      throw new ApplicationException(
          "The project is already pinned by this user: " + pin.projectId().value());
    }
    pinnedProjectRepository.saveAndFlush(pin);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public boolean remove(String userId, ProjectId projectId) {
    Objects.requireNonNull(projectId, "projectId cannot be null");
    if (userId == null || userId.isBlank()) {
      return false;
    }
    return pinnedProjectRepository.removePin(userId, projectId.value()) > 0;
  }
}
