package life.qbic.datamanager.views.projects.edit;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.general.contact.Contact;
import life.qbic.datamanager.views.general.contact.ContactField;
import life.qbic.projectmanagement.domain.project.ProjectObjective;
import life.qbic.projectmanagement.domain.project.ProjectTitle;

/**
 * <b>Project Information Dialog</b>
 *
 * <p>Dialog to create a project based on a project intent or to update a project's information</p>
 *
 * @since 1.0.0
 */
@SpringComponent
@UIScope
public class EditProjectInformationDialog extends DialogWindow {

  @Serial
  private static final long serialVersionUID = 7327075228498213661L;

  private final Binder<ProjectInformation> binder;

  private ProjectInformation oldValue = new ProjectInformation();
  private final TextField titleField;
  private final TextArea projectObjective;
  private final ContactField principalInvestigatorField;
  private final ContactField responsiblePersonField;
  private final ContactField projectManagerField;

  public EditProjectInformationDialog() {
    super();

    addClassName("create-project-dialog");
    setHeaderTitle("Project Information");
    setConfirmButtonLabel("Save");
    confirmButton.addClickListener(this::onConfirmClicked);
    setCancelButtonLabel("Cancel");
    cancelButton.addClickListener(this::onCancelClicked);

    binder = new Binder<>();

    titleField = new TextField("Title");
    titleField.addClassName("title");
    titleField.setId("project-title-field");
    titleField.setRequired(true);
    restrictProjectTitleLength();
    binder.forField(titleField)
        .withValidator(it -> !it.isBlank(), "Please provide a project title")
        .bind(ProjectInformation::getProjectTitle, ProjectInformation::setProjectTitle);

    projectObjective = new TextArea("Objective");
    projectObjective.setRequired(true);
    restrictProjectObjectiveLength();
    binder.forField(projectObjective)
        .withValidator(value -> !value.isBlank(), "Please provide an objective")
        .bind(ProjectInformation::getProjectObjective, ProjectInformation::setProjectObjective);

    Div projectContactsLayout = new Div();
    projectContactsLayout.setClassName("project-contacts");

    Span projectContactsTitle = new Span("Project Contacts");
    projectContactsTitle.addClassName("title");

    Span projectContactsDescription = new Span("Important contact people of the project");

    projectContactsLayout.add(projectContactsTitle);
    projectContactsLayout.add(projectContactsDescription);

    principalInvestigatorField = new ContactField("Principal Investigator");
    principalInvestigatorField.setRequired(true);
    principalInvestigatorField.setId("principal-investigator");
    binder.forField(principalInvestigatorField)
        .bind(ProjectInformation::getPrincipalInvestigator,
            ProjectInformation::setPrincipalInvestigator);

    responsiblePersonField = new ContactField("Project Responsible (optional)");
    responsiblePersonField.setRequired(false);
    responsiblePersonField.setId("responsible-person");
    responsiblePersonField.setHelperText("Should be contacted about project-related questions");
    binder.forField(responsiblePersonField)
        .bind(projectInformation -> projectInformation.getResponsiblePerson().orElse(null),
            ProjectInformation::setResponsiblePerson);

    projectManagerField = new ContactField("Project Manager");
    projectManagerField.setRequired(true);
    projectManagerField.setId("project-manager");
    binder.forField(projectManagerField)
        .bind(ProjectInformation::getProjectManager, ProjectInformation::setProjectManager);

    // Calls the reset method for all possible closure methods of the dialogue window:
    addDialogCloseActionListener(closeActionEvent -> close());
    cancelButton.addClickListener(buttonClickEvent -> close());

    FormLayout formLayout = new FormLayout();
    formLayout.addClassName("form-content");
    formLayout.add(
        titleField,
        projectObjective,
        projectContactsLayout,
        principalInvestigatorField,
        responsiblePersonField,
        projectManagerField
    );
    formLayout.setColspan(titleField, 2);
    formLayout.setColspan(projectObjective, 2);
    formLayout.setColspan(principalInvestigatorField, 2);
    formLayout.setColspan(responsiblePersonField, 2);
    formLayout.setColspan(projectManagerField, 2);
    add(formLayout);
  }

  public void setProjectInformation(ProjectInformation projectInformation) {
    binder.setBean(projectInformation);
    try {
      oldValue = new ProjectInformation();
      binder.writeBean(oldValue);
    } catch (ValidationException e) {
      oldValue = null;
      throw new IllegalArgumentException(
          "Project information should be valid but was not. " + projectInformation, e);
    }
  }

  private void onConfirmClicked(ClickEvent<Button> clickEvent) {
    ProjectInformation projectInformation = new ProjectInformation();
    try {
      binder.writeBean(projectInformation);
      fireEvent(
          new ProjectUpdateEvent(oldValue, projectInformation, this, clickEvent.isFromClient()));
    } catch (ValidationException e) {
      validate();
    }
  }

  private void validate() {
    binder.validate();
    principalInvestigatorField.validate();
    responsiblePersonField.validate();
    projectManagerField.validate();
  }

  private void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
    close();
  }

  @Override
  public void close() {
    super.close();
    reset();
  }

  /**
   * Resets the values and validity of all components that implement value storing and validity
   * interfaces
   */
  public void reset() {
    principalInvestigatorField.clear();
    projectManagerField.clear();
    binder.setBean(new ProjectInformation());
  }

  private void restrictProjectObjectiveLength() {
    projectObjective.setValueChangeMode(ValueChangeMode.EAGER);
    projectObjective.setMaxLength((int) ProjectObjective.maxLength());
    addConsumedLengthHelper(projectObjective, projectObjective.getValue());
    projectObjective.addValueChangeListener(
        e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
  }

  private void restrictProjectTitleLength() {
    titleField.setMaxLength((int) ProjectTitle.maxLength());
    titleField.setValueChangeMode(ValueChangeMode.EAGER);
    addConsumedLengthHelper(titleField, titleField.getValue());
    titleField.addValueChangeListener(e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
  }

  private static void addConsumedLengthHelper(TextField textField, String newValue) {
    int maxLength = textField.getMaxLength();
    int consumedLength = newValue.length();
    textField.setHelperText(consumedLength + "/" + maxLength);
  }

  private static void addConsumedLengthHelper(TextArea textArea, String newValue) {
    int maxLength = textArea.getMaxLength();
    int consumedLength = newValue.length();
    textArea.setHelperText(consumedLength + "/" + maxLength);
  }

  public void addProjectUpdateEventListener(ComponentEventListener<ProjectUpdateEvent> listener) {
    addListener(ProjectUpdateEvent.class, listener);
  }

  public void addCancelListener(ComponentEventListener<CancelEvent> listener) {
    addListener(CancelEvent.class, listener);
  }

  public static class CancelEvent extends
      life.qbic.datamanager.views.events.UserCancelEvent<EditProjectInformationDialog> {

    public CancelEvent(EditProjectInformationDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  /**
   * <b>Project Update Event</b>
   *
   * <p>Indicates that a user submitted a project update request</p>
   *
   * @since 1.0.0
   */
  public static class ProjectUpdateEvent extends ComponentEvent<EditProjectInformationDialog> {

    @Serial
    private static final long serialVersionUID = 1072173555312630829L;

    private final ProjectInformation oldValue;
    private final ProjectInformation value;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     * @param oldValue   the project information before modification
     * @param value      the modified project information
     */
    public ProjectUpdateEvent(ProjectInformation oldValue, ProjectInformation value,
        EditProjectInformationDialog source, boolean fromClient) {
      super(source, fromClient);
      requireNonNull(value, "new project information (value) must not be null");
      this.oldValue = oldValue;
      this.value = value;
    }

    public Optional<ProjectInformation> getOldValue() {
      return Optional.ofNullable(oldValue);
    }

    public ProjectInformation getValue() {
      return value;
    }
  }

  public static final class ProjectInformation implements Serializable {

    @Serial
    private static final long serialVersionUID = -7260109309939021850L;

    @NotEmpty
    private String projectTitle = "";
    @NotEmpty
    private String projectObjective = "";
    @NotEmpty
    private Contact principalInvestigator;
    private Contact responsiblePerson;
    @NotEmpty
    private Contact projectManager;

    public void setProjectTitle(String projectTitle) {
      this.projectTitle = projectTitle;
    }

    public void setProjectObjective(String projectObjective) {
      this.projectObjective = projectObjective;
    }

    public Contact getPrincipalInvestigator() {
      return principalInvestigator;
    }

    public void setPrincipalInvestigator(
        Contact principalInvestigator) {
      this.principalInvestigator = principalInvestigator;
    }

    public Optional<Contact> getResponsiblePerson() {
      return Optional.ofNullable(responsiblePerson);
    }

    public void setResponsiblePerson(Contact responsiblePerson) {
      this.responsiblePerson = responsiblePerson;
    }

    public Contact getProjectManager() {
      return projectManager;
    }

    public void setProjectManager(Contact projectManager) {
      this.projectManager = projectManager;
    }

    public String getProjectTitle() {
      return projectTitle;
    }

    public String getProjectObjective() {
      return projectObjective;
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }
      if (object == null || getClass() != object.getClass()) {
        return false;
      }

      ProjectInformation that = (ProjectInformation) object;

      if (!Objects.equals(projectTitle, that.projectTitle)) {
        return false;
      }
      if (!Objects.equals(projectObjective, that.projectObjective)) {
        return false;
      }
      if (!Objects.equals(principalInvestigator, that.principalInvestigator)) {
        return false;
      }
      if (!Objects.equals(responsiblePerson, that.responsiblePerson)) {
        return false;
      }
      return Objects.equals(projectManager, that.projectManager);
    }

    @Override
    public int hashCode() {
      int result = projectTitle != null ? projectTitle.hashCode() : 0;
      result = 31 * result + (projectObjective != null ? projectObjective.hashCode() : 0);
      result = 31 * result + (principalInvestigator != null ? principalInvestigator.hashCode() : 0);
      result = 31 * result + (responsiblePerson != null ? responsiblePerson.hashCode() : 0);
      result = 31 * result + (projectManager != null ? projectManager.hashCode() : 0);
      return result;
    }

    @Override
    public String toString() {
      return new StringJoiner(", ", ProjectInformation.class.getSimpleName() + "[", "]")
          .add("projectTitle='" + projectTitle + "'")
          .add("projectObjective='" + projectObjective + "'")
          .add("principalInvestigator=" + principalInvestigator)
          .add("responsiblePerson=" + responsiblePerson)
          .add("projectManager=" + projectManager)
          .toString();
    }
  }
}
