package life.qbic.projectmanagement.domain.project;

import life.qbic.projectmanagement.domain.project.repository.jpa.ProjectObjectiveConverter;
import life.qbic.projectmanagement.domain.project.repository.jpa.ProjectTitleConverter;

import static java.util.Objects.requireNonNull;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;

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
  @Column(name = "objective")
  private ProjectObjective projectObjective;

  public ProjectIntent(ProjectTitle projectTitle, ProjectObjective objective) {
    requireNonNull(projectTitle);
    requireNonNull(objective);

    this.projectTitle = projectTitle;
    this.projectObjective = objective;
  }

  protected ProjectIntent() {

  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ProjectIntent that = (ProjectIntent) o;

    return projectTitle.equals(that.projectTitle) && projectObjective.equals(that.projectObjective);
  }

  @Override
  public int hashCode() {
    return projectTitle.hashCode(); //todo integrate objective hash -> du it with project intent?
  }
}
