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
import com.vaadin.flow.data.binder.Binder.Binding;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import java.io.Serial;
import java.util.Optional;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.finances.offer.OfferLookupService;
import life.qbic.projectmanagement.domain.finances.offer.Offer;
import life.qbic.projectmanagement.domain.finances.offer.OfferId;
import life.qbic.projectmanagement.domain.finances.offer.OfferPreview;
import life.qbic.projectmanagement.domain.project.ProjectCode;
import life.qbic.projectmanagement.domain.project.ProjectObjective;
import life.qbic.projectmanagement.domain.project.ProjectTitle;

/**
 * <b>Project Creation Dialog</b>
 *
 * <p>Dialog to create a project based on a project intent</p>
 *
 * @since 1.0.0
 */
@SpringComponent
@UIScope
public class AddProjectDialog extends DialogWindow {

  @Serial
  private static final long serialVersionUID = -4891285981863969247L;
  private static final Logger log = logger(AddProjectDialog.class);

  private final Binder<ProjectDraft> binder;

  private final OfferLookupService offerLookupService;
  public final ComboBox<OfferPreview> offerSearchField;

  private final TextField codeField;
  private final TextField titleField;
  private final TextArea projectObjective;

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

    TextField principalInvestigatorName = new TextField("Principal Investigator Name",
        "Please enter a name");
    principalInvestigatorName.setRequired(true);
    binder.forField(principalInvestigatorName)
        .withValidator(it -> !it.isBlank(), "Please provide the principal investigator's name")
        .bind(ProjectDraft::getPrincipalInvestigatorName,
            ProjectDraft::setPrincipalInvestigatorName);

    TextField principalInvestigatorEmail = new TextField("Principal Investigator Email",
        "name@domain.de");
    principalInvestigatorEmail.setRequired(true);
    binder.forField(principalInvestigatorEmail)
        .withValidator(it -> !it.isBlank(), "Please provide the principal investigator's email")
        .withValidator(new EmailValidator("Please provide an email address name@domain.de", false))
        .bind(ProjectDraft::getPrincipalInvestigatorEmail,
            ProjectDraft::setPrincipalInvestigatorEmail);

    TextField projectManagerName = new TextField("Project Manager Name", "Please enter a name");
    projectManagerName.setRequired(true);
    binder.forField(projectManagerName)
        .withValidator(it -> !it.isBlank(), "Please provide the project manager's name")
        .bind(ProjectDraft::getProjectManagerName,
            ProjectDraft::setProjectManagerName);
    TextField projectManagerEmail = new TextField("Project Manager Email", "name@domain.de");
    projectManagerEmail.setRequired(true);
    binder.forField(projectManagerEmail)
        .withValidator(it -> !it.isBlank(), "Please provide the project manager's email")
        .withValidator(new EmailValidator("Please provide an email address name@domain.de", false))
        .bind(ProjectDraft::getProjectManagerEmail, ProjectDraft::setProjectManagerEmail);

    TextField responsiblePersonName = new TextField("Responsible Person Name",
        "Please enter a name");
    TextField responsiblePersonEmail = new TextField("Responsible Person Email",
        "name@domain.de");
    responsiblePersonName.setRequired(false);
    responsiblePersonEmail.setRequired(false);

    SerializablePredicate<String> isFilledOrNoResponsiblePerson = it -> !it.isBlank()
        || (responsiblePersonName.isEmpty() && responsiblePersonEmail.isEmpty());

    Binding<ProjectDraft, String> responsiblePersonNameFieldBinding = binder.forField(
            responsiblePersonName)
        .withValidator(isFilledOrNoResponsiblePerson,
            "Please provide the responsible person's name")
        .bind(ProjectDraft::getResponsiblePersonName,
            ProjectDraft::setResponsiblePersonName);
    Binding<ProjectDraft, String> responsiblePersonEmailFieldBinding = binder.forField(
            responsiblePersonEmail)
        .withValidator(isFilledOrNoResponsiblePerson,
            "Please provide the responsible person's email")
        .withValidator(new EmailValidator("Please provide an email address name@domain.de", true))
        .bind(ProjectDraft::getResponsiblePersonEmail, ProjectDraft::setResponsiblePersonEmail);
    responsiblePersonName.addValueChangeListener(
        it -> responsiblePersonEmailFieldBinding.validate());
    responsiblePersonEmail.addValueChangeListener(
        it -> responsiblePersonNameFieldBinding.validate());

    // Calls the reset method for all possible closure methods of the dialogue window:
    addDialogCloseActionListener(closeActionEvent -> resetAndClose());
    cancelButton.addClickListener(buttonClickEvent -> resetAndClose());

    // code generateCodeButton title
    // -------objective-------------
    // h3 > contacts
    // blabla contacts
    // PIname PIemail
    // PRname PRemail
    // PMname PMemail

    FormLayout formLayout = new FormLayout();
    formLayout.addClassName("form-content");
    formLayout.add(
        offerSearchField,
        codeAndTitleLayout,
        projectObjective,
        principalInvestigatorName, principalInvestigatorEmail,
        responsiblePersonName, responsiblePersonEmail,
        projectManagerName, projectManagerEmail
    );
    formLayout.setColspan(offerSearchField, 2);
    formLayout.setColspan(codeAndTitleLayout, 2);
    formLayout.setColspan(projectObjective, 2);
    add(formLayout);
  }

  private void onConfirmClicked(ClickEvent<Button> clickEvent) {
    if (isInputValid()) {
      fireEvent(new ProjectAddEvent(binder.getBean(), this, clickEvent.isFromClient()));
    }
  }

  private void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
    resetAndClose();
  }


  private ComboBox<OfferPreview> createOfferSearch(OfferLookupService offerLookupService) {
    final ComboBox<OfferPreview> offerSearchField = new ComboBox<>("Offer");
    offerSearchField.setClassName("search-field");
    offerSearchField.setPlaceholder("Search");
    offerSearchField.setPrefixComponent(VaadinIcon.SEARCH.create());

    offerSearchField.setItems(
        query -> offerLookupService.findOfferContainingProjectTitleOrId(
            query.getFilter().orElse(""), query.getFilter().orElse(""), query.getOffset(),
            query.getLimit()).stream());

    // Render the preview
    offerSearchField.setRenderer(
        new ComponentRenderer<>(preview -> new Text(previewToString(preview))));

    // Generate labels like the rendering
    offerSearchField.setItemLabelGenerator(
        (ItemLabelGenerator<OfferPreview>) it -> it.offerId().id());

    offerSearchField.addValueChangeListener(e -> {
      if (offerSearchField.getValue() != null) {
        setOffer(offerSearchField.getValue().offerId().id());
      }
    });
    return offerSearchField;
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

  public void resetAndClose() {
    close();
    reset();
  }

  /**
   * Resets the values and validity of all components that implement value storing and validity
   * interfaces
   */
  public void reset() {
    binder.removeBean();
    offerSearchField.clear();
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

  public boolean isInputValid() {
    return binder.validate().isOk();
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
    private final ProjectDraft projectDraft;

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param projectDraft
     * @param source       the source component
     * @param fromClient   <code>true</code> if the event originated from the client
     *                     side, <code>false</code> otherwise
     */
    public ProjectAddEvent(ProjectDraft projectDraft, AddProjectDialog source, boolean fromClient) {
      super(source, fromClient);
      this.projectDraft = projectDraft;
    }

    public ProjectDraft projectDraft() {
      return projectDraft;
    }
  }

  public static final class ProjectDraft {

    private String offerId = "";
    @NotEmpty
    private String projectTitle = "";
    @NotEmpty
    private String projectCode = "";
    @NotEmpty
    private String projectObjective = "";
    @NotEmpty
    private String principalInvestigatorName = "";
    @NotEmpty
    private String principalInvestigatorEmail = "";
    private String responsiblePersonName = "";
    @Email
    private String responsiblePersonEmail = "";
    @NotEmpty
    private String projectManagerName = "";
    @NotEmpty
    private String projectManagerEmail = "";

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

    void setResponsiblePersonName(String responsibleName) {
      this.responsiblePersonName = responsibleName;
    }

    void setResponsiblePersonEmail(String responsibleEmail) {
      this.responsiblePersonEmail = responsibleEmail;
    }

    void setProjectManagerName(String projectManagerName) {
      this.projectManagerName = projectManagerName;
    }

    void setProjectManagerEmail(String projectManagerEmail) {
      this.projectManagerEmail = projectManagerEmail;
    }

    public String getOfferId() {
      return offerId;
    }

    public String getProjectTitle() {
      return projectTitle;
    }

    public String getProjectCode() {
      return projectManagerEmail;
    }

    public String getProjectObjective() {
      return projectManagerEmail;
    }

    public String getPrincipalInvestigatorName() {
      return principalInvestigatorName;
    }

    public void setPrincipalInvestigatorName(String principalInvestigatorName) {
      this.principalInvestigatorName = principalInvestigatorName;
    }

    public String getPrincipalInvestigatorEmail() {
      return principalInvestigatorEmail;
    }

    public void setPrincipalInvestigatorEmail(String principalInvestigatorEmail) {
      this.principalInvestigatorEmail = principalInvestigatorEmail;
    }

    public String getResponsiblePersonName() {
      return responsiblePersonName;
    }

    public String getResponsiblePersonEmail() {
      return responsiblePersonEmail;
    }

    public String getProjectManagerName() {
      return projectManagerName;
    }

    public String getProjectManagerEmail() {
      return projectManagerEmail;
    }
  }

}
