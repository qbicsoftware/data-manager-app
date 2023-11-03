package life.qbic.datamanager.views.projects;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import life.qbic.controlling.domain.finances.offer.Offer;
import life.qbic.controlling.domain.finances.offer.OfferPreview;
import life.qbic.datamanager.views.general.contact.ContactField;
import life.qbic.datamanager.views.general.funding.FundingField;
import life.qbic.datamanager.views.projects.edit.EditProjectInformationDialog.ProjectInformation;
import life.qbic.projectmanagement.domain.model.project.ProjectCode;
import life.qbic.projectmanagement.domain.model.project.ProjectObjective;
import life.qbic.projectmanagement.domain.model.project.ProjectTitle;

/**
 * <b>Project Form Layout</b>
 *
 * <p>Used to style and list the common elements of Project Edit and Project Creation
 * functionality</p>
 *
 * @since 1.0.0
 */
@SpringComponent
@UIScope
public class ProjectFormLayout extends FormLayout {

  @Serial
  private static final long serialVersionUID = 972380320581239752L;
  private final Div projectContactsLayout = new Div();

  private final Binder<ProjectInformation> binder;
  private final TextField titleField;
  private final TextArea projectObjective;
  private final ContactField principalInvestigatorField;
  private final ContactField responsiblePersonField;
  private final ContactField projectManagerField;

  private final FundingField fundingField;

  public ProjectFormLayout() {
    super();

    addClassName("form-content");
    binder = new Binder<>();
    binder.setBean(new ProjectInformation());

    titleField = new TextField("Title");
    titleField.addClassName("title");
    titleField.setId("project-title-field");
    titleField.setRequired(true);
    restrictProjectTitleLength();
    binder.forField(titleField)
        .withValidator(it -> !it.isBlank(), "Please provide a project title")
        .bind((ProjectInformation::getProjectTitle),
            ProjectInformation::setProjectTitle);

    projectObjective = new TextArea("Objective");
    projectObjective.setRequired(true);
    restrictProjectObjectiveLength();
    binder.forField(projectObjective)
        .withValidator(value -> !value.isBlank(), "Please provide an objective")
        .bind((ProjectInformation::getProjectObjective),
            ProjectInformation::setProjectObjective);

    fundingField = new FundingField("Funding Information");
    binder.forField(fundingField).withValidator(value -> {
          if (value == null) {
            return true;
          }
          return !value.getReferenceId().isBlank() || value.getLabel().isBlank();
        }, "Please provide the grant ID for the given grant")
        .withValidator(value -> {
              if (value == null) {
                return true;
              }
              return value.getReferenceId().isBlank() || !value.getLabel().isBlank();
            },
            "Please provide the grant for the given grant ID.")
        .bind(
            projectInformation -> projectInformation.getFundingEntry().orElse(null),
            ProjectInformation::setFundingEntry);

    projectContactsLayout.setClassName("project-contacts");

    Span projectContactsTitle = new Span("Project Contacts");
    projectContactsTitle.addClassName("title");

    Span projectContactsDescription = new Span("Important contact people of the project");

    projectContactsLayout.add(projectContactsTitle);
    projectContactsLayout.add(projectContactsDescription);

    principalInvestigatorField = new ContactField("Principal Investigator");
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

    projectManagerField = new ContactField("Project Manager");
    projectManagerField.setRequired(true);
    projectManagerField.setId("project-manager");
    binder.forField(projectManagerField)
        .bind((ProjectInformation::getProjectManager),
            ProjectInformation::setProjectManager);
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

  public ProjectFormLayout buildEditProjectLayout() {
    add(
        titleField,
        projectObjective,
        fundingField,
        projectContactsLayout,
        principalInvestigatorField,
        responsiblePersonField,
        projectManagerField
    );
    setColspan(titleField, 2);
    setColspan(projectObjective, 2);
    setColspan(fundingField, 2);
    setColspan(principalInvestigatorField, 2);
    setColspan(responsiblePersonField, 2);
    setColspan(projectManagerField, 2);

    return this;
  }

  public ProjectFormLayout buildAddProjectLayout(ComboBox<OfferPreview> offerSearchField,
      TextField codeField) {

    Button generateCodeButton = new Button(new Icon(VaadinIcon.REFRESH));
    generateCodeButton.getElement().setAttribute("aria-label", "Generate Code");
    generateCodeButton.setId("generate-code-btn");
    generateCodeButton.addThemeVariants(ButtonVariant.LUMO_ICON);
    generateCodeButton.addClickListener(
        buttonClickEvent -> codeField.setValue(ProjectCode.random().value()));

    Span codeAndTitleLayout = new Span();
    codeAndTitleLayout.addClassName("code-and-title");
    codeAndTitleLayout.add(codeField, generateCodeButton, titleField);

    add(
        offerSearchField,
        codeAndTitleLayout,
        projectObjective,
        fundingField,
        projectContactsLayout,
        principalInvestigatorField,
        responsiblePersonField,
        projectManagerField
    );
    setColspan(offerSearchField, 2);
    setColspan(codeAndTitleLayout, 2);
    setColspan(projectObjective, 2);
    setColspan(fundingField, 2);
    setColspan(principalInvestigatorField, 2);
    setColspan(responsiblePersonField, 2);
    setColspan(projectManagerField, 2);

    return this;
  }

  public void fillProjectInformationFromOffer(Offer offer) {
    titleField.setValue(offer.projectTitle().title());
    projectObjective.setValue(offer.projectObjective().objective().replace("\n", " "));
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

  private void restrictProjectObjectiveLength() {
    projectObjective.setValueChangeMode(ValueChangeMode.EAGER);
    projectObjective.setMaxLength((int) ProjectObjective.maxLength());
    addConsumedLengthHelper(projectObjective, projectObjective.getValue());
    projectObjective.addValueChangeListener(
        e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
  }

  private void restrictProjectTitleLength() {
    titleField.setMaxLength((int) ProjectTitle.maxLength());
    titleField.setValueChangeMode(ValueChangeMode.EAGER);
    addConsumedLengthHelper(titleField, titleField.getValue());
    titleField.addValueChangeListener(e -> addConsumedLengthHelper(e.getSource(), e.getValue()));
  }

  public Binder<ProjectInformation> getBinder() {
    return binder;
  }

  public static final class ProjectDraft implements Serializable {

    @Serial
    private static final long serialVersionUID = 1997619416908358254L;
    private final String offerId = "";
    private ProjectInformation projectInformation = new ProjectInformation();
    @NotEmpty
    private String projectCode = "";

    public String getOfferId() {
      return offerId;
    }

    public String getProjectCode() {
      return projectCode;
    }

    public void setProjectCode(String projectCode) {
      this.projectCode = projectCode;
    }

    public ProjectInformation getProjectInformation() {
      return projectInformation;
    }

    public void setProjectInformation(ProjectInformation projectInformation) {
      this.projectInformation = projectInformation;
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }
      if (object == null || getClass() != object.getClass()) {
        return false;
      }

      ProjectDraft that = (ProjectDraft) object;

      if (!Objects.equals(offerId, that.offerId)) {
        return false;
      }
      if (!Objects.equals(projectInformation, that.projectInformation)) {
        return false;
      }
      return Objects.equals(projectCode, that.projectCode);
    }

    @Override
    public int hashCode() {
      int result = offerId != null ? offerId.hashCode() : 0;
      result = 31 * result + (projectInformation != null ? projectInformation.hashCode() : 0);
      result = 31 * result + (projectCode != null ? projectCode.hashCode() : 0);
      return result;
    }
  }
}
