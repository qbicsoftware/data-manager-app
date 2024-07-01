package life.qbic.datamanager.views.projects.create;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import life.qbic.datamanager.views.general.HasBinderValidation;
import life.qbic.datamanager.views.general.Stepper;
import life.qbic.datamanager.views.general.Stepper.StepIndicator;
import life.qbic.datamanager.views.general.contact.Contact;
import life.qbic.datamanager.views.general.funding.FundingEntry;
import life.qbic.datamanager.views.projects.create.CollaboratorsLayout.ProjectCollaborators;
import life.qbic.datamanager.views.projects.create.ExperimentalInformationLayout.ExperimentalInformation;
import life.qbic.datamanager.views.projects.create.ProjectDesignLayout.ProjectDesign;
import life.qbic.datamanager.views.projects.project.CreateProjectCancelConfirmationNotification;
import life.qbic.finances.api.FinanceService;
import life.qbic.projectmanagement.application.ContactRepository;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.ontology.OntologyLookupService;
import life.qbic.projectmanagement.domain.model.project.Project;

/**
 * Project Creation Dialog
 *
 * <p>Vaadin dialog component which enables the user to trigger the {@link Project}
 * creation process</p>
 */

@SpringComponent
@UIScope
public class AddProjectDialog extends Dialog {

  @Serial
  private static final long serialVersionUID = 7643754818237178416L;
  private final Div dialogContent;
  private final Stepper stepper;
  private final ProjectDesignLayout projectDesignLayout;
  private final FundingInformationLayout fundingInformationLayout;
  private final CollaboratorsLayout collaboratorsLayout;
  private final ExperimentalInformationLayout experimentalInformationLayout;

  private final Button confirmButton;
  private final Button backButton;
  private final Button nextButton;

  private final Map<String, Component> stepContent;


  private StepIndicator addStep(Stepper stepper, String label, Component layout) {
    stepContent.put(label, layout);
    return stepper.addStep(label);
  }

  public AddProjectDialog(ProjectInformationService projectInformationService,
      FinanceService financeService,
      OntologyLookupService ontologyLookupService,
      ContactRepository contactRepository) {
    super();

    initCancelShortcuts();

    addClassName("add-project-dialog");
    requireNonNull(projectInformationService, "project information service must not be null");
    requireNonNull(financeService, "financeService must not be null");
    requireNonNull(ontologyLookupService,
        "ontologyTermInformationService must not be null");
    this.projectDesignLayout = new ProjectDesignLayout(projectInformationService, financeService);
    this.fundingInformationLayout = new FundingInformationLayout();
    this.collaboratorsLayout = new CollaboratorsLayout();
    this.experimentalInformationLayout = new ExperimentalInformationLayout(
        ontologyLookupService);

    List<Contact> knownContacts = contactRepository.findAll().stream().map(contact ->
        new Contact(contact.fullName(), contact.emailAddress())).toList();
    if(knownContacts.isEmpty()) {
      collaboratorsLayout.hideContactBox();
    } else {
      collaboratorsLayout.setKnownContacts(knownContacts);
    }

    stepContent = new HashMap<>();

    setHeaderTitle("Create Project");
    dialogContent = new Div();
    dialogContent.addClassName("layout-container");

    stepper = new Stepper(this::createArrowSpan);
    add(generateSectionDivider(),
        stepper,
        generateSectionDivider(),
        dialogContent,
        generateSectionDivider());

    StepIndicator projectDesign = addStep(stepper, "Project Design", projectDesignLayout);
    addStep(stepper, "Funding Information", fundingInformationLayout);
    addStep(stepper, "Project Collaborators", collaboratorsLayout);
    addStep(stepper, "Experimental Information", experimentalInformationLayout);
    stepper.setSelectedStep(projectDesign);

    nextButton = new Button("Next");
    nextButton.addClassNames("primary", "next");
    nextButton.addClickListener(this::onNextClicked);

    confirmButton = new Button("Confirm");
    confirmButton.addClassNames("primary", "confirm");
    confirmButton.addClickListener(this::onConfirmClicked);


    setDialogContent(stepper.getFirstStep());

    stepper.addStepSelectionListener(
        stepSelectedEvent -> {
          setDialogContent(stepSelectedEvent.getSelectedStep());
          adaptFooterButtons(stepSelectedEvent.getSelectedStep());
        });

    Button cancelButton = new Button("Cancel");
    cancelButton.addClassName("cancel");
    cancelButton.addClickListener(this::onCancelClicked);
    backButton = new Button("Back");
    backButton.addClassName("back");
    backButton.addClickListener(this::onBackClicked);

    DialogFooter footer = getFooter();
    Div rightButtons = new Div();
    rightButtons.addClassName("footer-right-buttons-container");
    rightButtons.add(cancelButton, nextButton, confirmButton);
    footer.add(backButton, rightButtons);
    adaptFooterButtons(stepper.getFirstStep());
  }

  private void initCancelShortcuts() {
    setCloseOnOutsideClick(false);
    setCloseOnEsc(false);
    Shortcuts.addShortcutListener(this,
        this::onCreationCanceled, Key.ESCAPE);
  }

  private void onCreationCanceled() {
    CreateProjectCancelConfirmationNotification projectCancelNotification = new CreateProjectCancelConfirmationNotification();
    projectCancelNotification.open();
    projectCancelNotification.addConfirmListener(event -> {
      projectCancelNotification.close();
      fireEvent(new CancelEvent(this, true));
    });
    projectCancelNotification.addCancelListener(
        event -> projectCancelNotification.close());
  }

  /**
   * Allows user to search the offer database to prefill some project information
   */
  public void enableOfferSearch() {
    projectDesignLayout.enableOfferSearch();
  }

  private void onCancelClicked(ClickEvent<Button> clickEvent) {
    onCreationCanceled();
  }

  private void onConfirmClicked(ClickEvent<Button> event) {
    if (projectDesignLayout.validate().isInvalid()) {
      return;
    }
    if (fundingInformationLayout.validate().isInvalid()) {
      return;
    }
    if (collaboratorsLayout.validate().isInvalid()) {
      return;
    }
    if (experimentalInformationLayout.validate().isInvalid()) {
      return;
    }
    fireEvent(new ConfirmEvent(this, projectDesignLayout.getProjectDesign(),
        fundingInformationLayout.getFundingInformation(),
        collaboratorsLayout.getProjectCollaborators(),
        experimentalInformationLayout.getExperimentalInformation(), true));

  }

  private void onNextClicked(ClickEvent<Button> event) {
    if (isDialogContentValid()) {
      stepper.selectNextStep();
    }
  }

  private void onBackClicked(ClickEvent<Button> event) {
    stepper.selectPreviousStep();
  }

  private Span createArrowSpan() {
    Icon arrowIcon = VaadinIcon.ARROW_RIGHT.create();
    Span arrow = new Span(arrowIcon);
    arrow.addClassName("project-creation-stepper-arrow");
    return arrow;
  }

  /**
   * Add a listener that is called, when a new {@link ConfirmEvent event} is emitted.
   *
   * @param listener a listener that should be called
   * @since 1.0.0
   */
  public void addConfirmListener(ComponentEventListener<ConfirmEvent> listener) {
    requireNonNull(listener);
    addListener(ConfirmEvent.class, listener);
  }

  public Registration addCancelListener(ComponentEventListener<CancelEvent> listener) {
    requireNonNull(listener, "listener must not be null");
    return addListener(CancelEvent.class, listener);
  }

  private boolean isDialogContentValid() {
    return !isDialogContentInvalid();
  }

  private boolean isDialogContentInvalid() {
    return dialogContent.getChildren()
        .filter(HasBinderValidation.class::isInstance)
        .map(HasBinderValidation.class::cast)
        .map(HasBinderValidation::validate)
        .anyMatch(HasValidation::isInvalid);
  }


  private void adaptFooterButtons(StepIndicator step) {

    backButton.setVisible(true);
    nextButton.setVisible(true);
    confirmButton.setVisible(false);

    if (stepper.isFirstStep(step)) {
      backButton.setVisible(false);
    }

    if (stepper.isLastStep(step)) {
      nextButton.setVisible(false);
      confirmButton.setVisible(true);
    }

  }

  private static Span generateSectionDivider() {
    Span sectionDivider = new Span(new Hr());
    sectionDivider.addClassName("section-divider");
    return sectionDivider;
  }

  private void setDialogContent(StepIndicator step) {
    dialogContent.removeAll();
    var selectedComponent = stepContent.getOrDefault(step.getLabel(), new Div());
    dialogContent.add(selectedComponent);
  }

  /**
   * <b>Project Creation Event</b>
   *
   * <p>Indicates that a user submitted a project creation request</p>
   *
   * @since 1.0.0
   */
  public static class ConfirmEvent extends ComponentEvent<AddProjectDialog> {

    @Serial
    private static final long serialVersionUID = 3629446840913968906L;
    private final ProjectDesign projectDesign;
    private final FundingEntry fundingEntry;
    private final ProjectCollaborators projectCollaborators;
    private final ExperimentalInformation experimentalInformation;

    public ProjectDesign getProjectDesign() {
      return projectDesign;
    }

    public FundingEntry getFundingEntry() {
      return fundingEntry;
    }

    public ProjectCollaborators getProjectCollaborators() {
      return projectCollaborators;
    }

    public ExperimentalInformation getExperimentalInformation() {
      return experimentalInformation;
    }


    public ConfirmEvent(AddProjectDialog source,
        ProjectDesign projectDesign, FundingEntry fundingEntry,
        ProjectCollaborators projectCollaborators,
        ExperimentalInformation experimentalInformation, boolean fromClient) {
      super(source, fromClient);
      this.projectDesign = projectDesign;
      this.fundingEntry = fundingEntry;
      this.projectCollaborators = projectCollaborators;
      this.experimentalInformation = experimentalInformation;
    }
  }

  public static class CancelEvent extends ComponentEvent<AddProjectDialog> {

    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public CancelEvent(AddProjectDialog source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
