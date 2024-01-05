package life.qbic.datamanager.views.projects.create;

import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.binder.Binder;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.datamanager.views.general.contact.AutocompleteContactField;
import life.qbic.datamanager.views.general.contact.Contact;
import life.qbic.datamanager.views.general.contact.ContactField;
import org.springframework.stereotype.Component;

/**
 * <b>Collaborators Layout</b>
 *
 * <p>Layout which enables the user to input the information associated with the collaborators
 * within a project during project creation and validates the provided information</p>
 */
@Component
public class CollaboratorsLayout extends Div implements HasValidation {

  private final AutocompleteContactField principalInvestigatorField;
  private final ContactField responsiblePersonField;
  private final AutocompleteContactField projectManagerField;
  private final Binder<ProjectCollaborators> collaboratorsBinder;

  public CollaboratorsLayout() {

    collaboratorsBinder = new Binder<>(ProjectCollaborators.class);
    collaboratorsBinder.setBean(new ProjectCollaborators());

    principalInvestigatorField = new AutocompleteContactField("Principal Investigator");
    principalInvestigatorField.setRequired(true);
    collaboratorsBinder.forField(principalInvestigatorField)
        .withNullRepresentation(principalInvestigatorField.getEmptyValue())
        .withValidator(it -> principalInvestigatorField.isValid(), "")
        .bind(ProjectCollaborators::getPrincipalInvestigator,
            ProjectCollaborators::setPrincipalInvestigator);

    responsiblePersonField = new ContactField("Project Responsible/Core Investigator (optional)");
    responsiblePersonField.setRequired(false);
    responsiblePersonField.setHelperText("Should be contacted about project-related questions");
    collaboratorsBinder.forField(responsiblePersonField)
        .withNullRepresentation(responsiblePersonField.getEmptyValue())
        .withValidator(it -> responsiblePersonField.isValid(), "")
        .bind(bean -> bean.getResponsiblePerson().orElse(null),
            ProjectCollaborators::setResponsiblePerson);

    projectManagerField = new AutocompleteContactField("Project Manager");
    projectManagerField.setRequired(true);
    collaboratorsBinder.forField(projectManagerField)
        .withNullRepresentation(projectManagerField.getEmptyValue())
        .withValidator(it -> projectManagerField.isValid(), "")
        .bind(ProjectCollaborators::getProjectManager,
            ProjectCollaborators::setProjectManager);

    Span projectContactsTitle = new Span("Project Collaborators");
    projectContactsTitle.addClassName("title");
    Span projectContactsDescription = new Span("Important contact people of the project");
    addClassName("collaborators-layout");
    add(projectContactsTitle,
        projectContactsDescription,
        principalInvestigatorField,
        responsiblePersonField,
        projectManagerField);
  }

  public void setProjectManagers(List<Contact> projectManagers) {
    projectManagerField.setItems(projectManagers);
  }

  public void setPrincipalInvestigators(List<Contact> principalInvestigators) {
    principalInvestigatorField.setItems(principalInvestigators);
  }

  public Binder<ProjectCollaborators> getBinder() {
    return collaboratorsBinder;
  }

  @Override
  public void setErrorMessage(String errorMessage) {
  }

  @Override
  public String getErrorMessage() {
    return null;
  }

  @Override
  public void setInvalid(boolean invalid) {

  }

  @Override
  public boolean isInvalid() {
    return getBinder().validate().hasErrors();
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

      if (!principalInvestigator.equals(that.principalInvestigator)) {
        return false;
      }
      if (!Objects.equals(responsiblePerson, that.responsiblePerson)) {
        return false;
      }
      return projectManager.equals(that.projectManager);
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
