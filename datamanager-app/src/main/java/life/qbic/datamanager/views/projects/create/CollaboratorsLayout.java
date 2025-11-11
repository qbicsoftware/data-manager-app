package life.qbic.datamanager.views.projects.create;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.binder.ValidationException;
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
   * Returns the project collaborators. Fails for invalid collaborators with an exception.
   *
   * @return a valid project design
   */
  public ProjectCollaborators getProjectCollaborators() {
    try {
      //Necessary since otherwise an empty contact will be generated, which will fail during project creation in the application service
      Contact responsiblePerson = Contact.empty();
      if (responsibleBinding.getValue().hasMinimalInformation()) {
        responsiblePerson = responsibleBinding.getValue();
      }
      return new ProjectCollaborators(investigatorBinding.getValue(), responsiblePerson,
          managerBinding.getValue());
    } catch (ValidationException e) {
      throw new ApplicationException("Tried to access invalid project collaborators.", e);
    }
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

  public record ProjectCollaborators(Contact principalInvestigator, Contact responsiblePerson,
                                     Contact projectManager) {

  }
}
