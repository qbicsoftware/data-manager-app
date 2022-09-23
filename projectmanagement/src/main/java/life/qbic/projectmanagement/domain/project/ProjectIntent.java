package life.qbic.projectmanagement.domain.project;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.Optional;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import life.qbic.projectmanagement.domain.project.repository.jpa.ExperimentalDesignDescriptionConverter;
import life.qbic.projectmanagement.domain.project.repository.jpa.ProjectObjectiveConverter;
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

  @Convert(converter = ProjectObjectiveConverter.class)
  @Column(name = "objective")
  private ProjectObjective projectObjective;

  @Convert(converter = ExperimentalDesignDescriptionConverter.class)
  @Column(name = "experimentalDesignDescription")
  private ExperimentalDesignDescription experimentalDesignDescription;

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
    if (!projectObjective.equals(that.projectObjective)) {
      return false;
    }
    return Objects.equals(experimentalDesignDescription, that.experimentalDesignDescription);
  }

  @Override
  public int hashCode() {
    int result = projectTitle.hashCode();
    result = 31 * result + projectObjective.hashCode();
    result = 31 * result + (experimentalDesignDescription != null
        ? experimentalDesignDescription.hashCode() : 0);
    return result;
  }
}
