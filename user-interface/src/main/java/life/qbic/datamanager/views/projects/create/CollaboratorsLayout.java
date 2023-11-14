package life.qbic.datamanager.views.projects.create;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;
import life.qbic.datamanager.views.general.contact.Contact;
import life.qbic.datamanager.views.general.contact.ContactField;
import org.springframework.stereotype.Component;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */
@Component
public class CollaboratorsLayout extends Div {
  private static final String TITLE = "Project Collaborators";
  private final ContactField principalInvestigatorField = new ContactField(
      "Principal Investigator");
  private final ContactField responsiblePersonField = new ContactField(
      "Project Responsible (optional)");
  private final ContactField projectManagerField = new ContactField("Project Manager");
  private final Binder<ProjectCollaborators> binder = new Binder<>();

  public CollaboratorsLayout() {
    initLayout();
    initValidation();
  }

  private void initLayout() {
    Span projectContactsTitle = new Span(TITLE);
    projectContactsTitle.addClassName("title");
    Span projectContactsDescription = new Span("Important contact people of the project");
    add(projectContactsTitle);
    add(projectContactsDescription);
    add(principalInvestigatorField);
    add(responsiblePersonField);
    add(projectManagerField);
    addClassName("collaborators-layout");
  }

  private void initValidation() {
    principalInvestigatorField.setRequired(true);
    binder.forField(principalInvestigatorField)
        .bind((ProjectCollaborators::getPrincipalInvestigator),
            ProjectCollaborators::setPrincipalInvestigator);

    responsiblePersonField.setRequired(false);
    responsiblePersonField.setHelperText("Should be contacted about project-related questions");
    binder.forField(responsiblePersonField)
        .bind(projectCollaborators -> projectCollaborators.getResponsiblePerson().orElse(null),
            (projectCollaborators, contact) -> {
              if (contact.getFullName().isEmpty() || contact.getEmail().isEmpty()) {
                projectCollaborators.setResponsiblePerson(null);
              } else {
                projectCollaborators.setResponsiblePerson(contact);
              }
            });
    projectManagerField.setRequired(true);
    binder.forField(projectManagerField)
        .bind((ProjectCollaborators::getProjectManager),
            ProjectCollaborators::setProjectManager);
  }

  private BinderValidationStatus<ProjectCollaborators> validateFields() {
    return binder.validate();
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
      if (!responsiblePerson.equals(that.responsiblePerson)) {
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
