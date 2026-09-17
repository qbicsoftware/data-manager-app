package life.qbic.projectmanagement.application.pinned;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.project.ProjectId;

/**
 * A single pinned project: the association between one user and one project they chose to pin for
 * quick access.
 * <p>
 * A pin is an application-layer user preference, <b>not</b> project state. It is intentionally not
 * reachable from the {@code Project} aggregate, so that pinning never modifies a project and never
 * changes {@code Project#lastModified} — the default sort key of the project overview (ADR-0008).
 * <p>
 * {@code projectCodeSnapshot} and {@code projectTitleSnapshot} are captured once, when the pin is
 * created, and are never updated. They exist only so that a pin whose project the user can no longer
 * read can still be recognised and removed by its owner. They must never be used to render a pin for
 * a project the user <i>can</i> read, and must never be exported or indexed.
 *
 * @since 1.19.0
 */
@Entity
@Table(name = "pinned_projects")
public class PinnedProject {

  @EmbeddedId
  private PinnedProjectId id;

  @Column(name = "pinnedAt", nullable = false)
  private Instant pinnedAt;

  @Column(name = "projectCodeSnapshot")
  private String projectCodeSnapshot;

  @Column(name = "projectTitleSnapshot")
  private String projectTitleSnapshot;

  protected PinnedProject() {
    // required by JPA
  }

  private PinnedProject(PinnedProjectId id, Instant pinnedAt, String projectCodeSnapshot,
      String projectTitleSnapshot) {
    this.id = Objects.requireNonNull(id, "id cannot be null");
    this.pinnedAt = Objects.requireNonNull(pinnedAt, "pinnedAt cannot be null");
    this.projectCodeSnapshot = projectCodeSnapshot;
    this.projectTitleSnapshot = projectTitleSnapshot;
  }

  /**
   * Creates a new pin for a user.
   *
   * @param userId              the id of the user that pins the project
   * @param projectId           the pinned project
   * @param pinnedAt            creation timestamp of the pin
   * @param projectCodeSnapshot project code as readable at pin creation time
   * @param projectTitleSnapshot project title as readable at pin creation time
   * @return the new pin, not yet persisted
   */
  public static PinnedProject create(String userId, ProjectId projectId, Instant pinnedAt,
      String projectCodeSnapshot, String projectTitleSnapshot) {
    Objects.requireNonNull(projectId, "projectId cannot be null");
    return new PinnedProject(PinnedProjectId.of(userId, projectId.value()), pinnedAt,
        projectCodeSnapshot, projectTitleSnapshot);
  }

  public String userId() {
    return id.userId();
  }

  public ProjectId projectId() {
    return ProjectId.parse(id.projectId());
  }

  public Instant pinnedAt() {
    return pinnedAt;
  }

  public String projectCodeSnapshot() {
    return projectCodeSnapshot;
  }

  public String projectTitleSnapshot() {
    return projectTitleSnapshot;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    return Objects.equals(id, ((PinnedProject) o).id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
