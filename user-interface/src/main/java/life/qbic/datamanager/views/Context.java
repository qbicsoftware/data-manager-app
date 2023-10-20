package life.qbic.datamanager.views;

import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import life.qbic.controlling.domain.model.project.ProjectId;
import life.qbic.controlling.domain.model.experiment.ExperimentId;

/**
 * The current context information
 *
 * @since 1.0.0
 */
public final class Context {

  private final ProjectId projectId;
  private final ExperimentId experimentId;

  public Context() {
    projectId = null;
    experimentId = null;
  }

  private Context(ProjectId projectId, ExperimentId experimentId) {
    this.projectId = projectId;
    this.experimentId = experimentId;
  }

  /**
   * Updates the context with the given projectId.
   *
   * @param projectId The projectId to update the context with.
   * @return The updated context with the new projectId.
   */
  public Context with(ProjectId projectId) {
    return new Context(projectId, experimentId);
  }

  /**
   * Updates the context with the given experimentId.
   *
   * @param experimentId The experimentId to update the context with.
   * @return The updated context with the new experimentId.
   */
  public Context with(ExperimentId experimentId) {
    return new Context(projectId, experimentId);
  }

  /**
   * Returns the projectId associated with the context, if available.
   *
   * @return An Optional containing the projectId, or an empty Optional if the projectId is null.
   */
  public Optional<ProjectId> projectId() {
    return Optional.ofNullable(projectId);
  }

  /**
   * Returns the experimentId associated with the context, if available.
   *
   * @return An Optional containing the experimentId, or an empty Optional if the experimentId is
   * null.
   */
  public Optional<ExperimentId> experimentId() {
    return Optional.ofNullable(experimentId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Context context = (Context) o;

    if (!Objects.equals(projectId, context.projectId)) {
      return false;
    }
    return Objects.equals(experimentId, context.experimentId);
  }

  @Override
  public int hashCode() {
    int result = projectId != null ? projectId.hashCode() : 0;
    result = 31 * result + (experimentId != null ? experimentId.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", Context.class.getSimpleName() + "[", "]")
        .add("projectId=" + projectId)
        .add("experimentId=" + experimentId)
        .toString();
  }
}
