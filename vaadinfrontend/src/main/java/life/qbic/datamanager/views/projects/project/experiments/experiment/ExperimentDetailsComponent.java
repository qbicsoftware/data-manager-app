package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
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
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.ConfirmEvent;
import life.qbic.datamanager.views.general.Disclaimer;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.ToggleDisplayEditComponent;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.CardCollection;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExistingGroupsPreventVariableEdit;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExistingSamplesPreventVariableEdit;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalGroupsDialog;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalGroupsDialog.ExperimentalGroupContent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalVariableContent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalVariablesDialog;
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.ExperimentInformationContent;
import life.qbic.projectmanagement.application.DeletionService;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ExperimentInformationService.ExperimentalGroupDTO;
import life.qbic.projectmanagement.application.ExperimentalDesignSearchService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
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

  @Serial
  private static final long serialVersionUID = -8992991642015281245L;
  private final transient ExperimentInformationService experimentInformationService;
  private final SampleInformationService sampleInformationService;
  private final transient ExperimentalDesignSearchService experimentalDesignSearchService;
  private final Div content = new Div();
  private final Div header = new Div();
  private final Span title = new Span();
  private final Span buttonBar = new Span();
  private final Div tagCollection = new Div();
  private final TabSheet experimentSheet = new TabSheet();
  private final Div experimentalGroups = new Div();
  private final Div experimentalVariables = new Div();
  private final CardCollection experimentalGroupsCollection = new CardCollection("GROUPS");
  private final CardCollection experimentalVariableCollection = new CardCollection("VARIABLES");
  private final Disclaimer noExperimentalVariablesDefined;
  private final Disclaimer noExperimentalGroupsDefined;
  private final Disclaimer addExperimentalVariablesNote;
  private Context context;
  private final DeletionService deletionService;
  private int experimentalGroupCount;


  public ExperimentDetailsComponent(
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired SampleInformationService sampleInformationService,
      @Autowired DeletionService deletionService,
      @Autowired ExperimentalDesignSearchService experimentalDesignSearchService) {
    this.experimentInformationService = Objects.requireNonNull(experimentInformationService);
    this.sampleInformationService = sampleInformationService;
    this.deletionService = Objects.requireNonNull(deletionService);
    this.experimentalDesignSearchService = Objects.requireNonNull(experimentalDesignSearchService);
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
    disclaimer.addDisclaimerConfirmedListener(
        confirmedEvent -> openExperimentalVariablesAddDialog());
    return disclaimer;
  }

  private Disclaimer createNoGroupsDisclaimer() {
    var disclaimer = Disclaimer.createWithTitle("Design your experiment",
        "Create conditions for your samples by adding experimental groups", "Add groups");
    disclaimer.addDisclaimerConfirmedListener(confirmedEvent -> openExperimentalGroupAddDialog());
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
    listenForExperimentCollectionComponentEvents();
    listenForExperimentalVariablesComponentEvents();
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
          addExperimentInformationDialogListeners(experimentInformationDialog);
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

  private void addExperimentInformationDialogListeners(
      ExperimentInformationDialog experimentInformationDialog) {
    experimentInformationDialog.addCancelEventListener(
        experimentInformationDialogCancelEvent -> experimentInformationDialogCancelEvent.getSource()
            .close());
    experimentInformationDialog.addConfirmEventListener(
        this::onExperimentInformationDialogConfirmEvent);
  }

  private void onExperimentInformationDialogConfirmEvent(
      ConfirmEvent<ExperimentInformationDialog> experimentInformationDialogConfirmEvent) {
    ExperimentId experimentId = context.experimentId().orElseThrow();

    ExperimentInformationContent experimentInformationContent = experimentInformationDialogConfirmEvent.getSource()
        .content();
    String oldExperimentName = experimentInformationService.find(experimentId)
        .map(Experiment::getName)
        .orElseThrow();
    String newExperimentName = experimentInformationContent.experimentName();
    experimentInformationService.editExperimentInformation(experimentId,
        newExperimentName, experimentInformationContent.species(),
        experimentInformationContent.specimen(), experimentInformationContent.analytes());
    experimentInformationDialogConfirmEvent.getSource().close();
    reloadExperimentInfo(context.experimentId().orElseThrow());
    if (!oldExperimentName.equals(newExperimentName)) {
      fireExperimentNameChangedEvent(oldExperimentName, newExperimentName);
    }
  }

  private void fireExperimentNameChangedEvent(String oldExperimentName, String newExperimentName) {
    ExperimentNameChangedEvent event = new ExperimentNameChangedEvent(context.experimentId()
        .orElseThrow(), oldExperimentName,
        newExperimentName, this, false);
    fireEvent(event);
  }

  private void listenForExperimentalVariablesComponentEvents() {
    experimentalVariableCollection.subscribeToAddEvent(addEvent -> openExperimentalVariablesAddDialog());
    experimentalVariableCollection.subscribeToEditEvent(editEvent -> openExperimentalVariablesEditDialog());
  }

  private void deleteExistingExperimentalVariables() {
    ExperimentId experimentId = context.experimentId().orElseThrow();
    var result = deletionService.deleteAllExperimentalVariables(experimentId);
    result.onError(responseCode -> {
      throw new ApplicationException("variable deletion failed: " + responseCode);
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

  private void reloadExperimentInfo(ExperimentId experimentId) {
    experimentInformationService.find(experimentId)
        .ifPresent(this::loadExperimentInformation);
  }

  private void openExperimentalVariablesAddDialog() {
    if (editVariablesNotAllowed()) {
      return;
    }
    var addDialog = new ExperimentalVariablesDialog();
    addDialog.addCancelEventListener(cancelEvent -> cancelEvent.getSource().close());
    addDialog.addConfirmEventListener(this::onExperimentalVariablesAddConfirmed);
    addDialog.open();
  }

  private void onExperimentalVariablesAddConfirmed(
      ExperimentalVariablesDialog.ConfirmEvent confirmEvent) {
    addExperimentalVariables(confirmEvent.getSource().definedVariables());
    confirmEvent.getSource().close();
    reloadExperimentInfo(context.experimentId().orElseThrow());
    if (hasExperimentalGroups()) {
      showSampleRegistrationPossibleNotification();
    }
  }

  private void openExperimentalVariablesEditDialog() {
    if (editVariablesNotAllowed()) {
      return;
    }
    ExperimentId experimentId = context.experimentId().orElseThrow();
    var editDialog = ExperimentalVariablesDialog.prefilled(
        experimentInformationService.getVariablesOfExperiment(experimentId));
    editDialog.addCancelEventListener(cancelEvent -> cancelEvent.getSource().close());
    editDialog.addConfirmEventListener(this::onExperimentalVariablesEditConfirmed);
    editDialog.open();
  }

  private void onExperimentalVariablesEditConfirmed(
      ExperimentalVariablesDialog.ConfirmEvent confirmEvent) {
    deleteExistingExperimentalVariables();
    addExperimentalVariables(confirmEvent.getSource().definedVariables());
    confirmEvent.getSource().close();
    reloadExperimentInfo(context.experimentId().orElseThrow());
  }

  private boolean editVariablesNotAllowed() {
    int numberOfRegisteredSamples = sampleInformationService.countPreviews(
        context.experimentId().orElseThrow(), "");
    if (numberOfRegisteredSamples > 0) {
      showExistingSamplesPreventVariableEdit(numberOfRegisteredSamples);
      return true;
    }
    int numOfExperimentalGroups = experimentInformationService.getExperimentalGroups(
        context.experimentId().orElseThrow()).size();
    if (numOfExperimentalGroups > 0) {
      showExistingGroupsPreventVariableEdit(numOfExperimentalGroups);
      return true;
    }
    return false;
  }

  private boolean editGroupsNotAllowed() {
    int numberOfRegisteredSamples = sampleInformationService.countPreviews(
        context.experimentId().orElseThrow(), "");
    if (numberOfRegisteredSamples > 0) {
      showExistingSamplesPreventGroupEdit(numberOfRegisteredSamples);
      return true;
    }
    return false;
  }

  private void showExistingSamplesPreventGroupEdit(int numberOfRegisteredSamples) {
    ExistingSamplesPreventGroupEdit existingSamplesPreventGroupEdit = new ExistingSamplesPreventGroupEdit(
        numberOfRegisteredSamples);
    existingSamplesPreventGroupEdit.open();
  }

  private void showExistingSamplesPreventVariableEdit(int sampleCount) {
    ExistingSamplesPreventVariableEdit existingSamplesPreventVariableEdit = new ExistingSamplesPreventVariableEdit(
        sampleCount);
    existingSamplesPreventVariableEdit.open();
  }

  private void showExistingGroupsPreventVariableEdit(int numOfExperimentalGroups) {
    ExistingGroupsPreventVariableEdit existingGroupsPreventVariableEdit = new ExistingGroupsPreventVariableEdit(
        numOfExperimentalGroups);
    existingGroupsPreventVariableEdit.addConfirmListener(
        confirmEvent -> {
          experimentSheet.setSelectedIndex(1);
          confirmEvent.getSource().close();
        });
    existingGroupsPreventVariableEdit.addRejectListener(
        rejectEvent -> rejectEvent.getSource().close());
    existingGroupsPreventVariableEdit.open();
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
    experimentalVariables.addClassName("experimental-groups-container");
    experimentSheet.add("Experimental Groups", experimentalGroups);
    experimentalGroups.addClassName("experimental-groups-container");
    content.add(experimentSheet);
  }

  private void listenForExperimentCollectionComponentEvents() {
    experimentalGroupsCollection.subscribeToAddEvent(listener -> openExperimentalGroupAddDialog());
    experimentalGroupsCollection.subscribeToEditEvent(editEvent -> openExperimentalGroupEditDialog());
  }

  /**
   * Adds a listener for an {@link ExperimentDetailsComponent.ExperimentNameChangedEvent}
   *
   * @param listener the listener to add
   */
  public void addExperimentNameChangedListener(
      ComponentEventListener<ExperimentNameChangedEvent> listener) {
    this.addListener(ExperimentNameChangedEvent.class, listener);
  }

  private void openExperimentalGroupAddDialog() {
    ExperimentId experimentId = context.experimentId().orElseThrow();
    List<ExperimentalVariable> variables = experimentInformationService.getVariablesOfExperiment(
        experimentId);
    List<VariableLevel> levels = variables.stream()
        .flatMap(variable -> variable.levels().stream())
        .toList();
    var dialog = ExperimentalGroupsDialog.empty(levels);
    dialog.addCancelEventListener(cancelEvent -> cancelEvent.getSource().close());
    dialog.addConfirmEventListener(this::onExperimentalGroupAddConfirmed);
    dialog.open();
  }

  private void onExperimentalGroupAddConfirmed(
      ConfirmEvent<ExperimentalGroupsDialog> confirmEvent) {
    ExperimentalGroupsDialog dialog = confirmEvent.getSource();
    addExperimentalGroups(dialog.experimentalGroups());
    reloadExperimentalGroups();
    dialog.close();
  }

  private void openExperimentalGroupEditDialog() {
    if (editGroupsNotAllowed()) {
      return;
    }
    ExperimentId experimentId = context.experimentId().orElseThrow();
    List<ExperimentalVariable> variables = experimentInformationService.getVariablesOfExperiment(
        experimentId);
    List<VariableLevel> levels = variables.stream()
        .flatMap(variable -> variable.levels().stream()).toList();
    var experimentalGroups = experimentInformationService.getExperimentalGroups(experimentId)
        .stream().map(this::toContent).toList();
    var dialog = ExperimentalGroupsDialog.prefilled(levels, experimentalGroups);
    dialog.addCancelEventListener(cancelEvent -> cancelEvent.getSource().close());
    dialog.addConfirmEventListener(this::onExperimentalGroupEditConfirmed);
    dialog.open();
  }

  private void onExperimentalGroupEditConfirmed(
      ConfirmEvent<ExperimentalGroupsDialog> confirmEvent) {
    editExperimentalGroups(confirmEvent.getSource().experimentalGroups());
    reloadExperimentalGroups();
    confirmEvent.getSource().close();
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

  private void showSampleRegistrationPossibleNotification() {
    String projectId = this.context.projectId().map(ProjectId::value).orElseThrow();
    Notification notification = createSampleRegistrationPossibleNotification(projectId);
    notification.open();
  }


  private void reloadExperimentalGroups() {
    loadExperimentalGroups();
    if (hasExperimentalGroups()) {
      onGroupsDefined();
      showSampleRegistrationPossibleNotification();
    } else {
      onNoGroupsDefined();
    }
  }

  private boolean hasExperimentalGroups() {
    return this.experimentalGroupCount > 0;
  }

  private void loadExperimentalGroups() {
    // We load the experimental groups of the experiment and render them as cards
    List<ExperimentalGroup> experimentalGroups = experimentInformationService.experimentalGroupsFor(
        context.experimentId().orElseThrow());
    List<ExperimentalGroupCard> experimentalGroupsCards = experimentalGroups.stream()
        .map(ExperimentalGroupCard::new).toList();
    experimentalGroupsCollection.setContent(experimentalGroupsCards);
    this.experimentalGroupCount = experimentalGroupsCards.size();
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
    reloadExperimentInfo(experimentId);
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
    // We load the experimental variables of the experiment and render them as cards
    List<ExperimentalVariable> variables = experiment.variables();
    List<ExperimentalVariableCard> experimentalVariableCards = variables.stream()
        .map(ExperimentalVariableCard::new).toList();
    experimentalVariableCollection.setContent(experimentalVariableCards);

    if (variables.isEmpty()) {
      this.experimentalVariables.add(addExperimentalVariablesNote);
    } else {
      this.experimentalVariables.add(experimentalVariableCollection);
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

  public static class ExperimentNameChangedEvent extends
      ComponentEvent<ExperimentDetailsComponent> {

    private final ExperimentId experimentId;
    public final String oldValue;
    public final String newValue;

    public ExperimentNameChangedEvent(ExperimentId experimentId, String oldValue, String newValue,
        ExperimentDetailsComponent source, boolean fromClient) {
      super(source, fromClient);
      this.oldValue = oldValue;
      this.newValue = newValue;
      this.experimentId = experimentId;
    }
    public ExperimentId experimentId() {
      return experimentId;
    }
  }
}
