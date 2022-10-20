package life.qbic.datamanager.views.project.view.components;

import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.ExperimentalDesignDescription;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.ProjectObjective;
import life.qbic.projectmanagement.domain.project.ProjectTitle;

/**
 * Component logic for the {@link ProjectDetailsComponent}
 *
 * @since 1.0.0
 */
class ProjectDetailsHandler {

  private final ProjectDetailsComponent component;
  private final ProjectInformationService projectInformationService;

  private ProjectId selectedProject;

  public ProjectDetailsHandler(ProjectDetailsComponent component,
      ProjectInformationService projectInformationService) {
    this.component = component;
    this.projectInformationService = projectInformationService;
    restrictInputLength();
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

  private void restrictInputLength() {
    TextField titleField = component.titleField;
    TextArea projectObjective = component.projectObjective;
    TextArea experimentalDesignField = component.experimentalDesignField;

    titleField.setMaxLength((int) ProjectTitle.maxLength());
    projectObjective.setMaxLength((int) ProjectObjective.maxLength());
    experimentalDesignField.setMaxLength(
        (int) ExperimentalDesignDescription.maxLength());

    titleField.setValueChangeMode(ValueChangeMode.EAGER);
    projectObjective.setValueChangeMode(ValueChangeMode.EAGER);
    experimentalDesignField.setValueChangeMode(ValueChangeMode.EAGER);

    addConsumedLengthHelper(titleField, titleField.getValue());
    addConsumedLengthHelper(projectObjective, projectObjective.getValue());
    addConsumedLengthHelper(experimentalDesignField, experimentalDesignField.getValue());

    titleField.addValueChangeListener(
        e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
    projectObjective.addValueChangeListener(
        e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
    experimentalDesignField.addValueChangeListener(
        e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
  }

  private void addConsumedLengthHelper(TextArea textArea, String newValue) {
    int maxLength = textArea.getMaxLength();
    int consumedLength = newValue.length();
    textArea.setHelperText(consumedLength + "/" + maxLength);
  }

  private void addConsumedLengthHelper(TextField textField, String newValue) {
    int maxLength = textField.getMaxLength();
    int consumedLength = newValue.length();
    textField.setHelperText(consumedLength + "/" + maxLength);
  }

}
