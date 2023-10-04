package life.qbic.datamanager.views.projects.create;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.Optional;
import life.qbic.datamanager.views.general.DialogWindow;
import life.qbic.datamanager.views.projects.ProjectFormLayout;
import life.qbic.datamanager.views.projects.ProjectFormLayout.ProjectDraft;
import life.qbic.datamanager.views.projects.edit.EditProjectInformationDialog.ProjectInformation;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.finances.offer.OfferLookupService;
import life.qbic.projectmanagement.domain.finances.offer.Offer;
import life.qbic.projectmanagement.domain.finances.offer.OfferId;
import life.qbic.projectmanagement.domain.finances.offer.OfferPreview;
import life.qbic.projectmanagement.domain.project.ProjectCode;

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
  private final Binder<ProjectInformation> binder;
  private final Binder<String> projectCodeBinder;
  private final ProjectFormLayout formLayout;
  private final OfferLookupService offerLookupService;
  public final ComboBox<OfferPreview> offerSearchField;
  private final TextField codeField;

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

    String code = "";
    projectCodeBinder = new Binder<>();
    projectCodeBinder.forField(codeField).withValidator(ProjectCode::isValid,
        "A project code starts with Q followed by 4 letters/numbers").
        bind(s -> code, (s, v) -> s = v);

    formLayout = new ProjectFormLayout().buildAddProjectLayout(offerSearchField, codeField);
    binder = formLayout.getBinder();
    // Calls the reset method for all possible closure methods of the dialogue window:
    addDialogCloseActionListener(closeActionEvent -> close());
    cancelButton.addClickListener(buttonClickEvent -> close());

    add(formLayout);
  }

  @Override
  protected void onConfirmClicked(ClickEvent<Button> clickEvent) {
    ProjectDraft projectDraft = new ProjectDraft();
    ProjectInformation projectInformation = new ProjectInformation();
    try {
      binder.writeBean(projectInformation);
      projectDraft.setProjectInformation(projectInformation);
      projectDraft.setProjectCode(codeField.getValue());
      fireEvent(new ProjectAddEvent(projectDraft, this, clickEvent.isFromClient()));
    } catch (ValidationException e) {
      validate();
    }
  }

  private void validate() {
    formLayout.validate();
    projectCodeBinder.validate();
  }

  @Override
  protected void onCancelClicked(ClickEvent<Button> clickEvent) {
    fireEvent(new CancelEvent(this, clickEvent.isFromClient()));
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
    formLayout.fillProjectInformationFromOffer(offer);
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
    formLayout.reset();
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

}
