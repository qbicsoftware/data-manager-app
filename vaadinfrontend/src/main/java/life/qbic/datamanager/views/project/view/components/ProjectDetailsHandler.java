package life.qbic.datamanager.views.project.view.components;

import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class ProjectDetailsHandler {

  private final ProjectDetailsComponent component;
  private final ProjectInformationService projectInformationService;

  private ProjectId selectedProject;

  public ProjectDetailsHandler(ProjectDetailsComponent component,
      ProjectInformationService projectInformationService) {
    this.component = component;
    this.projectInformationService = projectInformationService;
  }

  public void projectId(String projectId) {
    projectInformationService.find(ProjectId.parse(projectId)).ifPresentOrElse(
        this::loadProjectData,
        () -> component.titleField.setValue("Not found"));
  }

  public void loadProjectData(Project project) {
    this.selectedProject = project.getId();
    component.titleField.setValue(project.getProjectIntent().projectTitle().title());
    component.projectObjective.setValue(project.getProjectIntent().objective().value());
    project.getProjectIntent().experimentalDesign().ifPresentOrElse(
        experimentalDesignDescription -> component.experimentalDesignField.setValue(
            experimentalDesignDescription.value()),
        () -> component.experimentalDesignField.setPlaceholder("No description yet."));
  }

}
