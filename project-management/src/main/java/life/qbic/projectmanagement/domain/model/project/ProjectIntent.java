package life.qbic.projectmanagement.domain.model.project;

import static java.util.Objects.requireNonNull;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import life.qbic.projectmanagement.domain.model.project.translation.ProjectObjectiveConverter;
import life.qbic.projectmanagement.domain.model.project.translation.ProjectTitleConverter;

/**
 * A project intent contains information on the project that is related to the intent of the
 * project.
 */
@Embeddable
public class ProjectIntent {

  @Convert(converter = ProjectTitleConverter.class)
  @Column(name = "projectTitle")
  private ProjectTitle projectTitle;

  @Convert(converter = ProjectObjectiveConverter.class)
  @Column(name = "objective", length = 2000)
  private ProjectObjective projectObjective;

  private ProjectIntent(ProjectTitle projectTitle, ProjectObjective objective) {
    requireNonNull(projectTitle);
    requireNonNull(objective);

    this.projectTitle = projectTitle;
    this.projectObjective = objective;
  }

  protected ProjectIntent() {

  }

  public static ProjectIntent of(ProjectTitle projectTitle, ProjectObjective projectObjective) {
    return new ProjectIntent(projectTitle, projectObjective);
  }

  public ProjectTitle projectTitle() {
    return projectTitle;
  }

  public void projectTitle(ProjectTitle projectTitle) {
    Objects.requireNonNull(projectTitle);
    this.projectTitle = projectTitle;
  }

  public ProjectObjective objective() {
    return projectObjective;
  }

  public void objective(ProjectObjective projectObjective) {
    Objects.requireNonNull(projectObjective);
    this.projectObjective = projectObjective;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    ProjectIntent that = (ProjectIntent) object;

    if (!projectTitle.equals(that.projectTitle)) {
      return false;
    }
    return projectObjective.equals(that.projectObjective);
  }

  @Override
  public int hashCode() {
    int result = projectTitle.hashCode();
    result = 31 * result + projectObjective.hashCode();
    return result;
  }
}
