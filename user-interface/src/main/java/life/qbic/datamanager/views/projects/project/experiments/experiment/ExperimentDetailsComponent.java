package life.qbic.datamanager.views.projects.project.experiments.experiment;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
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
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
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
import life.qbic.datamanager.views.projects.project.experiments.experiment.create.ExperimentAddDialog.ExperimentDraft;
import life.qbic.datamanager.views.projects.project.experiments.experiment.update.ExperimentUpdateDialog;
import life.qbic.datamanager.views.projects.project.experiments.experiment.update.ExperimentUpdateDialog.ExperimentUpdateEvent;
import life.qbic.datamanager.views.projects.project.samples.SampleInformationMain;
import life.qbic.projectmanagement.application.DeletionService;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ExperimentInformationService.ExperimentalGroupDTO;
import life.qbic.projectmanagement.application.ExperimentalDesignSearchService;
import life.qbic.projectmanagement.application.OntologyTermInformationService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalDesign;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalDesign.AddExperimentalGroupResponse.ResponseCode;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalVariable;
import life.qbic.projectmanagement.domain.model.experiment.VariableLevel;
import life.qbic.projectmanagement.domain.model.project.Project;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <b>Experimental Details Component</b>
 *
 * <p>A PageComponent based Composite showing the information stored in the
 * {@link ExperimentalDesign} associated with
 * a {@link Project} within the {@link ExperimentInformationMain}
 */
@UIScope
@SpringComponent
public class ExperimentDetailsComponent extends PageArea {

  @Serial
  private static final long serialVersionUID = -8992991642015281245L;
  private final transient ExperimentInformationService experimentInformationService;
  private final SampleInformationService sampleInformationService;
  private final transient OntologyTermInformationService ontologyTermInformationService;
  private final Div content = new Div();
  private final Div header = new Div();
  private final Span title = new Span();
  private final Span buttonBar = new Span();
  private final Div tagCollection = new Div();
  private final Span sampleSourceComponent = new Span();
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
  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";


  public ExperimentDetailsComponent(
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired SampleInformationService sampleInformationService,
      @Autowired DeletionService deletionService,
      @Autowired OntologyTermInformationService ontologyTermInformationService) {
    this.experimentInformationService = Objects.requireNonNull(experimentInformationService);
    this.sampleInformationService = sampleInformationService;
    this.deletionService = Objects.requireNonNull(deletionService);
    this.ontologyTermInformationService = Objects.requireNonNull(ontologyTermInformationService);
    this.noExperimentalVariablesDefined = createNoVariableDisclaimer();
    this.noExperimentalGroupsDefined = createNoGroupsDisclaimer();
    this.addExperimentalVariablesNote = createNoVariableDisclaimer();
    this.addClassName("experiment-details-component");
    layoutComponent();
    configureComponent();
  }

  private Notification createSampleRegistrationPossibleNotification() {
    Notification notification = new Notification();

    RouteParam projectRouteParam = new RouteParam(PROJECT_ID_ROUTE_PARAMETER,
        context.projectId().orElseThrow().value());
    RouteParam experimentRouteParam = new RouteParam(EXPERIMENT_ID_ROUTE_PARAMETER,
        context.experimentId().orElseThrow().value());
    String samplesUrl = RouteConfiguration.forSessionScope().getUrl(SampleInformationMain.class,
        new RouteParameters(projectRouteParam, experimentRouteParam));
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
    addSampleSourceInformationComponent();
    layoutTabSheet();
  }

  private void configureComponent() {
    listenForExperimentCollectionComponentEvents();
    listenForExperimentalVariablesComponentEvents();
  }

  private void initButtonBar() {
    Button editButton = new Button("Edit");
    editButton.addClickListener(event -> onEditButtonClicked());
    buttonBar.add(editButton);
  }

  private void onEditButtonClicked() {
    ExperimentId experimentId = context.experimentId().orElseThrow();
    Optional<Experiment> optionalExperiment = experimentInformationService.find(experimentId);
    if (optionalExperiment.isEmpty()) {
      throw new ApplicationException(
          "Experiment information could not be retrieved from service");
    }
    optionalExperiment.ifPresent(experiment -> {
      ExperimentUpdateDialog experimentUpdateDialog = new ExperimentUpdateDialog(
          ontologyTermInformationService);

      ExperimentDraft experimentDraft = new ExperimentDraft();
      experimentDraft.setExperimentName(experiment.getName());
      experimentDraft.setSpecies(experiment.getSpecies());
      experimentDraft.setSpecimens(experiment.getSpecimens());
      experimentDraft.setAnalytes(experiment.getAnalytes());

      experimentUpdateDialog.setExperiment(experimentDraft);
      experimentUpdateDialog.setConfirmButtonLabel("Save");

      experimentUpdateDialog.addExperimentUpdateEventListener(this::onExperimentUpdateEvent);
      experimentUpdateDialog.addCancelListener(event -> event.getSource().close());
      experimentUpdateDialog.open();
    });
  }

  private void onExperimentUpdateEvent(ExperimentUpdateEvent event) {
    ExperimentId experimentId = context.experimentId().orElseThrow();

    ExperimentDraft experimentDraft = event.getExperimentDraft();
    experimentInformationService.editExperimentInformation(experimentId,
        experimentDraft.getExperimentName(),
        experimentDraft.getSpecies(),
        experimentDraft.getSpecimens(),
        experimentDraft.getAnalytes());
    reloadExperimentInfo(experimentId);
    event.getSource().close();
  }


  private void listenForExperimentalVariablesComponentEvents() {
    experimentalVariableCollection.addAddListener(addEvent -> openExperimentalVariablesAddDialog());
    experimentalVariableCollection.addEditListener(editEvent -> openExperimentalVariablesEditDialog());
  }

  private void deleteExistingExperimentalVariables() {
    ExperimentId experimentId = context.experimentId().orElseThrow();
    var result = deletionService.deleteAllExperimentalVariables(experimentId);
    result.onError(responseCode -> {
      throw new ApplicationException("variable deletion failed: " + responseCode);
    });
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

  private void addSampleSourceInformationComponent() {
    sampleSourceComponent.addClassName("sample-source-display");

    content.add(sampleSourceComponent);
  }

  private Span createSampleSourceList(String title, Icon icon, List<String> tags) {
    Span iconAndList = new Span();
    iconAndList.addClassName("icon-with-list");
    iconAndList.add(icon);

    Div list = new Div();
    Span listTitle = new Span();
    listTitle.setText(title);
    listTitle.addClassName("title");
    list.add(listTitle);
    list.addClassName("taglist");
    tags.forEach(name -> list.add(new Span(name)));
    iconAndList.add(list);
    return iconAndList;
  }

  private void loadSampleSources(Experiment experiment) {
    sampleSourceComponent.removeAll();
    List<String> speciesTags = new ArrayList<>();
    List<String> specimenTags = new ArrayList<>();
    List<String> analyteTags = new ArrayList<>();
    experiment.getSpecies().forEach(species -> speciesTags.add(species.value()));
    experiment.getSpecimens().forEach(specimen -> specimenTags.add(specimen.value()));
    experiment.getAnalytes().forEach(analyte -> analyteTags.add(analyte.value()));

    sampleSourceComponent.add(
        createSampleSourceList("Species", VaadinIcon.BUG.create(), speciesTags));
    sampleSourceComponent.add(
        createSampleSourceList("Specimen", VaadinIcon.DROP.create(), specimenTags));
    sampleSourceComponent.add(
        createSampleSourceList("Analytes", VaadinIcon.CLUSTER.create(), analyteTags));
  }

  private void layoutTabSheet() {
    experimentSheet.add("Experimental Variables", experimentalVariables);
    experimentalVariables.addClassName("experimental-groups-container");
    experimentSheet.add("Experimental Groups", experimentalGroups);
    experimentalGroups.addClassName("experimental-groups-container");
    content.add(experimentSheet);
  }

  private void listenForExperimentCollectionComponentEvents() {
    experimentalGroupsCollection.addAddListener(listener -> openExperimentalGroupAddDialog());
    experimentalGroupsCollection.addEditListener(editEvent -> openExperimentalGroupEditDialog());
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
    if(result.isError()) {
      if (result.getError().equals(ResponseCode.CONDITION_EXISTS)) {
        throw new ApplicationException("Duplicate experimental group was selected", ErrorCode.DUPLICATE_GROUP_SELECTED,
            ErrorParameters.empty());
      }
      if (result.getError().equals(ResponseCode.EMPTY_VARIABLE)) {
        throw new ApplicationException("No experimental variable was selected", ErrorCode.NO_CONDITION_SELECTED,
            ErrorParameters.empty());
      } else {
        throw new ApplicationException(
            "Could not save one or more experimental groups %s %nReason: %s".formatted(
                Arrays.toString(
                    experimentalGroupContents.toArray()), result.getError()));
      }
    }
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
    Notification notification = createSampleRegistrationPossibleNotification();
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
    loadTagInformation();
    loadSampleSources(experiment);
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

  private void loadTagInformation() {
    tagCollection.removeAll();
    tagCollection.add(new Tag("Space for tags"));
  }

  private void loadExperimentalVariables(Experiment experiment) {
    this.experimentalVariables.removeAll();
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

}
