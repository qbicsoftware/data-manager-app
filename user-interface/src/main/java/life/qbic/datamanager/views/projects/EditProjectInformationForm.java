package life.qbic.datamanager.views.projects;

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
import life.qbic.datamanager.views.general.contact.ContactField;
import life.qbic.datamanager.views.general.funding.FundingField;
import life.qbic.datamanager.views.projects.edit.EditProjectInformationDialog.ProjectInformation;
import life.qbic.projectmanagement.domain.model.project.ProjectObjective;
import life.qbic.projectmanagement.domain.model.project.ProjectTitle;

public class EditProjectInformationForm extends FormLayout {

  @Serial
  private static final long serialVersionUID = 972380320581239752L;
  private final Binder<ProjectInformation> binder;
  private final AutocompleteContactField principalInvestigatorField;
  private final ContactField responsiblePersonField;
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
    titleField.setMaxLength((int) ProjectTitle.maxLength());
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
    projectObjective.setMaxLength((int) ProjectObjective.maxLength());
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

    principalInvestigatorField = new AutocompleteContactField("Principal Investigator");
    principalInvestigatorField.setRequired(true);
    principalInvestigatorField.setId("principal-investigator");
    binder.forField(principalInvestigatorField)
        .bind((ProjectInformation::getPrincipalInvestigator),
            ProjectInformation::setPrincipalInvestigator);

    responsiblePersonField = new ContactField("Project Responsible (optional)");
    responsiblePersonField.setRequired(false);
    responsiblePersonField.setId("responsible-person");
    responsiblePersonField.setHelperText("Should be contacted about project-related questions");
    binder.forField(responsiblePersonField)
        .bind(projectInformation -> projectInformation.getResponsiblePerson().orElse(null),
            (projectInformation, contact) -> {
              if (contact.getFullName().isEmpty() || contact.getEmail().isEmpty()) {
                projectInformation.setResponsiblePerson(null);
              } else {
                projectInformation.setResponsiblePerson(contact);
              }
            });

    projectManagerField = new AutocompleteContactField("Project Manager");
    projectManagerField.setRequired(true);
    projectManagerField.setId("project-manager");
    binder.forField(projectManagerField)
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
    setProjectManagers(dummyContacts());
    setPrincipalInvestigators(dummyContacts());
  }

  private static List<Contact> dummyContacts() {
    return List.of(
        new Contact("Max Mustermann", "max.mustermann@qbic.uni-tuebingen.de"),
        new Contact("David Müller", "david.mueller@qbic.uni-tuebingen.de"),
        new Contact("John Koch", "john.koch@qbic.uni-tuebingen.de"),
        new Contact("Trevor Noah", "trevor.noah@qbic.uni-tuebingen.de"),
        new Contact("Sarah Connor", "sarah.connor@qbic.uni-tuebingen.de"),
        new Contact("Anna Bell", "anna.bell@qbic.uni-tuebingen.de"),
        new Contact("Sophia Turner", "sophia.turner@qbic.uni-tuebingen.de"),
        new Contact("Tylor Smith", "tylor.smith@qbic.uni-tuebingen.de")
    );
  }

  public void setProjectManagers(List<Contact> projectManagers) {
    projectManagerField.setItems(projectManagers);
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

}
