package life.qbic.projectmanagement.domain.project;

import life.qbic.projectmanagement.domain.project.repository.jpa.ProjectObjectiveConverter;
import life.qbic.projectmanagement.domain.project.repository.jpa.ProjectTitleConverter;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.Optional;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import life.qbic.projectmanagement.domain.project.repository.jpa.ExperimentalDesignDescriptionConverter;
import life.qbic.projectmanagement.domain.project.repository.jpa.ProjectTitleConverter;
import java.util.Objects;

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

  public ProjectIntent(ProjectTitle projectTitle, ProjectObjective objective) {
    requireNonNull(projectTitle);
    requireNonNull(objective);

    this.projectTitle = projectTitle;
    this.projectObjective = objective;
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

  //FIXME equals and hash
}
