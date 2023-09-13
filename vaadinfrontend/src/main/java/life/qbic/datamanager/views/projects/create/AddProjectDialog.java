package life.qbic.datamanager.views.projects.create;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.Text;
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
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.general.contact.Contact;
import life.qbic.datamanager.views.general.contact.ContactField;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.finances.offer.OfferLookupService;
import life.qbic.projectmanagement.domain.finances.offer.Offer;
import life.qbic.projectmanagement.domain.finances.offer.OfferId;
import life.qbic.projectmanagement.domain.finances.offer.OfferPreview;
import life.qbic.projectmanagement.domain.project.ProjectCode;
import life.qbic.projectmanagement.domain.project.ProjectObjective;
import life.qbic.projectmanagement.domain.project.ProjectTitle;

/**
 * <b>Project Information Dialog</b>
 *
 * <p>Dialog to create a project based on a project intent or to update a project's information</p>
 *
 * @since 1.0.0
 */
@SpringComponent
@UIScope
public class AddProjectDialog extends DialogWindow {

  @Serial
  private static final long serialVersionUID = 7327075228498213661L;
  private static final Logger log = logger(AddProjectDialog.class);

  private final Binder<ProjectDraft> binder;

  private final OfferLookupService offerLookupService;
  public final ComboBox<OfferPreview> offerSearchField;

  private final TextField codeField;
  private final TextField titleField;
  private final TextArea projectObjective;
  private final ContactField principalInvestigatorField;
  private final ContactField responsiblePersonField;
  private final ContactField projectManagerField;

  public AddProjectDialog(OfferLookupService offerLookupService) {
    super();
    this.offerLookupService = requireNonNull(offerLookupService,
        "offerLookupService must not be null");

    addClassName("create-project-dialog");
    setHeaderTitle("Add Project");
    setConfirmButtonLabel("Add");
    confirmButton.addClickListener(this::onConfirmClicked);
    setCancelButtonLabel("Cancel");
    cancelButton.addClickListener(this::onCancelClicked);

    binder = new Binder<>();
    offerSearchField = createOfferSearch(this.offerLookupService);

    codeField = new TextField("Code");
    codeField.addClassName("code");
    codeField.setId("project-code-field");
    codeField.setRequired(true);
    codeField.setHelperText("Q and 4 letters/numbers");
    this.addOpenedChangeListener(openedChangeEvent -> {
      if (openedChangeEvent.isOpened()) {
        codeField.setValue(ProjectCode.random().value());
      }
    });
    binder.forField(codeField)
        .withValidator(ProjectCode::isValid,
            "A project code starts with Q followed by 4 letters/numbers")
        .bind(ProjectDraft::getProjectCode, ProjectDraft::setProjectCode);

    Button generateCodeButton = new Button(new Icon(VaadinIcon.REFRESH));
    generateCodeButton.getElement().setAttribute("aria-label", "Generate Code");
    generateCodeButton.setId("generate-code-btn");
    generateCodeButton.addThemeVariants(ButtonVariant.LUMO_ICON);
    generateCodeButton.addClickListener(
        buttonClickEvent -> codeField.setValue(ProjectCode.random().value()));

    titleField = new TextField("Title");
    titleField.addClassName("title");
    titleField.setId("project-title-field");
    titleField.setRequired(true);
    restrictProjectTitleLength();
    binder.forField(titleField)
        .withValidator(it -> !it.isBlank(), "Please provide a project title")
        .bind(ProjectDraft::getProjectTitle, ProjectDraft::setProjectTitle);

    Span codeAndTitleLayout = new Span();
    codeAndTitleLayout.addClassName("code-and-title");
    codeAndTitleLayout.add(codeField, generateCodeButton, titleField);

    projectObjective = new TextArea("Objective");
    projectObjective.setRequired(true);
    restrictProjectObjectiveLength();
    binder.forField(projectObjective)
        .withValidator(value -> !value.isBlank(), "Please provide an objective")
        .bind(ProjectDraft::getProjectObjective, ProjectDraft::setProjectObjective);

    Div projectContactsLayout = new Div();
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
        .bind(ProjectDraft::getPrincipalInvestigator, ProjectDraft::setPrincipalInvestigator);

    responsiblePersonField = new ContactField("Project Responsible (optional)");
    responsiblePersonField.setRequired(false);
    responsiblePersonField.setId("responsible-person");
    responsiblePersonField.setHelperText("Should be contacted about project-related questions");
    binder.forField(responsiblePersonField)
        .bind(projectDraft -> projectDraft.getResponsiblePerson().orElse(null),
            ProjectDraft::setResponsiblePerson);

    projectManagerField = new ContactField("Project Manager");
    projectManagerField.setRequired(true);
    projectManagerField.setId("project-manager");
    binder.forField(projectManagerField)
        .bind(ProjectDraft::getProjectManager, ProjectDraft::setProjectManager);

    // Calls the reset method for all possible closure methods of the dialogue window:
    addDialogCloseActionListener(closeActionEvent -> close());
    cancelButton.addClickListener(buttonClickEvent -> close());

    FormLayout formLayout = new FormLayout();
    formLayout.addClassName("form-content");
    formLayout.add(
        offerSearchField,
        codeAndTitleLayout,
        projectObjective,
        projectContactsLayout,
        principalInvestigatorField,
        responsiblePersonField,
        projectManagerField
    );
    formLayout.setColspan(offerSearchField, 2);
    formLayout.setColspan(codeAndTitleLayout, 2);
    formLayout.setColspan(projectObjective, 2);
    formLayout.setColspan(principalInvestigatorField, 2);
    formLayout.setColspan(responsiblePersonField, 2);
    formLayout.setColspan(projectManagerField, 2);
    add(formLayout);
  }

  private void onConfirmClicked(ClickEvent<Button> clickEvent) {
    ProjectDraft projectDraft = new ProjectDraft();
    try {
      binder.writeBean(projectDraft);
      fireEvent(new ProjectAddEvent(projectDraft, this, clickEvent.isFromClient()));
    } catch (ValidationException e) {
      validate();
    }
  }

  private void validate() {
    binder.validate();
    principalInvestigatorField.validate();
    responsiblePersonField.validate();
    projectManagerField.validate();
  }

  private void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
    close();
  }


  private ComboBox<OfferPreview> createOfferSearch(OfferLookupService offerLookupService) {
    final ComboBox<OfferPreview> searchField = new ComboBox<>("Offer");
    searchField.setClassName("search-field");
    searchField.setPlaceholder("Search");
    searchField.setPrefixComponent(VaadinIcon.SEARCH.create());

    searchField.setItems(
        query -> offerLookupService.findOfferContainingProjectTitleOrId(
            query.getFilter().orElse(""), query.getFilter().orElse(""), query.getOffset(),
            query.getLimit()).stream());

    // Render the preview
    searchField.setRenderer(
        new ComponentRenderer<>(preview -> new Text(previewToString(preview))));

    // Generate labels like the rendering
    searchField.setItemLabelGenerator(
        (ItemLabelGenerator<OfferPreview>) it -> it.offerId().id());

    searchField.addValueChangeListener(e -> {
      if (searchField.getValue() != null) {
        setOffer(searchField.getValue().offerId().id());
      }
    });
    return searchField;
  }

  private void setOffer(String offerId) {
    OfferId id = OfferId.from(offerId);
    Optional<Offer> offer = offerLookupService.findOfferById(id);
    offer.ifPresentOrElse(this::fillProjectInformationFromOffer,
        () -> log.error("No offer found with id: " + offerId));
  }

  private void fillProjectInformationFromOffer(Offer offer) {
    titleField.setValue(offer.projectTitle().title());
    projectObjective.setValue(offer.projectObjective().objective().replace("\n", " "));
  }

  @Override
  public void close() {
    super.close();
    reset();
  }

  /**
   * Resets the values and validity of all components that implement value storing and validity
   * interfaces
   */
  public void reset() {
    offerSearchField.clear();
    principalInvestigatorField.clear();
    projectManagerField.clear();
    binder.removeBean();
    binder.setBean(new ProjectDraft());
  }

  /**
   * Render the preview like `#offer-id, #project title`
   *
   * @param offerPreview the offer preview
   * @return the formatted String representation
   * @since 1.0.0
   */
  private static String previewToString(OfferPreview offerPreview) {
    return offerPreview.offerId().id() + ", " + offerPreview.getProjectTitle().title();
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

  public void addProjectAddEventListener(ComponentEventListener<ProjectAddEvent> listener) {
    addListener(ProjectAddEvent.class, listener);
  }

  public void addCancelListener(ComponentEventListener<CancelEvent> listener) {
    addListener(CancelEvent.class, listener);
  }

  public static class CancelEvent extends
      life.qbic.datamanager.views.events.UserCancelEvent<AddProjectDialog> {

    public CancelEvent(AddProjectDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  /**
   * <b>Project Add Event</b>
   *
   * <p>Indicates that a user submitted a project addition request</p>
   *
   * @since 1.0.0
   */
  public static class ProjectAddEvent extends ComponentEvent<AddProjectDialog> {

    @Serial
    private static final long serialVersionUID = 1072173555312630829L;
    private ProjectDraft projectDraft;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param projectDraft the project draft for which the event is fired
     * @param source       the source component
     * @param fromClient   <code>true</code> if the event originated from the client
     *                     side, <code>false</code> otherwise
     */
    public ProjectAddEvent(ProjectDraft projectDraft, AddProjectDialog source, boolean fromClient) {
      super(source, fromClient);
      requireNonNull(projectDraft, "projectDraft must not be null");
      this.projectDraft = projectDraft;
    }

    public ProjectDraft projectDraft() {
      return projectDraft;
    }
  }

  public static final class ProjectDraft implements Serializable {

    @Serial
    private static final long serialVersionUID = 1997619416908358254L;
    private String offerId = "";
    @NotEmpty
    private String projectTitle = "";
    @NotEmpty
    private String projectCode = "";
    @NotEmpty
    private String projectObjective = "";
    @NotEmpty
    private Contact principalInvestigator;
    private Contact responsiblePerson;
    @NotEmpty
    private Contact projectManager;

    void setOfferId(String offerId) {
      this.offerId = offerId;
    }

    void setProjectTitle(String projectTitle) {
      this.projectTitle = projectTitle;
    }

    void setProjectCode(String projectCode) {
      this.projectCode = projectCode;
    }

    void setProjectObjective(String projectObjective) {
      this.projectObjective = projectObjective;
    }

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

    public String getOfferId() {
      return offerId;
    }

    public String getProjectTitle() {
      return projectTitle;
    }

    public String getProjectCode() {
      return projectCode;
    }

    public String getProjectObjective() {
      return projectObjective;
    }

    public String getPrincipalInvestigatorName() {
      return principalInvestigator.getFullName();
    }

    public String getPrincipalInvestigatorEmail() {
      return principalInvestigator.getEmail();
    }

    public String getResponsiblePersonName() {
      return getResponsiblePerson().map(Contact::getFullName).orElse(null);
    }

    public String getResponsiblePersonEmail() {
      return getResponsiblePerson().map(Contact::getEmail).orElse(null);
    }

    public String getProjectManagerName() {
      return projectManager.getFullName();
    }

    public String getProjectManagerEmail() {
      return projectManager.getEmail();
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
      if (!Objects.equals(projectTitle, that.projectTitle)) {
        return false;
      }
      if (!Objects.equals(projectCode, that.projectCode)) {
        return false;
      }
      if (!Objects.equals(projectObjective, that.projectObjective)) {
        return false;
      }
      if (!Objects.equals(principalInvestigator, that.principalInvestigator)) {
        return false;
      }
      if (!Objects.equals(responsiblePerson, that.responsiblePerson)) {
        return false;
      }
      return Objects.equals(projectManager, that.projectManager);
    }

    @Override
    public int hashCode() {
      int result = offerId != null ? offerId.hashCode() : 0;
      result = 31 * result + (projectTitle != null ? projectTitle.hashCode() : 0);
      result = 31 * result + (projectCode != null ? projectCode.hashCode() : 0);
      result = 31 * result + (projectObjective != null ? projectObjective.hashCode() : 0);
      result = 31 * result + (principalInvestigator != null ? principalInvestigator.hashCode() : 0);
      result = 31 * result + (responsiblePerson != null ? responsiblePerson.hashCode() : 0);
      result = 31 * result + (projectManager != null ? projectManager.hashCode() : 0);
      return result;
    }
  }

}
