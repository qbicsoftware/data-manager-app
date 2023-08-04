package life.qbic.datamanager.views.projects.project.experiments.experiment;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoIcon;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.ConfirmEvent;
import life.qbic.datamanager.views.general.DisclaimerCard;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.ToggleDisplayEditComponent;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentEditEvent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentInfoComponent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalGroupCardCollection;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalGroupsDialog;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalGroupsDialog.ExperimentalGroupContent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalVariableContent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalVariablesComponent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalVariablesDialog;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.ExperimentInformationContent;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.DeletionService;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ExperimentInformationService.ExperimentalGroupDTO;
import life.qbic.projectmanagement.application.ExperimentalDesignSearchService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
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
 * a {@link Project} within the {@link ExperimentInformationMain}
 */
@UIScope
@SpringComponent
public class ExperimentDetailsComponent extends PageArea {
  private static final Logger log = logger(ExperimentDetailsComponent.class);
  @Serial
  private static final long serialVersionUID = -8992991642015281245L;
  private final transient ExperimentInformationService experimentInformationService;
  private final transient ExperimentalDesignSearchService experimentalDesignSearchService;
  private final Div content = new Div();
  private final Div header = new Div();
  private final Span title = new Span();
  private final Span buttonBar = new Span();
  private final Div tagCollection = new Div();
  private final TabSheet experimentSheet = new TabSheet();
  private final ExperimentalVariablesComponent experimentalVariablesComponent = ExperimentalVariablesComponent.create(
      new ArrayList<>());
  private final Div contentExperimentalGroupsTab = new Div();
  private final Div experimentSummary = new Div();
  private final ExperimentalGroupCardCollection experimentalGroupsCollection = new ExperimentalGroupCardCollection();
  private final ExperimentalVariablesDialog addExperimentalVariablesDialog;
  private final DisclaimerCard noExperimentalVariablesDefined;
  private final DisclaimerCard addExperimentalVariablesNote;
  private Context context;
  private boolean hasExperimentalGroups;
  private final DeletionService deletionService;
  private final List<ComponentEventListener<ExperimentEditEvent>> editListeners = new ArrayList<>();


  public ExperimentDetailsComponent(
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired DeletionService deletionService,
      @Autowired ExperimentalDesignSearchService experimentalDesignSearchService) {
    this.experimentInformationService = Objects.requireNonNull(experimentInformationService);
    this.deletionService = Objects.requireNonNull(deletionService);
    this.experimentalDesignSearchService = Objects.requireNonNull(experimentalDesignSearchService);
    this.addExperimentalVariablesDialog = new ExperimentalVariablesDialog();
    this.noExperimentalVariablesDefined = createNoVariableDisclaimer();
    this.addExperimentalVariablesNote = createNoVariableDisclaimer();
    this.addClassName("experiment-details-component");
    layoutComponent();
    configureComponent();
  }

  private Notification createSampleRegistrationPossibleNotification(String projectId) {
    Notification notification = new Notification();

    String samplesUrl = Projects.SAMPLES.formatted(projectId);
    Div text = new Div(new Text("You can now register sample batches. "),
        new Anchor(samplesUrl, new Button("Go to Samples", event -> notification.close())));

    Button closeButton = new Button(LumoIcon.CROSS.create());
    closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    closeButton.addClickListener(event -> notification.close());

    Component layout = new HorizontalLayout(text, closeButton);
    layout.addClassName("content");
    notification.setPosition(Position.BOTTOM_START);
    notification.setDuration(3_000);
    notification.add(layout);
    return notification;
  }

  private DisclaimerCard createNoVariableDisclaimer() {
    var disclaimer = DisclaimerCard.createWithTitle("Missing variables",
        "No experiment variables defined", "Add");
    disclaimer.subscribe(listener -> displayAddExperimentalVariablesDialog());
    return disclaimer;
  }

  private void layoutComponent() {
    this.add(header);
    header.addClassName("header");
    this.add(content);
    //Necessary to avoid css collution
    content.addClassName("details-content");
    initButtonBar();
    header.add(title, buttonBar);
    title.addClassName("title");
    addTagCollectionToContent();
    addExperimentNotesComponent();
    layoutTabSheet();
  }

  private void configureComponent() {
    configureExperimentalGroupCreation();
    configureExperimentalGroupsEdit();
    addCancelListenerForAddVariableDialog();
    addConfirmListenerForAddVariableDialog();
    addConfirmListenerForEditVariableDialog();
    addListenerForNewVariableEvent();
  }

  private void initButtonBar() {
    Button editButton = new Button("Edit");
    editButton.addClickListener(event -> generateExperimentInformationDialog());
    buttonBar.add(editButton);
  }

  private void generateExperimentInformationDialog() {
    ExperimentId experimentId = context.experimentId().orElseThrow();
    Optional<Experiment> experiment = experimentInformationService.find(experimentId);
    experiment.ifPresentOrElse(exp -> {
          var editDialog = ExperimentInformationDialog.prefilled(experimentalDesignSearchService,
              exp.getName(), exp.getSpecies(),
              exp.getSpecimens(), exp.getAnalytes());
          editDialog.setConfirmButtonLabel("Save");
          editDialog.addCancelEventListener(
              experimentInformationDialogCancelEvent -> editDialog.close());
          editDialog.addConfirmEventListener(experimentInformationDialogConfirmEvent -> {
            ExperimentInformationContent experimentInformationContent = experimentInformationDialogConfirmEvent.getSource()
                .content();
            experimentInformationService.editExperiment(experimentId,
                experimentInformationContent.experimentName(), experimentInformationContent.species(),
                experimentInformationContent.specimen(), experimentInformationContent.analytes());
            editDialog.close();
            fireEditEvent();
          });
          editDialog.open();
        }
        , () -> {
          throw new ApplicationException(
              "Experiment information could not be retrieved from service");
        });
  }

  private void addConfirmListenerForEditVariableDialog() {
    experimentalVariablesComponent.subscribeToEditEvent(experimentalVariablesEditEvent -> {
      ExperimentId experimentId = context.experimentId().orElseThrow();
      var editDialog = ExperimentalVariablesDialog.prefilled(
          experimentInformationService.getVariablesOfExperiment(experimentId));
      editDialog.addCancelEventListener(
          experimentalVariablesDialogCancelEvent -> editDialog.close());
      editDialog.addConfirmEventListener(experimentalVariablesDialogConfirmEvent -> {
        var confirmDialog = experimentalGroupDeletionConfirmDialog();
        confirmDialog.addConfirmListener(confirmDeletionEvent -> {
          deleteExistingExperimentalVariables(experimentId);
          addExperimentalVariables(
              experimentalVariablesDialogConfirmEvent.getSource().definedVariables());
          editDialog.close();
          reloadExperimentInformation();
        });
        confirmDialog.open();
      });
      editDialog.open();
    });
  }

  private static ConfirmDialog experimentalGroupDeletionConfirmDialog() {
    var confirmDialog = new ConfirmDialog();
    confirmDialog.setHeader("Your experimental groups will be deleted");
    confirmDialog.setText(
        "Editing experimental variables requires all experimental groups to be deleted. Are you sure you want to delete them?");
    confirmDialog.setConfirmText("Delete experimental groups");
    confirmDialog.setCancelable(true);
    confirmDialog.setCancelText("Abort");
    confirmDialog.setRejectable(false);
    return confirmDialog;
  }

  private void reloadExperimentInformation() {
    experimentInformationService.find(context.experimentId().orElseThrow())
        .ifPresent(this::loadExperimentInformation);
  }

  private void deleteExistingExperimentalVariables(ExperimentId experimentId) {
    var result = deletionService.deleteAllExperimentalVariables(experimentId);
    result.onError(responseCode -> {
      throw new ApplicationException("variable deletion failed: " + responseCode, ErrorCode.GENERAL,
          ErrorParameters.empty());
    });
  }

  private void addListenerForNewVariableEvent() {
    this.experimentalVariablesComponent.subscribeToAddEvent(
        listener -> displayAddExperimentalVariablesDialog());
  }

  private void displayAddExperimentalVariablesDialog() {
    this.addExperimentalVariablesDialog.open();
  }

  private void addTagCollectionToContent() {
    tagCollection.addClassName("tag-collection");
    content.add(tagCollection);
  }

  private void addExperimentNotesComponent() {
    Span emptyNotes = new Span("Click to add Notes");
    ToggleDisplayEditComponent<Span, TextField, String> experimentNotes = new ToggleDisplayEditComponent<>(
        Span::new, new TextField(), emptyNotes);
    content.add(experimentNotes);
  }

  private void layoutTabSheet() {
    experimentSheet.add("Summary", experimentSummary);
    experimentSummary.addClassName(Display.FLEX);
    experimentSheet.add("Experimental Groups", contentExperimentalGroupsTab);
    content.add(experimentSheet);
    experimentSheet.setSizeFull();
  }

  private void configureExperimentalGroupCreation() {
    experimentalGroupsCollection.addAddEventListener(listener -> openExperimentalGroupAddDialog());
  }

  /**
   * Adds a listener for an {@link ExperimentEditEvent}
   *
   * @param listener the listener to add
   */
  public void addExperimentEditEventListener(
      ComponentEventListener<ExperimentEditEvent> listener) {
    this.editListeners.add(listener);
  }

  private void fireEditEvent() {
    ExperimentId experimentId = context.experimentId().orElseThrow();
    var editEvent = new ExperimentEditEvent(this, experimentId, true);
    editListeners.forEach(listener -> listener.onComponentEvent(editEvent));
  }

  private void openExperimentalGroupAddDialog() {
    ExperimentId experimentId = context.experimentId().orElseThrow();
    List<ExperimentalVariable> variables = experimentInformationService.getVariablesOfExperiment(
        experimentId);
    List<VariableLevel> levels = variables.stream()
        .flatMap(variable -> variable.levels().stream())
        .toList();
    var dialog = getExperimentalGroupsDialogForAdding(levels);
    dialog.open();
  }

  private ExperimentalGroupsDialog getExperimentalGroupsDialogForAdding(
      List<VariableLevel> levels) {
    var dialog = ExperimentalGroupsDialog.empty(levels);
    dialog.addCancelEventListener(cancelEvent -> cancelEvent.getSource().close());
    dialog.addConfirmEventListener(this::onAddExperimentalGroupDialogConfirmed);
    return dialog;
  }

  private void onAddExperimentalGroupDialogConfirmed(
      ConfirmEvent<ExperimentalGroupsDialog> confirmEvent) {
    ExperimentalGroupsDialog dialog = confirmEvent.getSource();
    addExperimentalGroups(dialog.experimentalGroups());
    reloadExperimentalGroups();
    dialog.close();
  }


  private void configureExperimentalGroupsEdit() {
    experimentalGroupsCollection.addEditEventListener(listener -> {
      ExperimentId experimentId = context.experimentId().orElseThrow();
      List<ExperimentalVariable> variables = experimentInformationService.getVariablesOfExperiment(
          experimentId);
      List<VariableLevel> levels = variables.stream()
          .flatMap(variable -> variable.levels().stream()).toList();
      var experimentalGroups = experimentInformationService.getExperimentalGroups(experimentId)
          .stream().map(this::toContent).toList();
      var dialog = ExperimentalGroupsDialog.prefilled(levels, experimentalGroups);
      dialog.addCancelEventListener(cancelEvent -> cancelEvent.getSource().close());
      dialog.addConfirmEventListener(
          confirmEvent -> {
            editExperimentalGroups(confirmEvent.getSource().experimentalGroups());
            reloadExperimentalGroups();
            dialog.close();
          });
      dialog.open();
    });
  }

  private void editExperimentalGroups(
      Collection<ExperimentalGroupContent> experimentalGroupContents) {
    ExperimentId experimentId = context.experimentId().orElseThrow();
    deletionService.deleteAllExperimentalGroups(experimentId).onError(error -> {
      throw new ApplicationException(
          "Could not edit experiments because samples are already registered.");
    });
    addExperimentalGroups(experimentalGroupContents);
  }

  private void addExperimentalGroups(
      Collection<ExperimentalGroupContent> experimentalGroupContents) {
    List<ExperimentalGroupDTO> experimentalGroupDTOS = experimentalGroupContents.stream()
        .map(this::toExperimentalGroupDTO).toList();
    ExperimentId experimentId = context.experimentId().orElseThrow();
    Result<Collection<ExperimentalGroup>, ResponseCode> result = experimentInformationService.addExperimentalGroupsToExperiment(
        experimentId, experimentalGroupDTOS);
    result.onError(error -> {
      throw new ApplicationException(
          "Could not save one or more experimental groups %s %nReason: %s".formatted(
              Arrays.toString(
                  experimentalGroupContents.toArray()), error));
    });
  }

  private ExperimentalGroupDTO toExperimentalGroupDTO(
      ExperimentalGroupContent experimentalGroupContent) {
    return new ExperimentalGroupDTO(experimentalGroupContent.variableLevels(),
        experimentalGroupContent.size());
  }

  private ExperimentalGroupContent toContent(ExperimentalGroupDTO experimentalGroupDTO) {
    return new ExperimentalGroupContent(experimentalGroupDTO.replicateCount(),
        experimentalGroupDTO.levels());
  }

  private void addCancelListenerForAddVariableDialog() {
    addExperimentalVariablesDialog.addCancelEventListener(it -> it.getSource().close());
  }

  private void showSampleRegistrationPossibleNotification() {
    String projectId = this.context.projectId().map(ProjectId::value).orElseThrow();
    Notification notification = createSampleRegistrationPossibleNotification(projectId);
    notification.open();
  }


  private void reloadExperimentalGroups() {
    loadExperimentalGroups();
    if (hasExperimentalGroups) {
      showSampleRegistrationPossibleNotification();
    }
  }

  private void loadExperimentalGroups() {
    // We load the experimental groups of the experiment and render them as cards
    List<ExperimentalGroup> experimentalGroups = experimentInformationService.experimentalGroupsFor(
        context.experimentId().orElseThrow());
    List<ExperimentalGroupCard> experimentalGroupsCards = experimentalGroups.stream()
        .map(ExperimentalGroupCard::new).toList();

    experimentalGroupsCollection.setContent(experimentalGroupsCards);
    this.hasExperimentalGroups = !experimentalGroupsCards.isEmpty();
  }

  private void addConfirmListenerForAddVariableDialog() {
    addExperimentalVariablesDialog.addConfirmEventListener(it -> {
      try {
        addExperimentalVariables(it.getSource().definedVariables());
        it.getSource().close();
        setContext(this.context);
        if (hasExperimentalGroups) {
          showSampleRegistrationPossibleNotification();
        }
      } catch (Exception e) {
        log.error("Experimental variables registration failed.", e);
      }
    });
  }

  private void addExperimentalVariables(
      List<ExperimentalVariableContent> experimentalVariableContents) {
    experimentalVariableContents.forEach(
        experimentalVariableContent -> experimentInformationService.addVariableToExperiment(
            context.experimentId().orElseThrow(),
            experimentalVariableContent.name(), experimentalVariableContent.unit(),
            experimentalVariableContent.levels()));
  }

  public void setContext(Context context) {
    ExperimentId experimentId = context.experimentId()
        .orElseThrow(() -> new ApplicationException("no experiment id in context " + context));
    context.projectId()
        .orElseThrow(() -> new ApplicationException("no project id in context " + context));
    this.context = context;
    experimentInformationService.find(experimentId).ifPresent(this::loadExperimentInformation);
  }

  private void loadExperimentInformation(Experiment experiment) {
    title.setText(experiment.getName());
    loadTagInformation(experiment);
    loadExperimentInfo(experiment);
    loadExperimentalGroups();
    if (experiment.variables().isEmpty()) {
      onNoVariablesDefined();
    } else {
      removeNoExperimentalVariablesDefinedDisclaimer();
      contentExperimentalGroupsTab.add(experimentalGroupsCollection);
    }
  }


  private void loadTagInformation(Experiment experiment) {
    tagCollection.removeAll();
    List<String> tags = new ArrayList<>();
    experiment.getSpecies().forEach(species -> tags.add(species.value()));
    experiment.getSpecimens().forEach(specimen -> tags.add(specimen.value()));
    experiment.getAnalytes().forEach(analyte -> tags.add(analyte.value()));
    tags.stream().map(Tag::new).forEach(tagCollection::add);
  }

  private void loadExperimentInfo(Experiment experiment) {
    ExperimentInfoComponent factSheet = ExperimentInfoComponent.create(experiment.getSpecies(),
        experiment.getSpecimens(), experiment.getAnalytes());
    this.experimentSummary.removeAll();
    this.experimentSummary.add(factSheet);
    factSheet.showMenu();
    reloadExperimentInformation(experiment);
  }

  private void reloadExperimentInformation(Experiment experiment) {
    this.experimentalVariablesComponent.setExperimentalVariables(experiment.variables());
    if (experiment.variables().isEmpty()) {
      this.experimentSummary.add(addExperimentalVariablesNote);
    } else {
      this.experimentSummary.add(experimentalVariablesComponent);
    }
  }

  private void onNoVariablesDefined() {
    contentExperimentalGroupsTab.add(noExperimentalVariablesDefined);
    contentExperimentalGroupsTab.remove(experimentalGroupsCollection);
  }

  private void removeNoExperimentalVariablesDefinedDisclaimer() {
    contentExperimentalGroupsTab.remove(noExperimentalVariablesDefined);
  }
}
