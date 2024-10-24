package life.qbic.datamanager.views.projects.edit;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.UIScope;
import life.qbic.datamanager.views.projects.edit.EditProjectInformationDialog.ProjectInformation;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class EditProjectDesignForm extends FormLayout {

  private final Binder<ProjectInformation> binder;

  private ProjectInformation oldValue;

  private ProjectInformation newValue;

  private TextField titleField;

  public EditProjectDesignForm() {
    super();
    addClassName("form-content");
    binder = new Binder<>(ProjectInformation.class);

    titleField = new TextField("Title");
    titleField.setRequiredIndicatorVisible(true);
    titleField.setId("project-title-field");
    titleField.setValueChangeMode(ValueChangeMode.EAGER);

    binder.forField(titleField).withValidator(it -> !it.isBlank(), "Please provide a project title" )
        .bind(ProjectInformation::getProjectTitle, ProjectInformation::setProjectTitle);

    add(titleField);

  }

  public void setContent(ProjectInformation project) {
    binder.setBean(project);
    binder.readBean(project);
    binder.refreshFields();
//    try {
//      oldValue = new ProjectInformation();
//      binder.writeBean(oldValue);
//    } catch (ValidationException e) {
//      oldValue = null;
//      throw new IllegalArgumentException(
//          "Project information should be valid but was not. " + project, e);
//    }
  }
}
