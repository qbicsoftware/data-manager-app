package life.qbic.datamanager.views.projects.edit;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.data.binder.ValidationException;
import java.util.Objects;
import life.qbic.datamanager.views.events.FundingInformationUpdateEvent;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.projects.edit.EditProjectInformationDialog.ProjectInformation;
import life.qbic.datamanager.views.strategy.DialogClosingStrategy;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class EditContactDialog extends DialogWindow {

  private final ProjectInformation projectInformation;
  private DialogClosingStrategy noChangesClosingStrategy;
  private DialogClosingStrategy warningClosingStrategy;

  private BoundContactField managerBinding;
  private BoundContactField investigatorBinding;
  private BoundContactField projectResponsibleBinding;

  public EditContactDialog(ProjectInformation projectInformation) {
    super();
    this.projectInformation = Objects.requireNonNull(projectInformation);

    addClassName("large-dialog");
    var content = new Div();
    content.addClassNames("vertical-list");
    setConfirmButtonLabel("Save");
    setCancelButtonLabel("Cancel");
    setHeaderTitle("Project Contacts");
    var fieldPrincipalInvestigator = createRequired("Principal Investigator");
    var fieldProjectResponsible = createOptional(
        "Project Responsible / Co-Investigator (optional)");
    var fieldProjectManager = createRequired("Project Manager");

    this.investigatorBinding = BoundContactField.createMandatory(fieldPrincipalInvestigator);
    this.managerBinding = BoundContactField.createMandatory(fieldProjectManager);
    this.projectResponsibleBinding = BoundContactField.createOptional(fieldProjectResponsible);

    content.add(
        new ContactsForm(fieldPrincipalInvestigator, fieldProjectResponsible, fieldProjectManager)
    );

    add(content);
  }

  private static ContactField createRequired(String label) {
    var contact = new ContactField(label);
    contact.setRequiredIndicatorVisible(true);
    return contact;
  }

  private static ContactField createOptional(String label) {
    return new ContactField(label);
  }

  public void setDefaultCancelStrategy(DialogClosingStrategy strategy) {
    this.noChangesClosingStrategy = strategy;
  }

  public void setCancelWithoutSaveStrategy(DialogClosingStrategy strategy) {
    this.warningClosingStrategy = strategy;
  }


  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    try {
      investigatorBinding.isValid();
      projectResponsibleBinding.isValid();
      managerBinding.getValue();
    } catch (ValidationException e) {
      // user needs to intervene
    }
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {

  }

  public void addUpdateEventListener(
      ComponentEventListener<FundingInformationUpdateEvent> listener) {
    throw new RuntimeException("Needs to be implemented");
  }
}
