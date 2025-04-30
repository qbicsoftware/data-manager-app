package life.qbic.datamanager.views.projects.create;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.binder.ValidationException;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.general.contact.BoundContactField;
import life.qbic.datamanager.views.general.contact.Contact;
import life.qbic.datamanager.views.general.contact.ContactField;
import life.qbic.datamanager.views.general.utils.Utility;
import life.qbic.projectmanagement.application.contact.PersonLookupService;

/**
 * <b>Collaborators Layout</b>
 *
 * <p>Layout which enables the user to input the information associated with the collaborators
 * within a project during project creation and validates the provided information</p>
 */

public class CollaboratorsLayout extends Div {

  protected transient BoundContactField managerBinding;
  protected transient BoundContactField investigatorBinding;
  protected transient BoundContactField responsibleBinding;

  public CollaboratorsLayout(PersonLookupService personLookupService) {

    var fieldPrincipalInvestigator = createRequired("Principal Investigator", personLookupService);
    var fieldProjectResponsible = createOptional(
        "Project Responsible / Co-Investigator (optional)", personLookupService);
    var fieldProjectManager = createRequired("Project Manager", personLookupService);
    var currentUser = Utility.tryToLoadFromPrincipal().orElseThrow();
    fieldProjectManager.setMyself(currentUser, "Set myself as project manager");
    fieldProjectResponsible.setMyself(currentUser, "Set myself as project responsible");
    fieldPrincipalInvestigator.setMyself(currentUser, "Set myself as principal investigator");
    this.responsibleBinding = BoundContactField.createOptional(fieldProjectResponsible);
    this.investigatorBinding = BoundContactField.createMandatory(fieldPrincipalInvestigator);
    this.managerBinding = BoundContactField.createMandatory(fieldProjectManager);
    Span projectContactsTitle = new Span("Project Collaborators");
    projectContactsTitle.addClassName("title");
    Span projectContactsDescription = new Span(
        "Add the names and email address of the important contact people of the project.");
    addClassName("collaborators-layout");
    add(projectContactsTitle,
        projectContactsDescription,
        fieldPrincipalInvestigator,
        fieldProjectResponsible,
        fieldProjectManager);
  }

  private static ContactField createRequired(String label,
      PersonLookupService personLookupService) {
    var contact = ContactField.createSimple(label, personLookupService);
    contact.setRequiredIndicatorVisible(true);
    return contact;
  }

  private static ContactField createOptional(String label,
      PersonLookupService personLookupService) {
    return ContactField.createSimple(label, personLookupService);
  }

  /**
   * Returns the project design. Fails for invalid designs with an exception.
   *
   * @return a valid project design
   */
  public ProjectCollaborators getProjectCollaborators() {
    ProjectCollaborators projectCollaborators = new ProjectCollaborators();
    try {
      projectCollaborators.setProjectManager(managerBinding.getValue());
      projectCollaborators.setPrincipalInvestigator(investigatorBinding.getValue());
      //Necessary since otherwise an empty contact will be generated, which will fail during project creation in the application service
      if (responsibleBinding.getValue().hasMinimalInformation()) {
        projectCollaborators.setResponsiblePerson(responsibleBinding.getValue());
      }
    } catch (ValidationException e) {
      throw new ApplicationException("Tried to access invalid project collaborators.", e);
    }
    return projectCollaborators;
  }

  /**
   * Checks if the contained fields within the collaborator layout are invalid.
   * Necessary since the binding is now done on a field level and not on one binder for all fields
   *
   * @return true if the contained fields are invalid, false otherwise
   */
  public boolean isInvalid() {
    return !(investigatorBinding.isValid() && managerBinding.isValid()
        && responsibleBinding.isValid());
  }

  public static final class ProjectCollaborators implements Serializable {

    @Serial
    private static final long serialVersionUID = 8403602920219892326L;
    @NotEmpty
    private Contact principalInvestigator;
    private Contact responsiblePerson;
    @NotEmpty
    private Contact projectManager;

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
