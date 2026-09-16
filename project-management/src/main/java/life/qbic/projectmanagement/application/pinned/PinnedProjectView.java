package life.qbic.projectmanagement.application.pinned;

import java.time.Instant;
import life.qbic.projectmanagement.application.ProjectOverview;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import java.util.Objects;

/**
 * Render-ready representation of one pinned project for the current user.
 * <p>
 * A pin is either {@link AccessState#ACCESSIBLE} — in which case it carries the live project
 * information from the access-restricted overview — or {@link AccessState#REVOKED}, in which case it
 * carries only the label that was captured when the pin was created and nothing else. The overview of
 * a revoked pin is never populated, so the UI cannot accidentally render project data the user is not
 * entitled to see (USER-R-04).
 *
 * @param projectId the pinned project
 * @param pinnedAt  when the user pinned it; pins are ordered newest first
 * @param accessState whether the requesting user may still read the project
 * @param projectCode display code; live for accessible pins, snapshot for revoked ones
 * @param projectTitle display title; live for accessible pins, snapshot for revoked ones
 * @param overview the live overview for accessible pins, {@code null} for revoked ones
 * @since 1.12.0
 */
public record PinnedProjectView(ProjectId projectId,
                                Instant pinnedAt,
                                AccessState accessState,
                                String projectCode,
                                String projectTitle,
                                ProjectOverview overview) {

  /**
   * Whether the requesting user can still read the pinned project.
   */
  public enum AccessState {
    /** The project is readable; live overview data is present. */
    ACCESSIBLE,
    /** Read access is gone; only the pin-time label is present. */
    REVOKED
  }

  public PinnedProjectView {
    Objects.requireNonNull(projectId, "projectId cannot be null");
    Objects.requireNonNull(pinnedAt, "pinnedAt cannot be null");
    Objects.requireNonNull(accessState, "accessState cannot be null");
    Objects.requireNonNull(projectCode, "projectCode cannot be null");
    Objects.requireNonNull(projectTitle, "projectTitle cannot be null");
    if (accessState == AccessState.ACCESSIBLE && overview == null) {
      throw new IllegalArgumentException("accessible pinned projects require a live overview");
    }
  }

  /**
   * Builds the view of a pin whose project the user can still read; the live overview supplies the
   * displayed label, so the pin-time snapshot stays unused.
   */
  public static PinnedProjectView liveOf(PinnedProject pin, ProjectOverview overview) {
    return new PinnedProjectView(pin.projectId(), pin.pinnedAt(), AccessState.ACCESSIBLE,
        overview.projectCode(), overview.projectTitle(), overview);
  }

  /**
   * Builds the view of a pin whose project the user can no longer read. Only the label captured when
   * the pin was created is carried; nothing else about the project is available to the renderer.
   */
  public static PinnedProjectView placeholderOf(PinnedProject pin) {
    return new PinnedProjectView(pin.projectId(), pin.pinnedAt(), AccessState.REVOKED,
        pin.projectCodeSnapshot() == null ? "" : pin.projectCodeSnapshot(),
        pin.projectTitleSnapshot() == null ? "" : pin.projectTitleSnapshot(), null);
  }

  public boolean isAccessible() {
    return accessState == AccessState.ACCESSIBLE;
  }

  /**
   * @return the live measurement counts of an accessible pin, {@code 0} for a revoked pin
   */
  public long ngsMeasurementCount() {
    return overview == null ? 0 : overview.ngsMeasurementCount();
  }

  public long pxpMeasurementCount() {
    return overview == null ? 0 : overview.pxpMeasurementCount();
  }

  public long ipMeasurementCount() {
    return overview == null ? 0 : overview.ipMeasurementCount();
  }

  /**
   * @return the live last-modified instant of an accessible pin, {@code null} for a revoked pin
   */
  public Instant lastModified() {
    return overview == null ? null : overview.lastModified();
  }
}
