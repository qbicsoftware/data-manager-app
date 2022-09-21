package life.qbic.projectmanagement.domain.project;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.Optional;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import life.qbic.projectmanagement.domain.project.repository.jpa.ExperimentalDesignDescriptionConverter;
import life.qbic.projectmanagement.domain.project.repository.jpa.ProjectTitleConverter;

/**
 * A project intent contains information on the project that is related to the intent of the
 * project.
 */
@Embeddable
public class ProjectIntent {

  @Convert(converter = ProjectTitleConverter.class)
  @Column(name = "projectTitle")
  private ProjectTitle projectTitle;

  @Convert(converter = ExperimentalDesignDescriptionConverter.class)
  @Column(name = "experimentalDesignDescription")
  private ExperimentalDesignDescription experimentalDesignDescription;

  public ProjectIntent(ProjectTitle projectTitle) {
    requireNonNull(projectTitle);
    this.projectTitle = projectTitle;
  }

  protected ProjectIntent() {

  }

  public ProjectIntent with(ExperimentalDesignDescription experimentalDesignDescription) {
    if (Objects.isNull(experimentalDesignDescription)) {
      throw new IllegalArgumentException("experimental design is null");
    }
    Objects.requireNonNull(experimentalDesignDescription);
    this.experimentalDesignDescription = experimentalDesignDescription;
    return this;
  }

  public ProjectTitle projectTitle() {
    return projectTitle;
  }

  public Optional<ExperimentalDesignDescription> experimentalDesign() {
    return Optional.ofNullable(experimentalDesignDescription);
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

    if (!projectTitle.equals(that.projectTitle)) {
      return false;
    }
    return Objects.equals(experimentalDesignDescription, that.experimentalDesignDescription);
  }

  @Override
  public int hashCode() {
    int result = projectTitle.hashCode();
    result = 31 * result + (experimentalDesignDescription != null
        ? experimentalDesignDescription.hashCode() : 0);
    return result;
  }
}
