package life.qbic.datamanager.views.projects.project.experiments.experiment;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import life.qbic.datamanager.views.general.Disclaimer;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.ToggleDisplayEditComponent;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentEditEvent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalGroupCardCollection;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalGroupsDialog;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalGroupsDialog.ExperimentalGroupContent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalGroupsExistDialog;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalVariableContent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalVariablesComponent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalVariablesDialog;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.SamplesExistDialog;
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
  private final Div experimentalGroups = new Div();
  private final Div experimentalVariables = new Div();
  private final ExperimentalGroupCardCollection experimentalGroupsCollection = new ExperimentalGroupCardCollection();
  private final ExperimentalVariablesDialog addExperimentalVariablesDialog;
  private final Disclaimer noExperimentalVariablesDefined;
  private final Disclaimer noExperimentalGroupsDefined;
  private final Disclaimer addExperimentalVariablesNote;
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
    this.noExperimentalGroupsDefined = createNoGroupsDisclaimer();
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

  private Disclaimer createNoVariableDisclaimer() {
    var disclaimer = Disclaimer.createWithTitle("Design your experiment",
        "Get started by adding experimental variables", "Add variables");
    disclaimer.subscribe(listener -> openAddExperimentalVariablesDialog());
    return disclaimer;
  }

  private Disclaimer createNoGroupsDisclaimer() {
    var disclaimer = Disclaimer.createWithTitle("Design your experiment",
        "Create conditions for your samples by adding experimental groups", "Add groups");
    disclaimer.subscribe(listener -> openExperimentalGroupAddDialog());
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
    addListenerForNewEditEvent();
    addListenerForNewVariableEvent();
  }

  private void initButtonBar() {
    Button editButton = new Button("Edit");
    editButton.addClickListener(event -> openExperimentInformationDialog());
    buttonBar.add(editButton);
  }

  private void openExperimentInformationDialog() {
    ExperimentId experimentId = context.experimentId().orElseThrow();
    Optional<Experiment> experiment = experimentInformationService.find(experimentId);
    experiment.ifPresentOrElse(exp -> {
          ExperimentInformationDialog experimentInformationDialog = openExperimentInformationDialog(
              exp);
          addExperimentInformationDialogListeners(experimentId, experimentInformationDialog);
          experimentInformationDialog.open();
        }
        , () -> {
          throw new ApplicationException(
              "Experiment information could not be retrieved from service");
        });
  }

  private ExperimentInformationDialog openExperimentInformationDialog(Experiment experiment) {
    ExperimentInformationDialog experimentInformationDialog = ExperimentInformationDialog.prefilled(
        experimentalDesignSearchService,
        experiment.getName(), experiment.getSpecies(),
        experiment.getSpecimens(), experiment.getAnalytes());
    experimentInformationDialog.setConfirmButtonLabel("Save");
    return experimentInformationDialog;
  }

  private void addExperimentInformationDialogListeners(ExperimentId experimentId,
      ExperimentInformationDialog experimentInformationDialog) {
    experimentInformationDialog.addCancelEventListener(
        experimentInformationDialogCancelEvent -> experimentInformationDialog.close());
    experimentInformationDialog.addConfirmEventListener(experimentInformationDialogConfirmEvent -> {
      ExperimentInformationContent experimentInformationContent = experimentInformationDialogConfirmEvent.getSource()
          .content();
      experimentInformationService.editExperimentInformation(experimentId,
          experimentInformationContent.experimentName(), experimentInformationContent.species(),
          experimentInformationContent.specimen(), experimentInformationContent.analytes());
      experimentInformationDialog.close();
      fireEditEvent();
    });
  }

  private void addConfirmListenerForEditVariableDialog() {
    experimentalVariablesComponent.addEditListener(editButtonClickedEvent -> {
      int numOfExperimentalGroups = experimentInformationService.getExperimentalGroups(
          context.experimentId().orElseThrow()).size();
      if (numOfExperimentalGroups > 0) {
        showExperimentalGroupExistsDialog(numOfExperimentalGroups);
      } else {
        showExperimentalVariableDialog();
      }
    });
  }

  private void showExperimentalVariableDialog() {
    ExperimentId experimentId = context.experimentId().orElseThrow();
    var editDialog = ExperimentalVariablesDialog.prefilled(
        experimentInformationService.getVariablesOfExperiment(experimentId));
    editDialog.addCancelEventListener(
        experimentalVariablesDialogCancelEvent -> editDialog.close());
    editDialog.addConfirmEventListener(confirmEvent -> {
      deleteExistingExperimentalVariables(experimentId);
      addExperimentalVariables(
          confirmEvent.getSource().definedVariables());
      editDialog.close();
      reloadExperimentalVariables();
    });
    editDialog.open();
  }

  private void showExperimentalGroupExistsDialog(int numOfExperimentalGroups) {
    ExperimentalGroupsExistDialog experimentalGroupsExistDialog = new ExperimentalGroupsExistDialog(
        numOfExperimentalGroups);
    experimentalGroupsExistDialog.addConfirmListener(
        confirmEvent -> {
          experimentSheet.setSelectedIndex(1);
          confirmEvent.getSource().close();
        });
    experimentalGroupsExistDialog.addRejectListener(rejectEvent -> rejectEvent.getSource().close());
    experimentalGroupsExistDialog.open();
    new SamplesExistDialog().open();
  }

  private void reloadExperimentalVariables() {
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

  private void addListenerForNewEditEvent() {
    this.editListeners.add(event -> experimentInformationService.find(event.experimentId())
        .ifPresent(this::loadExperimentInformation));
  }

  private void addListenerForNewVariableEvent() {
    this.experimentalVariablesComponent.addAddListener(
        listener -> openAddExperimentalVariablesDialog());
  }

  private void openAddExperimentalVariablesDialog() {
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
    experimentSheet.add("Experimental Variables", experimentalVariables);
    experimentalVariables.addClassName(Display.FLEX);
    experimentalVariables.addClassName("experimental-variables-container");
    experimentSheet.add("Experimental Groups", experimentalGroups);
    experimentalGroups.addClassName("experimental-groups-container");
    content.add(experimentSheet);
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
      onGroupsDefined();
      showSampleRegistrationPossibleNotification();
    } else {
      onNoGroupsDefined();
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
    loadExperimentalVariables(experiment);
    loadExperimentalGroups();
    if (experiment.variables().isEmpty()) {
      onNoVariablesDefined();
      return;
    }
    if (experiment.getExperimentalGroups().isEmpty()) {
      onNoGroupsDefined();
    } else {
      onGroupsDefined();
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

  private void loadExperimentalVariables(Experiment experiment) {
    this.experimentalVariables.removeAll();
    this.experimentalVariablesComponent.setExperimentalVariables(experiment.variables());
    if (experiment.variables().isEmpty()) {
      this.experimentalVariables.add(addExperimentalVariablesNote);
    } else {
      this.experimentalVariables.add(experimentalVariablesComponent);
    }
  }

  private void onNoVariablesDefined() {
    experimentalGroups.removeAll();
    experimentalGroups.add(noExperimentalVariablesDefined);

  }

  private void onNoGroupsDefined() {
    experimentalGroups.removeAll();
    experimentalGroups.add(noExperimentalGroupsDefined);
  }

  private void onGroupsDefined() {
    experimentalGroups.removeAll();
    experimentalGroups.add(experimentalGroupsCollection);
  }
}
