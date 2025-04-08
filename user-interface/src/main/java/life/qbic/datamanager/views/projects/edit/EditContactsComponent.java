package life.qbic.datamanager.views.projects.edit;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.binder.ValidationException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import life.qbic.datamanager.views.general.contact.BoundContactField;
import life.qbic.datamanager.views.general.contact.Contact;
import life.qbic.datamanager.views.general.contact.ContactField;
import life.qbic.datamanager.views.general.contact.ContactsForm;
import life.qbic.datamanager.views.general.dialog.InputValidation;
import life.qbic.datamanager.views.general.dialog.UserInput;
import life.qbic.datamanager.views.projects.ProjectInformation;
import life.qbic.projectmanagement.application.contact.PersonLookupService;
import org.springframework.lang.NonNull;

/**
 * <b>Edit Contacts Component</b>
 * <p>
 * Component that can be displayed to the user, when they want to edit contact information.
 *
 * @since 1.10.0
 */
public class EditContactsComponent extends FormLayout implements UserInput {

  private final ProjectInformation projectInformation;

  private transient BoundContactField managerBinding;
  private transient BoundContactField investigatorBinding;
  private transient BoundContactField projectResponsibleBinding;

  public EditContactsComponent(ProjectInformation projectInformation, Contact currentUser,
      PersonLookupService personLookupService) {
    super();

    var content = new Div();
    content.addClassNames("vertical-list");
    var fieldPrincipalInvestigator = createRequired("Principal Investigator", personLookupService);
    var fieldProjectResponsible = createOptional(
        "Project Responsible / Co-Investigator (optional)", personLookupService);
    var fieldProjectManager = createRequired("Project Manager", personLookupService);

    this.projectInformation = Objects.requireNonNull(projectInformation);
    this.investigatorBinding = BoundContactField.createMandatory(fieldPrincipalInvestigator);
    this.managerBinding = BoundContactField.createMandatory(fieldProjectManager);
    this.projectResponsibleBinding = BoundContactField.createOptional(fieldProjectResponsible);

    //We need to load the current user into the dialog so we can compare which type of user was provided
    fieldProjectManager.setMyself(currentUser, "Set myself as project manager");
    fieldProjectResponsible.setMyself(currentUser, "Set myself as project responsible");
    fieldPrincipalInvestigator.setMyself(currentUser, "Set myself as principal investigator");

    investigatorBinding.setValue(projectInformation.getPrincipalInvestigator());
    managerBinding.setValue(projectInformation.getProjectManager());
    projectInformation.getResponsiblePerson()
        .ifPresent(contact -> projectResponsibleBinding.setValue(contact));

    content.add(
        new ContactsForm(fieldPrincipalInvestigator, fieldProjectResponsible, fieldProjectManager));

    add(content);
  }

  public Optional<Contact> getIfValidManager() {
    if (managerBinding.isValid()) {
      try {
        return Optional.ofNullable(managerBinding.getValue());
      } catch (ValidationException e) {
        // swallow exception by design
      }
    }
    return Optional.empty();
  }

  public Optional<Contact> getIfValidInvestigator() {
    if (investigatorBinding.isValid()) {
      try {
        return Optional.ofNullable(investigatorBinding.getValue());
      } catch (ValidationException e) {
        // swallow exception by design
      }
    }
    return Optional.empty();
  }

  public Optional<Contact> getIfValidProjectResponsible() {
    if (investigatorBinding.isValid()) {
      try {
        return Optional.ofNullable(projectResponsibleBinding.getValue());
      } catch (ValidationException e) {
        // swallow exception by design
      }
    }
    return Optional.empty();
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


  private boolean anyChanges() {
    return investigatorBinding.hasChanged() || projectResponsibleBinding.hasChanged()
        || managerBinding.hasChanged();
  }

  @Override
  @NonNull
  public InputValidation validate() {
    if (Stream.of(managerBinding, investigatorBinding, projectResponsibleBinding)
        .anyMatch(field -> !field.isValid())) {
      return InputValidation.failed();
    }
    return InputValidation.passed();
  }

  @Override
  public boolean hasChanges() {
    return anyChanges();
  }
}
