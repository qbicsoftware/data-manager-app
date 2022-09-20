package life.qbic.projectmanagement.domain;

import life.qbic.projectmanagement.domain.repository.jpa.ProjectTitleConverter;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;

import static java.util.Objects.requireNonNull;

/**
 * A project intent contains information on the project that is related to the intent of the
 * project.
 */
@Embeddable
public class ProjectIntent {

  @Convert(converter = ProjectTitleConverter.class)
  @Column(name = "projectTitle")
  private ProjectTitle projectTitle;

  public ProjectIntent(ProjectTitle projectTitle) {
    requireNonNull(projectTitle);
    this.projectTitle = projectTitle;
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

    return projectTitle.equals(that.projectTitle);
  }

  @Override
  public int hashCode() {
    return projectTitle.hashCode();
  }
}
