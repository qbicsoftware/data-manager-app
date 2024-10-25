package life.qbic.datamanager.views.projects.edit;

import static java.util.Objects.isNull;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import java.io.Serial;
import java.util.List;
import life.qbic.datamanager.views.general.contact.AutocompleteContactField;
import life.qbic.datamanager.views.general.contact.Contact;
import life.qbic.datamanager.views.general.funding.FundingField;
import life.qbic.datamanager.views.general.utils.Constants;
import life.qbic.datamanager.views.projects.edit.EditProjectInformationDialog.ProjectInformation;
import life.qbic.projectmanagement.domain.model.project.ProjectObjective;
import life.qbic.projectmanagement.domain.model.project.ProjectTitle;

public class EditProjectInformationForm extends FormLayout {

  @Serial
  private static final long serialVersionUID = 972380320581239752L;
  private final Binder<ProjectInformation> binder;
  private final AutocompleteContactField principalInvestigatorField;
  private final AutocompleteContactField responsiblePersonField;
  private final AutocompleteContactField projectManagerField;

  private final FundingField fundingField;

  public EditProjectInformationForm() {
    super();

    addClassName("form-content");
    binder = new Binder<>();
    binder.setBean(new ProjectInformation());

    TextField titleField = new TextField("Title");
    titleField.addClassName("title");
    titleField.setId("project-title-field");
    titleField.setRequired(true);
    titleField.setMaxLength(Constants.PROJECT_TITLE_MAX_LENGTH);
    titleField.setValueChangeMode(ValueChangeMode.EAGER);
    addConsumedLengthHelper(titleField, titleField.getValue());
    titleField.addValueChangeListener(e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
    binder.forField(titleField)
        .withValidator(it -> !it.isBlank(), "Please provide a project title")
        .bind((ProjectInformation::getProjectTitle),
            ProjectInformation::setProjectTitle);

    TextArea projectObjective = new TextArea("Objective");
    projectObjective.setRequired(true);
    projectObjective.setValueChangeMode(ValueChangeMode.EAGER);
    projectObjective.setMaxLength(Constants.PROJECT_OBJECTIVE_MAX_LENGTH);
    addConsumedLengthHelper(projectObjective, projectObjective.getValue());
    projectObjective.addValueChangeListener(
        e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
    binder.forField(projectObjective)
        .withValidator(value -> !value.isBlank(), "Please provide an objective")
        .bind((ProjectInformation::getProjectObjective),
            ProjectInformation::setProjectObjective);

    fundingField = new FundingField("Funding Information");
    binder.forField(fundingField)
        .withValidator(
            value -> isNull(value)
                || !value.getReferenceId().isBlank()
                || value.getLabel().isBlank(), "Please provide the grant ID for the given grant")
        .withValidator(
            value -> isNull(value)
                || value.getReferenceId().isBlank()
                || !value.getLabel().isBlank(), "Please provide the grant for the given grant ID.")
        .bind(projectInformation -> projectInformation.getFundingEntry().orElse(null),
            ProjectInformation::setFundingEntry);

    Div projectContactsLayout = new Div();
    projectContactsLayout.setClassName("project-contacts");

    Span projectContactsTitle = new Span("Project Contacts");
    projectContactsTitle.addClassName("title");

    Span projectContactsDescription = new Span("Important contact people of the project");

    projectContactsLayout.add(projectContactsTitle);
    projectContactsLayout.add(projectContactsDescription);

    principalInvestigatorField = new AutocompleteContactField("Principal Investigator", "PI");
    principalInvestigatorField.setRequired(true);
    principalInvestigatorField.setId("principal-investigator");
    binder.forField(principalInvestigatorField)
        .withValidator(it -> principalInvestigatorField.isValid(), "")
        .bind((ProjectInformation::getPrincipalInvestigator),
            ProjectInformation::setPrincipalInvestigator);

    responsiblePersonField = new AutocompleteContactField("Project Responsible (optional)", "Responsible");
    responsiblePersonField.setRequired(false);
    responsiblePersonField.setId("responsible-person");
    responsiblePersonField.setHelperText("Should be contacted about project-related questions");
    binder.forField(responsiblePersonField)
        .withNullRepresentation(responsiblePersonField.getEmptyValue())
        .withValidator(it -> responsiblePersonField.isValid(), "")
        .bind(projectInformation -> projectInformation.getResponsiblePerson().orElse(null),
            ProjectInformation::setResponsiblePerson);

    projectManagerField = new AutocompleteContactField("Project Manager", "Manager");
    projectManagerField.setRequired(true);
    projectManagerField.setId("project-manager");
    binder.forField(projectManagerField)
        .withNullRepresentation(projectManagerField.getEmptyValue())
        .withValidator(it -> projectManagerField.isValid(), "")
        .bind((ProjectInformation::getProjectManager),
            ProjectInformation::setProjectManager);

    setColspan(titleField, 2);
    setColspan(projectObjective, 2);
    setColspan(fundingField, 2);
    setColspan(principalInvestigatorField, 2);
    setColspan(responsiblePersonField, 2);
    setColspan(projectManagerField, 2);
    add(
        titleField,
        projectObjective,
        fundingField,
        projectContactsLayout,
        principalInvestigatorField,
        responsiblePersonField,
        projectManagerField
    );

  }

  public void setProjectManagers(List<Contact> projectManagers) {
    projectManagerField.setItems(projectManagers);
  }

  public void setResponsiblePersons(List<Contact> responsiblePersons) {
    responsiblePersonField.setItems(responsiblePersons);
  }

  public void setPrincipalInvestigators(List<Contact> principalInvestigators) {
    principalInvestigatorField.setItems(principalInvestigators);
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

  public void validate() {
    binder.validate();
    principalInvestigatorField.validate();
    responsiblePersonField.validate();
    projectManagerField.validate();
  }

  /**
   * Resets the values and validity of all components that implement value storing and validity
   * interfaces
   */
  public void reset() {
    principalInvestigatorField.clear();
    projectManagerField.clear();
    fundingField.clear();
    binder.setBean(new ProjectInformation());
  }

  public Binder<ProjectInformation> getBinder() {
    return binder;
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

}
