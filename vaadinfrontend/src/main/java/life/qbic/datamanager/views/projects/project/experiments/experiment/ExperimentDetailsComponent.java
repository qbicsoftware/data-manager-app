package life.qbic.datamanager.views.projects.project.experiments.experiment;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.Overflow;
import com.vaadin.flow.theme.lumo.LumoUtility.TextOverflow;
import com.vaadin.flow.theme.lumo.LumoUtility.Whitespace;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.general.CreationCard;
import life.qbic.datamanager.views.general.CreationClickedEvent;
import life.qbic.datamanager.views.general.DisclaimerCard;
import life.qbic.datamanager.views.general.DisclaimerConfirmedEvent;
import life.qbic.datamanager.views.general.ToggleDisplayEditComponent;
import life.qbic.datamanager.views.layouts.CardComponent;
import life.qbic.datamanager.views.layouts.PageComponent;
import life.qbic.datamanager.views.notifications.InformationMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationPage;
import life.qbic.datamanager.views.projects.project.experiments.experiment.AddExperimentalGroupsDialog.ExperimentalGroupSubmitEvent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.AddExperimentalVariablesDialog;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentInfoComponent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalGroupCardCollection;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalVariablesComponent;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ExperimentInformationService.ExperimentalGroupDTO;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalDesign.AddExperimentalGroupResponse.ResponseCode;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalVariable;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b>Experimental Details Component</b>
 *
 * <p>A PageComponent based Composite showing the information stored in the
 * {@link life.qbic.projectmanagement.domain.project.experiment.ExperimentalDesign} associated with
 * a {@link Project} within the {@link ExperimentInformationPage}
 */
@UIScope
@SpringComponent
public class ExperimentDetailsComponent extends Composite<PageComponent> {

  private ExperimentId experimentId;
  private final ExperimentalVariablesComponent experimentalVariablesComponent = ExperimentalVariablesComponent.create(new ArrayList<>());
  private static final Logger log = logger(ExperimentDetailsComponent.class);
  @Serial
  private static final long serialVersionUID = -8992991642015281245L;
  private final ExperimentInformationService experimentInformationService;
  private final HorizontalLayout tagLayout = new HorizontalLayout();
  private final TabSheet experimentSheet = new TabSheet();
  private final Div contentExperimentalGroupsTab = new Div();
  private final Div experimentSummary = new Div();
  private final ExperimentalGroupCardCollection experimentalGroupsCollection = new ExperimentalGroupCardCollection();
  private final CardComponent blockingVariableCard = new CardComponent();
  private final Button addBlockingVariableButton = new Button("Add");
  private final AddExperimentalVariablesDialog addExperimentalVariablesDialog;
  private final AddExperimentalGroupsDialog experimentalGroupsDialog;
  private final DisclaimerCard noExperimentalVariablesDefined;
  private final CreationCard experimentalGroupCreationCard = CreationCard.create(
      "Add more experimental groups");
  private final DisclaimerCard addExperimentalVariablesNote;


  public ExperimentDetailsComponent(@Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService) {
    Objects.requireNonNull(projectInformationService);
    this.experimentInformationService = Objects.requireNonNull(experimentInformationService);

    this.addExperimentalVariablesDialog = new AddExperimentalVariablesDialog();
    this.noExperimentalVariablesDefined = createNoVariableDisclaimer();
    this.addExperimentalVariablesNote = createNoVariableDisclaimer();
    this.experimentalGroupsDialog = createExperimentalGroupDialog();

    layoutComponent();
    configureComponent();
  }

  private DisclaimerCard createNoVariableDisclaimer() {
    var disclaimer = DisclaimerCard.createWithTitle("Missing variables",
        "No experiment variables defined", "Add");
    disclaimer.subscribe(listener -> displayAddExperimentalVariablesDialog());
    return disclaimer;
  }

  private AddExperimentalGroupsDialog createExperimentalGroupDialog() {
    AddExperimentalGroupsDialog dialog = new AddExperimentalGroupsDialog();
    dialog.addExperimentalGroupSubmitListener(
        groupSubmitted -> onGroupSubmitted(groupSubmitted));
    return dialog;
  }

  private void layoutComponent() {
    initTagAndNotesLayout();
    layoutTabSheet();
  }

  private void configureComponent() {
    configureExperimentalGroupCreation();
    addCancelListenerForAddVariableDialog();
    addConfirmListenerForAddVariableDialog();
    addListenerForNewVariableEvent();
  }

  private void addListenerForNewVariableEvent() {
    this.experimentalVariablesComponent.subscribeToAddEvent(listener -> displayAddExperimentalVariablesDialog());
  }

  private void displayAddExperimentalVariablesDialog() {
    this.addExperimentalVariablesDialog.open();
  }

  public void onGroupSubmitted(ExperimentalGroupSubmitEvent groupSubmitted) {
    Result<ExperimentalGroup, ResponseCode> response = experimentInformationService.addExperimentalGroupToExperiment(
        experimentId,
        new ExperimentalGroupDTO(groupSubmitted.variableLevels(), groupSubmitted.sampleSize()));
    if (response.isValue()) {
      handleGroupSubmittedSuccess();
    } else {
      handleDuplicateConditionInput();
    }
  }

  private void initTagAndNotesLayout() {
    VerticalLayout tagAndNotesLayout = new VerticalLayout();
    tagLayout.setWidthFull();
    tagLayout.addClassName("spacing-s");
    tagLayout.addClassName(Overflow.HIDDEN);
    tagLayout.addClassName(Whitespace.NOWRAP);
    tagLayout.addClassName(TextOverflow.ELLIPSIS);
    tagLayout.addClassName(Display.INLINE);
    Span noNotesDefined = new Span("Click to add Notes");
    ToggleDisplayEditComponent<Span, TextField, String> experimentNotes = new ToggleDisplayEditComponent<>(
        Span::new, new TextField(), noNotesDefined);
    tagAndNotesLayout.setWidthFull();
    tagAndNotesLayout.add(tagLayout, experimentNotes);
    tagAndNotesLayout.setPadding(false);
    tagAndNotesLayout.setMargin(false);
    getContent().addContent(tagAndNotesLayout);
  }

  private void layoutTabSheet() {
    experimentSheet.add("Summary", experimentSummary);
    experimentSheet.add("Experimental Groups", contentExperimentalGroupsTab);
    getContent().addContent(experimentSheet);
    experimentSheet.setSizeFull();
  }

  private void configureExperimentalGroupCreation() {
    experimentalGroupCreationCard.addListener(this::handleEvent);
  }

  private void addCancelListenerForAddVariableDialog() {
    addExperimentalVariablesDialog.subscribeToCancelEvent(it -> it.getSource().close());
  }

  private void handleGroupSubmittedSuccess() {
    reloadExperimentalGroups();
    experimentalGroupsDialog.close();
  }

  private void handleDuplicateConditionInput() {
    InformationMessage infoMessage = new InformationMessage(
        "A group with the same condition exists already.", "");
    StyledNotification notification = new StyledNotification(infoMessage);
    notification.open();
  }

  private void handleEvent(CreationClickedEvent creationClickedEvent) {
    experimentalGroupsDialog.open();
  }

  private void reloadExperimentalGroups() {
    loadExperimentalGroups();
    addCreationCardToExperimentalGroupCollection();
  }

  private void loadExperimentalGroups() {
    Objects.requireNonNull(experimentId, "Experiment id not set");
    // We load the experimental groups of the experiment and render them as cards
    List<ExperimentalGroupCard> experimentalGroupsCards = experimentInformationService.experimentalGroupsFor(
        experimentId).stream().map(ExperimentalGroupCard::new).toList();

    // We register the experimental details component as listener for group deletion events
    experimentalGroupsCards.forEach(this::subscribeToDeletionClickEvent);
    experimentalGroupsCollection.setComponents(experimentalGroupsCards);
    addCreationCardToExperimentalGroupCollection();
  }

  private void addCreationCardToExperimentalGroupCollection() {
    experimentalGroupsCollection.addComponentAsFirst(experimentalGroupCreationCard);
  }

  private void subscribeToDeletionClickEvent(ExperimentalGroupCard experimentalGroupCard) {
    experimentalGroupCard.addDeletionEventListener(ExperimentDetailsComponent.this::handleEvent);
  }

  private void handleEvent(ExperimentalGroupDeletionEvent experimentalGroupDeletionEvent) {
    experimentInformationService.deleteExperimentGroup(experimentId,
        experimentalGroupDeletionEvent.getSource().groupId());
    experimentalGroupsCollection.remove(experimentalGroupDeletionEvent.getSource());
  }

  private void handleEvent(DisclaimerConfirmedEvent disclaimerConfirmedEvent) {
    experimentSheet.setSelectedIndex(0);
  }

  private void addConfirmListenerForAddVariableDialog() {
    addExperimentalVariablesDialog.subscribeToConfirmEvent(it -> {
      try {
        registerExperimentalVariables(it.getSource());
        it.getSource().close();
        loadExperiment(experimentId);
      } catch (Exception e) {
        log.error("Experimental variables registration failed.", e);
      }
    });
  }

  private void registerExperimentalVariables(
      AddExperimentalVariablesDialog experimentalVariablesDialog) {
    experimentalVariablesDialog.definedVariables().forEach(experimentalVariableContent -> {
      experimentInformationService.addVariableToExperiment(experimentId,
          experimentalVariableContent.name(), experimentalVariableContent.unit(),
          experimentalVariableContent.levels());
    });
  }

  /**
   * Sets the experiment identifier for the component, the component does the rest.
   *
   * @param experimentId the experiment identifier
   * @since 1.0.0
   */
  public void setExperiment(ExperimentId experimentId) {
    this.loadExperiment(experimentId);
  }

  private void loadExperiment(ExperimentId experimentId) {
    experimentInformationService.find(experimentId).ifPresent(this::loadExperimentInformation);
  }

  private void loadExperimentInformation(Experiment experiment) {
    this.experimentId = experiment.experimentId();
    getContent().addTitle(experiment.getName());
    loadTagInformation(experiment);
    loadExperimentInfo(experiment);
    loadBlockingVariableInformation();
    fillExperimentalGroupDialog();
    loadExperimentalGroups();
    if (experiment.variables().isEmpty()) {
      useCaseNoVariablesYet();
    } else {
      removeDisclaimer();
      displayExperimentalGroupsCollection();
    }
  }

  private void loadTagInformation(Experiment experiment) {
    tagLayout.removeAll();
    List<String> tags = new ArrayList<>();
    experiment.getSpecies().forEach(species -> tags.add(species.value()));
    experiment.getSpecimens().forEach(specimen -> tags.add(specimen.value()));
    experiment.getAnalytes().forEach(analyte -> tags.add(analyte.value()));
    tags.forEach(tag -> tagLayout.add(new Tag(tag)));
    tagLayout.getElement().setAttribute("Title", String.join(" ", tags));
  }

  private void loadExperimentInfo(Experiment experiment) {
    ExperimentInfoComponent factSheet = ExperimentInfoComponent.create(experiment.getSpecies(),
        experiment.getSpecimens(), experiment.getAnalytes());
    this.experimentalVariablesComponent.setExperimentalVariables(experiment.variables());
    ExperimentDetailsComponent.this.experimentSummary.removeAll();
    ExperimentDetailsComponent.this.experimentSummary.add(factSheet);
    if (experiment.variables().isEmpty()) {
      ExperimentDetailsComponent.this.experimentSummary.add(addExperimentalVariablesNote);
    } else {
      ExperimentDetailsComponent.this.experimentSummary.add(experimentalVariablesComponent);
    }
    factSheet.showMenu();
  }

  private void loadBlockingVariableInformation() {
    //ToDo load information from backend once implemented
  }

  private void fillExperimentalGroupDialog() {
    Objects.requireNonNull(experimentId, "experiment id not set");
    List<ExperimentalVariable> variables = experimentInformationService.getVariablesOfExperiment(
        experimentId);
    List<VariableLevel> levels = variables.stream()
        .flatMap(variable -> variable.levels().stream()).toList();
    experimentalGroupsDialog.setLevels(levels);
  }

  private void useCaseNoVariablesYet() {
    displayDisclaimer();
    hideExperimentalGroupsCollection();
  }

  private void removeDisclaimer() {
    contentExperimentalGroupsTab.remove(noExperimentalVariablesDefined);
  }

  private void displayExperimentalGroupsCollection() {
    contentExperimentalGroupsTab.add(experimentalGroupsCollection);
  }

  private void displayDisclaimer() {
    contentExperimentalGroupsTab.add(noExperimentalVariablesDefined);
  }

  private void hideExperimentalGroupsCollection() {
    contentExperimentalGroupsTab.remove(experimentalGroupsCollection);
  }

  private void initBlockingVariableCard() {
    blockingVariableCard.addTitle("Blocking Variables");
    VerticalLayout templateLayout = new VerticalLayout();
    Span templateText = new Span("No Blocking Variable defined");
    addBlockingVariableButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    templateLayout.add(templateText, addBlockingVariableButton);
    templateLayout.setAlignItems(Alignment.CENTER);
    templateLayout.setSizeFull();
    templateLayout.setJustifyContentMode(JustifyContentMode.CENTER);
    blockingVariableCard.addContent(templateLayout);
    blockingVariableCard.setMargin(false);
  }

}
