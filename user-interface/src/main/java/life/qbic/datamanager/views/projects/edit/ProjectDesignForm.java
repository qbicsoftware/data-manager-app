package life.qbic.datamanager.views.projects.edit;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.value.ValueChangeMode;
import life.qbic.datamanager.views.general.utils.Constants;
import life.qbic.datamanager.views.general.utils.Utility;
import life.qbic.datamanager.views.projects.edit.EditProjectInformationDialog.ProjectInformation;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class ProjectDesignForm extends FormLayout {

  private final Binder<ProjectInformation> binder;

  private ProjectInformation oldValue;

  private ProjectInformation newValue;

  private TextField titleField;

  private TextArea objectiveField;

  public ProjectDesignForm() {
    super();
    addClassName("form-content");
    binder = new Binder<>(ProjectInformation.class);

    titleField = new TextField("Title");
    titleField.setRequiredIndicatorVisible(true);
    titleField.setId("project-title-field");
    titleField.setValueChangeMode(ValueChangeMode.EAGER);
    titleField.setMaxLength(Constants.PROJECT_TITLE_MAX_LENGTH);
    Utility.addConsumedLengthHelper(titleField);
    titleField.addValueChangeListener(event -> Utility.addConsumedLengthHelper(event.getSource()));

    objectiveField = new TextArea("Objective");
    objectiveField.setRequiredIndicatorVisible(true);
    objectiveField.addClassName("medium-text-area");
    objectiveField.setMaxLength(Constants.PROJECT_OBJECTIVE_MAX_LENGTH);
    objectiveField.setValueChangeMode(ValueChangeMode.EAGER);
    Utility.addConsumedLengthHelper(objectiveField);
    objectiveField.addValueChangeListener(event -> Utility.addConsumedLengthHelper(event.getSource()));

    binder.forField(titleField).withValidator(it -> !it.isBlank(), "Please provide a project title" )
        .bind(ProjectInformation::getProjectTitle, ProjectInformation::setProjectTitle);

    binder.forField(objectiveField).withValidator(it -> !it.isBlank(), "Please provide a project objective" )
        .bind(ProjectInformation::getProjectObjective, ProjectInformation::setProjectObjective);

    add( titleField, objectiveField);
    setColspan(titleField, 2);
    setColspan(objectiveField, 2);

  }

  public void setContent(ProjectInformation project) {
    oldValue = ProjectInformation.copy(project);
    binder.setBean(project);
  }

  public ProjectInformation fromUserInput() throws ValidationException {
    var projectInfo = new ProjectInformation();
    binder.writeBean(projectInfo);
    return projectInfo;
  }

  public boolean isValid() {
    return binder.isValid();
  }

  public boolean hasChanges() {
    return binder.hasChanges() || hasChanged(oldValue, binder.getBean());
  }

  private boolean hasChanged(ProjectInformation oldValue, ProjectInformation newValue) {
    return !oldValue.equals(newValue);
  }

}
