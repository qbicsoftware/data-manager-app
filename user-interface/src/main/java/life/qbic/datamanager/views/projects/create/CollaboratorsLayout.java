package life.qbic.datamanager.views.projects.create;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.general.HasBinderValidation;
import life.qbic.datamanager.views.general.contact.AutocompleteContactField;
import life.qbic.datamanager.views.general.contact.Contact;
import life.qbic.datamanager.views.projects.create.CollaboratorsLayout.ProjectCollaborators;
import org.checkerframework.checker.units.qual.A;
import org.springframework.stereotype.Component;

/**
 * <b>Collaborators Layout</b>
 *
 * <p>Layout which enables the user to input the information associated with the collaborators
 * within a project during project creation and validates the provided information</p>
 */
@Component
public class CollaboratorsLayout extends Div implements HasBinderValidation<ProjectCollaborators> {

  private final AutocompleteContactField principalInvestigatorField;
  private final AutocompleteContactField responsiblePersonField;
  private final AutocompleteContactField projectManagerField;
  private final Binder<ProjectCollaborators> collaboratorsBinder;

  public CollaboratorsLayout() {

    collaboratorsBinder = new Binder<>(ProjectCollaborators.class);
    collaboratorsBinder.setValidatorsDisabled(true);
    collaboratorsBinder.setBean(new ProjectCollaborators());
    collaboratorsBinder.setFieldsValidationStatusChangeListenerEnabled(true);

    principalInvestigatorField = new AutocompleteContactField("Principal Investigator", "PI");
    principalInvestigatorField.setRequired(true);
    collaboratorsBinder.forField(principalInvestigatorField)
        .withNullRepresentation(principalInvestigatorField.getEmptyValue())
        .withValidator(it -> principalInvestigatorField.validate().isValid(), "")
        .bind(ProjectCollaborators::getPrincipalInvestigator,
            ProjectCollaborators::setPrincipalInvestigator);

    responsiblePersonField = new AutocompleteContactField("Project Responsible/Co-Investigator (optional)", "Responsible");
    responsiblePersonField.setRequired(false);
    responsiblePersonField.setHelperText("Should be contacted about project-related questions");
    collaboratorsBinder.forField(responsiblePersonField)
        .withNullRepresentation(responsiblePersonField.getEmptyValue())
        .withValidator(it -> responsiblePersonField.validate().isValid(), "")
        .bind(bean -> bean.getResponsiblePerson().orElse(null),
            ProjectCollaborators::setResponsiblePerson);

    projectManagerField = new AutocompleteContactField("Project Manager", "Manager");
    projectManagerField.setRequired(true);
    collaboratorsBinder.forField(projectManagerField)
        .withNullRepresentation(projectManagerField.getEmptyValue())
        .withValidator(it -> projectManagerField.validate().isValid(), "")
        .bind(ProjectCollaborators::getProjectManager,
            ProjectCollaborators::setProjectManager);

    Span projectContactsTitle = new Span("Project Collaborators");
    projectContactsTitle.addClassName("title");
    Span projectContactsDescription = new Span(
        "Add the names and email address of the important contact people of the project.");
    addClassName("collaborators-layout");
    add(projectContactsTitle,
        projectContactsDescription,
        principalInvestigatorField,
        responsiblePersonField,
        projectManagerField);
    collaboratorsBinder.setValidatorsDisabled(false);
  }

  @Override
  public String getDefaultErrorMessage() {
    return "Please complete the mandatory information. Some input seems to be invalid.";
  }

  @Override
  public boolean isInvalid() {
    validate();
    return HasBinderValidation.super.isInvalid();
  }

  public void setProjectManagers(List<Contact> projectManagers) {
    projectManagerField.setItems(projectManagers);
  }

  public void setResponsiblePersons(List<Contact> contactPersons) {
    responsiblePersonField.setItems(contactPersons);
  }
  public void setPrincipalInvestigators(List<Contact> principalInvestigators) {
    principalInvestigatorField.setItems(principalInvestigators);
  }

  @Override
  public Binder<ProjectCollaborators> getBinder() {
    return collaboratorsBinder;
  }

  /**
   * Provides set project collaborators. Calling this method will lead to an exception if the
   * current value is not valid.
   *
   * @return a valid {@link ProjectCollaborators} object
   */
  public ProjectCollaborators getProjectCollaborators() {
    ProjectCollaborators projectCollaborators = new ProjectCollaborators();
    try {
      collaboratorsBinder.writeBean(projectCollaborators);
    } catch (ValidationException e) {
      throw new ApplicationException("Tried to access invalid project collaborator information.", e);
    }
    return projectCollaborators;
  }

  public void setKnownContacts(List<Contact> knownContacts) {
    principalInvestigatorField.setItems(knownContacts);
    responsiblePersonField.setItems(knownContacts);
    projectManagerField.setItems(knownContacts);
  }

  public void hideContactBox() {
    principalInvestigatorField.hideContactBox();
    responsiblePersonField.hideContactBox();
    projectManagerField.hideContactBox();
  }

  public static final class ProjectCollaborators implements Serializable {

    @Serial
    private static final long serialVersionUID = 8403602920219892326L;
    @NotEmpty
    private Contact principalInvestigator;
    private Contact responsiblePerson;
    @NotEmpty
    private Contact projectManager;

    public void setPrincipalInvestigator(
        Contact principalInvestigator) {
      this.principalInvestigator = principalInvestigator;
    }

    public void setResponsiblePerson(Contact responsiblePerson) {
      this.responsiblePerson = responsiblePerson;
    }

    public void setProjectManager(Contact projectManager) {
      this.projectManager = projectManager;
    }

    public Contact getPrincipalInvestigator() {
      return principalInvestigator;
    }

    public Optional<Contact> getResponsiblePerson() {
      return Optional.ofNullable(responsiblePerson);
    }

    public Contact getProjectManager() {
      return projectManager;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      ProjectCollaborators that = (ProjectCollaborators) o;

      if (!Objects.equals(principalInvestigator, that.principalInvestigator)) {
        return false;
      }
      if (!Objects.equals(responsiblePerson, that.responsiblePerson)) {
        return false;
      }
      return (!Objects.equals(projectManager, that.projectManager));
    }

    @Override
    public int hashCode() {
      int result = principalInvestigator.hashCode();
      result = 31 * result + responsiblePerson.hashCode();
      result = 31 * result + projectManager.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return "ProjectCollaborators{" +
          "principalInvestigator=" + principalInvestigator +
          ", responsiblePerson=" + responsiblePerson +
          ", projectManager=" + projectManager +
          '}';
    }
  }
}
