package life.qbic.datamanager.views.projects.create;

import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.binder.Binder;
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
public class CollaboratorsLayout extends Div implements HasValidation {

  private static final String TITLE = "Project Collaborators";
  private final ContactField principalInvestigatorField = new ContactField(
      "Principal Investigator");
  private final ContactField responsiblePersonField = new ContactField(
      "Project Responsible/Core Investigator (optional)");
  private final ContactField projectManagerField = new ContactField("Project Manager");
  private final Binder<ProjectCollaborators> collaboratorsBinder = new Binder<>(
      ProjectCollaborators.class);

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
    collaboratorsBinder.setBean(new ProjectCollaborators());
    principalInvestigatorField.setRequired(true);
    collaboratorsBinder.bind(principalInvestigatorField,
        ProjectCollaborators::getPrincipalInvestigator,
        ProjectCollaborators::setPrincipalInvestigator);

    responsiblePersonField.setRequired(false);
    responsiblePersonField.setHelperText("Should be contacted about project-related questions");
    collaboratorsBinder.forField(responsiblePersonField)
        .bind(bean -> bean.getResponsiblePerson().orElse(null),
            ProjectCollaborators::setResponsiblePerson);
    projectManagerField.setRequired(true);
    collaboratorsBinder.bind(projectManagerField, (ProjectCollaborators::getProjectManager),
        ProjectCollaborators::setProjectManager);
  }

  private boolean areContactFieldsValid() {
    projectManagerField.validate();
    principalInvestigatorField.validate();
    responsiblePersonField.validate();
    return principalInvestigatorField.isValid() && responsiblePersonField.isValid()
        && projectManagerField.isValid();
  }

  public ProjectCollaborators getCollaboratorInformation() {
    ProjectCollaborators projectCollaborators = new ProjectCollaborators();
    collaboratorsBinder.writeBeanIfValid(projectCollaborators);
    return projectCollaborators;
  }

  /**
   * Sets an error message to the component.
   * <p>
   * The Web Component is responsible for deciding when to show the error message to the user, and
   * this is usually triggered by triggering the invalid state for the Web Component. Which means
   * that there is no need to clean up the message when component becomes valid (otherwise it may
   * lead to undesired visual effects).
   *
   * @param errorMessage a new error message
   */
  @Override
  public void setErrorMessage(String errorMessage) {

  }

  /**
   * Gets current error message from the component.
   *
   * @return current error message
   */
  @Override
  public String getErrorMessage() {
    return "Invalid Input found in Collaborators Layout";
  }

  /**
   * Sets the validity of the component input.
   * <p>
   * When component becomes valid it hides the error message by itself, so there is no need to clean
   * up the error message via the {@link #setErrorMessage(String)} call.
   *
   * @param invalid new value for component input validity
   */
  @Override
  public void setInvalid(boolean invalid) {

  }

  /**
   * Returns {@code true} if component input is invalid, {@code false} otherwise.
   *
   * @return whether the component input is valid
   */
  @Override
  public boolean isInvalid() {
    collaboratorsBinder.validate();
    return !(areContactFieldsValid() && collaboratorsBinder.isValid());
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
