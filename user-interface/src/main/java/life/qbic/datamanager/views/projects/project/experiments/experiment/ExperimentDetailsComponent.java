package life.qbic.datamanager.views.projects.project.experiments.experiment;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static life.qbic.datamanager.views.projects.project.experiments.experiment.SampleOriginType.ANALYTE;
import static life.qbic.datamanager.views.projects.project.experiments.experiment.SampleOriginType.SPECIES;
import static life.qbic.datamanager.views.projects.project.experiments.experiment.SampleOriginType.SPECIMEN;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Card;
import life.qbic.datamanager.views.general.ConfirmEvent;
import life.qbic.datamanager.views.general.Disclaimer;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.confounding.ConfoundingVariable;
import life.qbic.datamanager.views.general.confounding.ConfoundingVariablesUserInput;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogAction;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.datamanager.views.general.icon.IconFactory;
import life.qbic.datamanager.views.notifications.CancelConfirmationDialogFactory;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.CardCollection;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalGroupsDialog;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalGroupsDialog.ExperimentalGroupContent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.ExperimentalVariablesInput;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableAdded;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableDeleted;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableLevelsChanged;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableRenamed;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableUnitChanged;
import life.qbic.datamanager.views.projects.project.experiments.experiment.update.EditExperimentDialog;
import life.qbic.datamanager.views.projects.project.experiments.experiment.update.EditExperimentDialog.ExperimentDraft;
import life.qbic.datamanager.views.projects.project.experiments.experiment.update.EditExperimentDialog.ExperimentUpdateEvent;
import life.qbic.datamanager.views.projects.project.samples.SampleInformationMain;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ExperimentalGroupCreationRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ExperimentalGroupDeletionRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ExperimentalGroupDeletionResponse;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ExperimentalGroupUpdateRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ExperimentalGroupUpdateResponse;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ExperimentalVariable;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableInformation;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ExperimentReference;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService.ExperimentalVariableInformation;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService.UsedVariableLevel;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalDesign;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <b>Experimental Details Component</b>
 *
 * <p>A PageComponent based Composite showing the information stored in the
 * {@link ExperimentalDesign} associated with a {@link Project} within the
 * {@link ExperimentInformationMain}
 */
@UIScope
@SpringComponent
public class ExperimentDetailsComponent extends PageArea {

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  private static final Logger log = LoggerFactory.logger(ExperimentDetailsComponent.class);
  @Serial
  private static final long serialVersionUID = -8992991642015281245L;
  private final transient ExperimentInformationService experimentInformationService;
  private final transient SampleInformationService sampleInformationService;
  private final transient SpeciesLookupService ontologyTermInformationService;
  private final Div content = new Div();
  private final Div header = new Div();
  private final Span title = new Span();
  private final Span buttonBar = new Span();
  private final Span sampleSourceComponent = new Span();
  private final TabSheet experimentSheet = new TabSheet();
  private final Div experimentalGroupsContainer = new Div();
  private final Div experimentalVariablesContainer = new Div();
  private final Div confoundingVariablesContainer;
  private final CardCollection experimentalGroupsCollection = new CardCollection("GROUPS");
  private final CardCollection experimentalVariableCollection = new CardCollection("VARIABLES");
  private final CardCollection confoundingVariableCollection = new CardCollection(
      "CONFOUNDING VARIABLES");
  private final Disclaimer noExperimentalVariablesDefined;
  private final Disclaimer noExperimentalGroupsDefined;
  private final Disclaimer noConfoundingVariablesDefined;
  private final transient TerminologyService terminologyService;
  private final transient MessageSourceNotificationFactory messageSourceNotificationFactory;
  private final transient CancelConfirmationDialogFactory cancelConfirmationDialogFactory;
  private final transient ConfoundingVariableService confoundingVariableService;
  private final transient AsyncProjectService asyncProjectService;
  private Context context;
  private int experimentalGroupCount;

  @Autowired
  public ExperimentDetailsComponent(
      ExperimentInformationService experimentInformationService,
      SampleInformationService sampleInformationService,
      SpeciesLookupService ontologyTermInformationService,
      TerminologyService terminologyService,
      MessageSourceNotificationFactory messageSourceNotificationFactory,
      CancelConfirmationDialogFactory cancelConfirmationDialogFactory,
      ConfoundingVariableService confoundingVariableService,
      AsyncProjectService asyncProjectService) {
    this.confoundingVariableService = requireNonNull(confoundingVariableService);
    this.messageSourceNotificationFactory = requireNonNull(messageSourceNotificationFactory,
        "messageSourceNotificationFactory must not be null");
    this.experimentInformationService = requireNonNull(experimentInformationService);
    this.sampleInformationService = sampleInformationService;
    this.ontologyTermInformationService = requireNonNull(ontologyTermInformationService);
    this.noExperimentalVariablesDefined = createNoVariableDisclaimer();
    this.noExperimentalGroupsDefined = createNoGroupsDisclaimer();
    this.noConfoundingVariablesDefined = createNoConfoundingVariablesDisclaimer();
    this.terminologyService = terminologyService;
    this.cancelConfirmationDialogFactory = requireNonNull(cancelConfirmationDialogFactory);
    this.addClassName("experiment-details-component");
    confoundingVariablesContainer = new Div();
    confoundingVariablesContainer.addClassNames("width-full", "height-full");
    // we use `edit` for both
    experimentalVariableCollection.setEditEnabled(true);
    experimentalVariableCollection.setAddEnabled(false);

    layoutComponent();
    configureComponent();
    this.asyncProjectService = asyncProjectService;
  }


  private static ComponentRenderer<Span, OntologyTerm> createOntologyRenderer() {
    return new ComponentRenderer<>(ontologyClassDTO -> {
      Span ontology = new Span();
      Span ontologyLabel = new Span(ontologyClassDTO.getLabel());
      /*Ontology terms are delimited by a column, the underscore is only used in the web environment*/
      String ontologyLinkName = ontologyClassDTO.oboId().toString();
      Span ontologyLink = new Span(ontologyLinkName);
      ontologyLink.addClassName("ontology-link");
      Anchor ontologyClassIri = new Anchor(ontologyClassDTO.getClassIri(), ontologyLink);
      ontologyClassIri.setTarget(AnchorTarget.BLANK);
      ontology.add(ontologyLabel, ontologyClassIri);
      ontology.addClassName("ontology");
      return ontology;
    });
  }

  private static AppDialog createConfoundingVarsDeleteConfirmDialog(
      List<ConfoundingVariable> deletedVars, DialogAction onConfirmAction) {
    var confirmDialog = AppDialog.small();
    DialogHeader.withIcon(confirmDialog,
        "Delete confounding variables?",
        IconFactory.warningIcon());
    String deletedVariableNames = deletedVars.stream().map(ConfoundingVariable::name)
        .collect(Collectors.joining(", "));
    DialogBody.withoutUserInput(confirmDialog, new Div(
        "Deleting a confounding variable will delete all levels of the confounding variable from annotated samples. "
            + "Do you want to delete the following confounding variables: " + deletedVariableNames
            + " ?"));
    DialogFooter.with(confirmDialog, "Continue editing",
        "Delete " + deletedVars.size() + " confounding variables");
    confirmDialog.registerConfirmAction(onConfirmAction);
    confirmDialog.registerCancelAction(confirmDialog::close);
    return confirmDialog;
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    reloadExperimentInfo(context.projectId().orElseThrow(), context.experimentId().orElseThrow());
  }

  private Notification createSampleRegistrationPossibleNotification() {

    RouteParam projectRouteParam = new RouteParam(PROJECT_ID_ROUTE_PARAMETER,
        context.projectId().orElseThrow().value());
    RouteParam experimentRouteParam = new RouteParam(EXPERIMENT_ID_ROUTE_PARAMETER,
        context.experimentId().orElseThrow().value());

    return messageSourceNotificationFactory.routingToast("from.experiment.to.sample.batch",
        new Object[]{},
        new Object[]{},
        SampleInformationMain.class,
        new RouteParameters(projectRouteParam, experimentRouteParam),
        getLocale());
  }

  private Disclaimer createNoVariableDisclaimer() {
    var disclaimer = Disclaimer.createWithTitle("Design your experiment",
        "Get started by adding experimental variables", "Add variables");
    disclaimer.addDisclaimerConfirmedListener(
        confirmedEvent -> openExperimentalVariablesAddDialog());
    return disclaimer;
  }

  private Disclaimer createNoConfoundingVariablesDisclaimer() {
    var disclaimer = Disclaimer.createWithTitle("Define Confounding Variables",
        "Add confounding variables to your experiment to use them during sample registration",
        "Add confounding variables");
    disclaimer.addDisclaimerConfirmedListener(confirmed -> openConfoundingVariablesAddDialog());
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
    addSampleSourceInformationComponent();
    layoutTabSheet();
  }

  private void configureComponent() {
    listenForExperimentCollectionComponentEvents();
    listenForExperimentalVariablesComponentEvents();
    listenForConfoundingVariablesComponentEvents();
  }

  private void initButtonBar() {
    Button editButton = new Button("Edit");
    editButton.addClickListener(event -> onEditButtonClicked());
    buttonBar.add(editButton);
  }

  private void onEditButtonClicked() {
    ExperimentId experimentId = context.experimentId().orElseThrow();
    Optional<Experiment> optionalExperiment = experimentInformationService.find(
        context.projectId().orElseThrow().value(), experimentId);
    if (optionalExperiment.isEmpty()) {
      throw new ApplicationException(
          "Experiment information could not be retrieved from service");
    }

    Map<SampleOriginType, Set<OntologyTerm>> usedTerms = getOntologyTermsUsedInSamples(
        experimentId);

    optionalExperiment.ifPresent(experiment -> {
      EditExperimentDialog editExperimentDialog = new EditExperimentDialog(
          ontologyTermInformationService, terminologyService);

      ExperimentDraft experimentDraft = new ExperimentDraft();
      experimentDraft.setExperimentName(experiment.getName());
      experimentDraft.setSpecies(experiment.getSpecies());
      experimentDraft.setSpecimens(experiment.getSpecimens());
      experimentDraft.setAnalytes(experiment.getAnalytes());

      editExperimentDialog.setExperiment(experimentDraft, usedTerms);
      editExperimentDialog.setConfirmButtonLabel("Save");

      editExperimentDialog.addExperimentUpdateEventListener(this::onExperimentUpdateEvent);
      editExperimentDialog.addCancelListener(
          cancelEvent -> showCancelConfirmationDialog(editExperimentDialog));
      editExperimentDialog.setEscAction(
          () -> showCancelConfirmationDialog(editExperimentDialog));
      editExperimentDialog.open();
    });
  }

  private void showCancelConfirmationDialog(EditExperimentDialog editExperimentDialog) {
    cancelConfirmationDialogFactory.cancelConfirmationDialog(
            it -> editExperimentDialog.close(),
            "experiment.edit", getLocale())
        .open();
  }

  private Map<SampleOriginType, Set<OntologyTerm>> getOntologyTermsUsedInSamples(
      ExperimentId experimentId) {
    Map<SampleOriginType, Set<OntologyTerm>> result = new EnumMap<>(SampleOriginType.class);
    var projectId = context.projectId().orElseThrow();
    Collection<Sample> samples;
    try {
      samples = sampleInformationService.retrieveSamplesForExperiment(projectId,
          experimentId.value());
    } catch (RuntimeException e) {
      log.debug("Could not load sample ontology terms", e);
      samples = new ArrayList<>();
    }

    Set<OntologyTerm> speciesSet = samples.stream()
        .map(sample -> sample.sampleOrigin().getSpecies())
        .collect(Collectors.toSet());
    result.put(SPECIES, speciesSet);

    Set<OntologyTerm> specimenSet = samples.stream()
        .map(sample -> sample.sampleOrigin().getSpecimen())
        .collect(Collectors.toSet());
    result.put(SPECIMEN, specimenSet);

    Set<OntologyTerm> analyteSet = samples.stream()
        .map(sample -> sample.sampleOrigin().getAnalyte())
        .collect(Collectors.toSet());
    result.put(ANALYTE, analyteSet);

    return result;
  }

  private void onExperimentUpdateEvent(ExperimentUpdateEvent event) {
    ExperimentId experimentId = context.experimentId().orElseThrow();
    ProjectId projectId = context.projectId().orElseThrow();
    ExperimentDraft experimentDraft = event.getExperimentDraft();
    experimentInformationService.editExperimentInformation(
        context.projectId().orElseThrow().value(),
        experimentId,
        experimentDraft.getExperimentName(),
        experimentDraft.getSpecies(),
        experimentDraft.getSpecimens(),
        experimentDraft.getAnalytes());
    reloadExperimentInfo(projectId, experimentId);
    event.getSource().close();
  }

  private void listenForExperimentalVariablesComponentEvents() {
    experimentalVariableCollection.addAddListener(
        addEvent -> openExperimentalVariablesAddDialog());
    experimentalVariableCollection.addEditListener(
        editEvent -> openExperimentalVariablesEditDialog());
  }

  private void listenForConfoundingVariablesComponentEvents() {
    confoundingVariableCollection.addAddListener(addEvent -> openConfoundingVariablesAddDialog());
    confoundingVariableCollection.addEditListener(
        editEvent -> openConfoundingVariablesEditDialog());
  }

  private void openConfoundingVariablesEditDialog() {
    var editDialog = AppDialog.small();

    ConfoundingVariablesUserInput confoundingVariablesUserInput = new ConfoundingVariablesUserInput();

    DialogHeader.with(editDialog, "Edit Confounding Variables");
    DialogBody.with(editDialog, confoundingVariablesUserInput, confoundingVariablesUserInput);
    DialogFooter.with(editDialog, "Cancel", "Confirm");

    List<ConfoundingVariable> confoundingVariableInformation = loadConfoundingVariables(
        context.projectId().map(ProjectId::value).orElseThrow(),
        context.experimentId().orElseThrow())
        .stream()
        .map(it -> new ConfoundingVariable(it.id(), it.variableName()))
        .toList();
    confoundingVariablesUserInput.setVariables(confoundingVariableInformation);
    editDialog.registerConfirmAction(
        () -> onConfoundingVariablesEditConfirmed(editDialog, confoundingVariablesUserInput,
            confoundingVariableInformation));
    editDialog.registerCancelAction(editDialog::close);
    editDialog.open();
  }

  private void onConfoundingVariablesEditConfirmed(AppDialog dialog,
      ConfoundingVariablesUserInput userInput,
      List<ConfoundingVariable> oldValue) {
    List<ConfoundingVariable> createdVars = userInput.values().stream()
        .filter(newVal -> isNull(newVal.variableReference())
            && oldValue.stream().noneMatch(old -> old.name().equals(newVal.name())))
        .toList();
    List<ConfoundingVariable> renamedVars = userInput.values().stream()
        .filter(newVal -> oldValue.stream().anyMatch(
            old -> old.variableReference().equals(newVal.variableReference()) && !old.name()
                .equals(newVal.name())))
        .toList();
    List<ConfoundingVariable> deletedVars = oldValue.stream()
        .filter(old -> userInput.values().stream()
            .noneMatch(
                newVal -> old.variableReference().equals(newVal.variableReference())
                    // the variable was removed
                    || old.name()
                    .equals(newVal.name()))) // no new variable was added with the same name
        .toList();
    DialogAction editingAction = () -> {
      editConfoundingVariables(createdVars, renamedVars, deletedVars);
      reloadExperimentInfo(context.projectId().orElseThrow(), context.experimentId().orElseThrow());
      dialog.close();
    };
    if (!deletedVars.isEmpty()) {
      askToConfirmConfoundingVariableDeletion(deletedVars, editingAction);
    } else {
      editingAction.execute();
    }

  }

  private void askToConfirmConfoundingVariableDeletion(List<ConfoundingVariable> deletedVars,
      DialogAction confirmAction) {
    AppDialog confirmDialog = createConfoundingVarsDeleteConfirmDialog(
        deletedVars, confirmAction);
    confirmDialog.open();
  }

  private void editConfoundingVariables(List<ConfoundingVariable> createdVars,
      List<ConfoundingVariable> renamedVars, List<ConfoundingVariable> deletedVars) {
    var projectId = context.projectId().orElseThrow();
    var experimentId = context.experimentId().orElseThrow();

    /*
     * Please be aware that deleting and re-adding a confounding variable with the same name does not lead to deletion and re-creation of a confounding variable.
     * For example if a user deletes a confounding variable "Test" in the UI and then re-adds a variable "Test" later on this is considered no change at all.
     */

    for (ConfoundingVariable deletedVar : deletedVars) {
      confoundingVariableService.deleteConfoundingVariable(projectId.value(),
          new ExperimentReference(experimentId.value()), deletedVar.variableReference());
    }
    for (ConfoundingVariable createdVar : createdVars) {
      confoundingVariableService.createConfoundingVariable(projectId.value(),
          new ExperimentReference(experimentId.value()), createdVar.name());
    }
    for (ConfoundingVariable renamedVar : renamedVars) {
      confoundingVariableService.renameConfoundingVariable(projectId.value(),
          new ExperimentReference(
              experimentId.value()), renamedVar.variableReference(), renamedVar.name());
    }
  }

  private void openConfoundingVariablesAddDialog() {

    var addDialog = AppDialog.small();

    ConfoundingVariablesUserInput confoundingVariablesUserInput = new ConfoundingVariablesUserInput();

    Set<String> namesAlreadyTaken = loadConfoundingVariables(
        context.projectId().map(ProjectId::value).orElseThrow(),
        context.experimentId().orElseThrow())
        .stream()
        .map(ConfoundingVariableInformation::variableName)
        .collect(Collectors.toSet());
    confoundingVariablesUserInput.setForbiddenNames(namesAlreadyTaken);

    DialogHeader.with(addDialog, "Add Confounding Variables");
    DialogBody.with(addDialog, confoundingVariablesUserInput, confoundingVariablesUserInput);
    DialogFooter.with(addDialog, "Cancel", "Confirm");

    addDialog.registerConfirmAction(
        () -> onConfoundingVariablesAddConfirmed(addDialog, confoundingVariablesUserInput));
    addDialog.registerCancelAction(addDialog::close);

    addDialog.open();
  }

  private void onConfoundingVariablesAddConfirmed(AppDialog dialog,
      ConfoundingVariablesUserInput userInput) {
    addConfoundingVariables(userInput.values());
    reloadExperimentInfo(context.projectId().orElseThrow(), context.experimentId().orElseThrow());
    dialog.close();
  }

  private void addConfoundingVariables(List<ConfoundingVariable> values) {
    var projectId = context.projectId().orElseThrow();
    var experimentId = context.experimentId().orElseThrow();
    for (ConfoundingVariable value : values) {
      confoundingVariableService.createConfoundingVariable(projectId.value(),
          new ExperimentReference(experimentId.value()),
          value.name());
    }
  }

  private void reloadExperimentInfo(ProjectId projectId, ExperimentId experimentId) {
    loadExperimentInformation(projectId, experimentId);
    reloadExperimentalVariables();
    reloadExperimentalGroups();
  }

  private void openExperimentalVariablesEditDialog() {
    ExperimentId experimentId = context.experimentId().orElseThrow();
    ExperimentalVariablesInput variablesInput = new ExperimentalVariablesInput();
    var usedLevelsByVariable = experimentInformationService.getUsedVariableLevels(
            context.projectId().orElseThrow().value(), experimentId).stream()
        .collect(Collectors.groupingBy(UsedVariableLevel::variableName));

    List<ExperimentalVariableInformation> variablesOfExperiment = experimentInformationService.getVariablesOfExperiment(
        context.projectId().orElseThrow().value(), experimentId);
    var variables = variablesOfExperiment.stream()
        .map(it -> new ExperimentalVariablesInput.ExperimentalVariableInformation(
            it.name(),
            it.unit(),
            it.levels()
        ))
        .sorted(
            Comparator.comparing(ExperimentalVariablesInput.ExperimentalVariableInformation::name))
        .toList();
    variables.forEach(variablesInput::addVariable);
    if (variables.isEmpty()) {
      variablesInput.addVariable();
    }

    for (Entry<String, List<UsedVariableLevel>> entry : usedLevelsByVariable.entrySet()) {
      Set<String> usedValues = entry
          .getValue().stream()
          .map(UsedVariableLevel::value)
          .collect(Collectors.toSet());
      String variableName = entry.getKey();
      variablesInput.setUsedLevels(variableName, usedValues);
    }
    if (!usedLevelsByVariable.isEmpty()) {
      variablesInput.setAddVariablesEnabled(false);
    }
    variablesInput.markInitialized();
    AppDialog dialog = AppDialog.medium();
    DialogHeader.with(dialog, "Define Experiment Variables");
    DialogFooter.with(dialog, "Cancel", "Save");
    DialogBody.with(dialog, variablesInput, variablesInput);
    dialog.registerConfirmAction(onVariableEditDialogConfirmed(variablesInput, dialog));
    dialog.registerCancelAction(dialog::close);
    dialog.open();
  }

  private DialogAction onVariableEditDialogConfirmed(ExperimentalVariablesInput variablesInput,
      AppDialog dialog) {
    return () -> {
      List<VariableChange> changes = variablesInput.getChanges();
      performEdit(dialog, changes);
    };
  }

  private void performEdit(AppDialog dialog, List<VariableChange> changes) {
    Map<String, List<VariableChange>> changesByVariable = changes.stream()
        .collect(Collectors.groupingBy(VariableChange::affectedVariable));
    changesByVariable.forEach(this::applyChangesToVariable);
    if (!changesByVariable.isEmpty()) {
      reloadExperimentalVariables();
    }
    dialog.close();
  }

  private void openExperimentalVariablesAddDialog() {
    openExperimentalVariablesEditDialog();
  }

  private void applyChangesToVariable(String variable, List<VariableChange> changes) {
    var projectId = context.projectId().orElseThrow();
    ExperimentId experimentId = context.experimentId().orElseThrow();

    //delete variable
    changes.stream()
        .filter(VariableDeleted.class::isInstance)
        .map(VariableDeleted.class::cast)
        .findAny()
        .ifPresent(
            change -> experimentInformationService.deleteExperimentalVariable(projectId,
                experimentId,
                change.name()));

    //change unit
    changes.stream()
        .filter(VariableUnitChanged.class::isInstance)
        .map(VariableUnitChanged.class::cast)
        .findAny()
        .ifPresent(
        change -> experimentInformationService.setVariableUnit(projectId.value(), experimentId,
            variable, change.newUnit()));

    //change levels
    changes.stream()
        .filter(VariableLevelsChanged.class::isInstance)
        .map(VariableLevelsChanged.class::cast)
        .findAny()
        .ifPresent(change -> experimentInformationService.setVariableLevels(projectId.value(),
            experimentId, change.name(), change.levels()));


    //add variable
    changes.stream()
        .filter(VariableAdded.class::isInstance)
        .map(VariableAdded.class::cast)
        .findAny()
        .ifPresent(change ->
            experimentInformationService.addVariableToExperiment(projectId.value(),
                experimentId,
                change.name(),
                change.unit(),
                change.levels()));

    //rename variable
    changes.stream()
        .filter(VariableRenamed.class::isInstance)
        .map(VariableRenamed.class::cast)
        .findAny()
        .ifPresent(change ->
            experimentInformationService.renameExperimentalVariable(projectId.value(),
                experimentId,
                change.oldName(),
                change.newName()));
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

  private void addSampleSourceInformationComponent() {
    sampleSourceComponent.addClassName("sample-source-display");
    content.add(sampleSourceComponent);
  }

  private Div createSampleSourceList(String titleText,
      List<OntologyTerm> ontologyClasses) {
    Div sampleSource = new Div();
    sampleSource.addClassName("sample-source");
    Span title = new Span(titleText);
    Span header = new Span(title);
    header.addClassName("header");
    Div ontologies = new Div();
    ontologies.addClassName("ontologies");
    ontologyClasses.forEach(ontologyClassDTO -> ontologies.add(
        createOntologyRenderer().createComponent(ontologyClassDTO)));
    sampleSource.add(header, ontologies);
    return sampleSource;
  }

  private void loadSampleSources(Experiment experiment) {
    sampleSourceComponent.removeAll();
    List<OntologyTerm> speciesTags = new ArrayList<>(experiment.getSpecies());
    List<OntologyTerm> specimenTags = new ArrayList<>(experiment.getSpecimens());
    List<OntologyTerm> analyteTags = new ArrayList<>(experiment.getAnalytes());

    sampleSourceComponent.add(
        createSampleSourceList("Species", speciesTags));
    sampleSourceComponent.add(
        createSampleSourceList("Specimen", specimenTags));
    sampleSourceComponent.add(
        createSampleSourceList("Analytes", analyteTags));
  }

  private void layoutTabSheet() {
    experimentSheet.addClassName("experimental-sheet");
    experimentSheet.add("Experimental Variables", experimentalVariablesContainer);
    experimentalVariablesContainer.addClassName("experimental-groups-container");
    experimentSheet.add("Experimental Groups", experimentalGroupsContainer);
    experimentalGroupsContainer.addClassName("experimental-groups-container");
    experimentSheet.add("Confounding Variables", confoundingVariablesContainer);
    confoundingVariablesContainer.add(confoundingVariableCollection);
    content.add(experimentSheet);
  }

  private void listenForExperimentCollectionComponentEvents() {
    experimentalGroupsCollection.addAddListener(listener -> openExperimentalGroupAddDialog());
    experimentalGroupsCollection.addEditListener(editEvent -> openExperimentalGroupEditDialog());
  }

  private void openExperimentalGroupAddDialog() {
    ExperimentId experimentId = context.experimentId().orElseThrow();
    List<ExperimentalVariableInformation> variables = experimentInformationService.getVariablesOfExperiment(
        context.projectId().orElseThrow().value(),
        experimentId);
    List<ExperimentalGroupInput.VariableLevel> levels = variables.stream()
        .flatMap(variable ->
            variable.levels().stream()
                .map(it -> new ExperimentalGroupInput.VariableLevel(variable.name(), it,
                    variable.unit())))
        .toList();
    var groups = experimentInformationService.experimentalGroupsFor(
            context.projectId().orElseThrow()
                .value(), experimentId)
        .stream().map(this::toContent).toList();

    ExperimentalGroupsDialog dialog;
    if (groups.isEmpty()) {
      dialog = ExperimentalGroupsDialog.empty(levels);
    } else {
      dialog = ExperimentalGroupsDialog.nonEditable(levels, groups);
    }
    dialog.addCancelEventListener(cancelEvent -> cancelEvent.getSource().close());
    dialog.addConfirmEventListener(this::onExperimentalGroupAddConfirmed);
    dialog.open();
  }

  private void onExperimentalGroupAddConfirmed(
      ConfirmEvent<ExperimentalGroupsDialog> confirmEvent) {
    ExperimentalGroupsDialog dialog = confirmEvent.getSource();
    if (dialog.isValid()) {
      var groupContents = dialog.experimentalGroups();
      addExperimentalGroups(groupContents.stream()
          // We don't want to add existing groups again. Since they
          // are already having a dedicated group number, we can just filter for the ones without any.
          .filter(experimentalGroupContent -> experimentalGroupContent.groupNumber() == -1)
          .toList());

      reloadExperimentalGroups();
      dialog.close();
    }
  }

  private void openExperimentalGroupEditDialog() {
    if (editGroupsNotAllowed()) {
      return;
    }
    ExperimentId experimentId = context.experimentId().orElseThrow();
    List<ExperimentalVariableInformation> variables = experimentInformationService.getVariablesOfExperiment(
        context.projectId().orElseThrow().value(),
        experimentId);
    List<ExperimentalGroupInput.VariableLevel> levels = variables.stream()
        .flatMap(variable ->
            variable.levels().stream()
                .map(it -> new ExperimentalGroupInput.VariableLevel(variable.name(), it,
                    variable.unit())))
        .toList();

    var groups = experimentInformationService.experimentalGroupsFor(
            context.projectId().orElseThrow().value(), experimentId)
        .stream().map(this::toContent).toList();
    var dialog = ExperimentalGroupsDialog.editable(levels, groups);
    dialog.addCancelEventListener(cancelEvent -> cancelEvent.getSource().close());
    dialog.addConfirmEventListener(this::onExperimentalGroupEditConfirmed);
    dialog.open();
  }

  private void onExperimentalGroupEditConfirmed(
      ConfirmEvent<ExperimentalGroupsDialog> confirmEvent) {
    ExperimentalGroupsDialog dialog = confirmEvent.getSource();
    if (dialog.isValid()) {
      var groups = dialog.experimentalGroups();
      var groupsToCreate = groups.stream().filter(group -> group.id() == -1L).toList();
      var groupsToUpdate = groups.stream().filter(group -> group.id() != -1L).toList();
      var groupsToDelete = dialog.groupsToDelete();
      addExperimentalGroups(groupsToCreate);
      updateExperimentalGroups(groupsToUpdate);
      deleteExperimentalGroups(groupsToDelete);
      confirmEvent.getSource().close();
    }
  }

  private void deleteExperimentalGroups(List<Integer> groupsToDelete) {
    if (groupsToDelete.isEmpty()) {
      return;
    }
    var projectId = context.projectId().orElseThrow();
    var experimentId = context.experimentId().orElseThrow();
    var requests = new ArrayList<Mono<ExperimentalGroupDeletionResponse>>();

    for (Integer groupNumber : groupsToDelete) {
      requests.add(asyncProjectService.delete(
          new ExperimentalGroupDeletionRequest(projectId.value(), experimentId.value(),
              groupNumber)));
    }

    Mono.when(requests)
        .doOnSuccess(s -> {
          displaySuccessfulDeletionNotification();
          reloadExperimentalGroups();
        })
        .doOnError(e -> {
          log.error("Failed to delete experimental groups", e);
          displayFailedExperimentalGroupDeletion();
        })
        .subscribe();
  }

  private void displayFailedExperimentalGroupDeletion() {
    getUI().ifPresent(ui -> ui.access(
        () -> messageSourceNotificationFactory.toast("experimental.groups.deleted.failed",
            new Object[]{},
            getLocale()).open()));
  }

  private void displaySuccessfulDeletionNotification() {
    getUI().ifPresent(ui -> ui.access(
        () -> messageSourceNotificationFactory.toast("experimental.groups.deleted.success",
            new Object[]{},
            getLocale()).open()));
  }

  private void updateExperimentalGroups(List<ExperimentalGroupContent> groupsToUpdate) {
    if (groupsToUpdate.isEmpty()) {
      return;
    }
    var experimentalGroups = groupsToUpdate.stream().map(this::toApi).toList();
    ExperimentId experimentId = context.experimentId().orElseThrow();
    var projectId = context.projectId().orElseThrow();

    var serviceCalls = new ArrayList<Mono<ExperimentalGroupUpdateResponse>>();

    experimentalGroups.forEach(experimentalGroup -> serviceCalls.add(
        asyncProjectService.update(new ExperimentalGroupUpdateRequest(projectId.value(),
            experimentId.value(), experimentalGroup))));

    Mono.when(serviceCalls).doOnSuccess(s -> {
          displaySuccessfulExperimentalGroupUpdate();
          reloadExperimentalGroups();
        }).doOnError(e -> {
          log.error("Error while updating experimental group", e);
          displayFailedExperimentalGroupUpdate();
        })
        .doOnSuccess(it -> log.debug("Updated experimental groups for project" + projectId))
        .subscribe();
  }

  private void displayFailedExperimentalGroupUpdate() {
    getUI().ifPresent(ui -> ui.access(
        () -> messageSourceNotificationFactory.toast("experimental.groups.updated.failed",
            new Object[]{},
            getLocale()).open()));
  }

  private void displaySuccessfulExperimentalGroupUpdate() {
    getUI().ifPresent(ui -> ui.access(
        () -> messageSourceNotificationFactory.toast("experimental.groups.updated.success",
            new Object[]{},
            getLocale()).open()));
  }

  private AsyncProjectService.ExperimentalGroup toApi(ExperimentalGroupContent experimentalGroup) {
    return new AsyncProjectService.ExperimentalGroup(experimentalGroup.id(),
        experimentalGroup.groupNumber(),
        experimentalGroup.name(), experimentalGroup.size(),
        experimentalGroup.variableLevels().stream()
            .map(it -> new AsyncProjectService.VariableLevel(it.variableName(), it.value(),
                it.unit()))
            .toList());
  }

  private void addExperimentalGroups(
      Collection<ExperimentalGroupContent> experimentalGroupContents) {
    if (experimentalGroupContents.isEmpty()) {
      return;
    }
    var experimentalGroups = experimentalGroupContents.stream().map(this::toApi).toList();
    ExperimentId experimentId = context.experimentId().orElseThrow();
    var projectId = context.projectId().orElseThrow();

    Flux.fromIterable(experimentalGroups)
        .map(experimentalGroup -> new ExperimentalGroupCreationRequest(projectId.value(),
            experimentId.value(), experimentalGroup))
        .concatMap(asyncProjectService::create)
        .timeout(Duration.ofSeconds(15))
        .collectList()
        .subscribe(ignored -> {
          displaySuccessfulExperimentalGroupCreation();
          reloadExperimentalGroups();
          showSampleRegistrationPossibleNotification();
        }, err -> {
          log.error("Error while creating experimental group", err);
          displayFailedExperimentalGroupCreation();
        });
  }

  private void displayFailedExperimentalGroupCreation() {
    getUI().ifPresent(ui -> ui.access(
        () -> messageSourceNotificationFactory.toast("experimental.groups.created.failed",
            new Object[]{},
            getLocale()).open()));
  }

  private void displaySuccessfulExperimentalGroupCreation() {
    getUI().ifPresent(ui -> ui.access(
        () -> messageSourceNotificationFactory.toast("experimental.groups.created.success",
            new Object[]{},
            getLocale()).open()));
  }

  private ExperimentalGroupContent toContent(ExperimentalGroup experimentalGroup) {
    return new ExperimentalGroupContent(experimentalGroup.id(), experimentalGroup.groupNumber(),
        experimentalGroup.name(), experimentalGroup.sampleSize(),
        experimentalGroup.condition()
            .getVariableLevels().stream()
            .map(this::fromDomain)
            .toList());
  }

  private ExperimentalGroupInput.VariableLevel fromDomain(
      life.qbic.projectmanagement.domain.model.experiment.VariableLevel variableLevel) {
    return new ExperimentalGroupInput.VariableLevel(variableLevel.variableName().value(),
        variableLevel.experimentalValue().value(),
        variableLevel.experimentalValue().unit().orElse(null));
  }

  private void showSampleRegistrationPossibleNotification() {
    getUI().ifPresent(ui -> ui.access(() -> {
      Notification notification = createSampleRegistrationPossibleNotification();
      notification.open();
    }));
  }

  private void reloadExperimentalGroups() {
    getUI().ifPresent(ui -> ui.access(() -> {
      List<ExperimentalGroup> groups = loadExperimentalGroups();
      fillExperimentalGroupCollection(groups);
      if (hasExperimentalGroups()) {
        onGroupsDefined();
      } else {
        onNoGroupsDefined();
      }
    }));
  }

  private boolean hasExperimentalGroups() {
    return this.experimentalGroupCount > 0;
  }

  private List<ExperimentalGroup> loadExperimentalGroups() {
    // We load the experimental groups of the experiment and render them as cards
    return experimentInformationService.experimentalGroupsFor(
        context.projectId().orElseThrow().value(),
        context.experimentId().orElseThrow());
  }

  public void setContext(Context context) {
    this.context = requireNonNull(context);
    var projectId = context.projectId()
        .orElseThrow(() -> new ApplicationException("no project id in context " + context));
    var experimentId = context.experimentId()
        .orElseThrow(() -> new ApplicationException("no experiment id in context " + context));
    reloadExperimentInfo(projectId, experimentId);
  }

  private void loadExperimentInformation(ProjectId projectId, ExperimentId experimentId) {
    Experiment experiment = experimentInformationService.find(projectId.value(), experimentId)
        .orElseThrow();
    title.setText(experiment.getName());
    loadSampleSources(experiment);
    var confoundingVariables = loadConfoundingVariables(projectId.value(), experimentId);
    fillConfoundingVariablesCollection(confoundingVariables);
    if (confoundingVariables.isEmpty()) {
      onNoConfoundingDefined();
    } else {
      onConfoundingDefined();
    }
  }

  private void reloadExperimentalVariables() {
    String projectId = context.projectId().orElseThrow().value();
    var experimentId = context.experimentId().orElseThrow().value();
    getUI().ifPresentOrElse(
        ui -> asyncProjectService.getExperimentalVariables(projectId, experimentId)
            .doOnSubscribe(ignored -> ui.access(this::onVariablesLoading))
            .doOnSubscribe(ignored -> ui.access(experimentalVariableCollection::clear))
            .doOnSubscribe(ignored -> log.debug("Loading experimental variables.."))
            .doOnNext(ignored -> ui.access(this::onVariablesDefined))
            .doOnNext(it -> ui.access(() -> addExperimentalVariablesDisplay(it)))
            .switchIfEmpty(Mono.just(new ExperimentalVariable("", List.of()))
                .doOnSubscribe(ignored -> log.debug("No experimental variables found."))
                .doOnNext(ignored -> ui.access(this::onNoVariablesDefined)))
            .subscribe(),
        () -> {
          log.warn(
              "No ui present when reloading experimental variables. Element %s is attached = %s".formatted(
                  this, isAttached()));
          onNoVariablesDefined();
        }
    );
  }

  private void addExperimentalVariablesDisplay(
      AsyncProjectService.ExperimentalVariable experimentalVariable) {
    var card = new ExperimentalVariableCard(experimentalVariable);
    experimentalVariableCollection.add(card);
  }

  private void onVariablesDefined() {
    experimentalVariablesContainer.removeAll();
    experimentalVariablesContainer.add(experimentalVariableCollection);
  }

  private void fillExperimentalGroupCollection(List<ExperimentalGroup> groups) {
    Comparator<String> natOrder = Comparator.naturalOrder();
    List<ExperimentalGroupCard> experimentalGroupsCards = groups.stream()
        .sorted((g1, g2) -> natOrder.compare(g1.name(), g2.name()))
        .map(ExperimentalGroupCard::new)
        .toList();
    experimentalGroupsCollection.setContent(experimentalGroupsCards);
    this.experimentalGroupCount = experimentalGroupsCards.size();
  }

  private void fillConfoundingVariablesCollection(
      List<ConfoundingVariableInformation> confoundingVariables) {
    var cards = new ArrayList<Component>();
    for (ConfoundingVariableInformation confoundingVariable : confoundingVariables) {
      Card card = new Card();
      card.addClassNames("padding-horizontal-05", "padding-vertical-05");
      card.add(new Div(confoundingVariable.variableName()));
      cards.add(card);
    }
    confoundingVariableCollection.setContent(cards);
  }

  private List<ConfoundingVariableInformation> loadConfoundingVariables(String projectId,
      ExperimentId experimentId) {
    return confoundingVariableService.listConfoundingVariablesForExperiment(projectId,
        new ExperimentReference(experimentId.value()));
  }

  private void onNoVariablesDefined() {
    experimentalVariablesContainer.removeAll();
    experimentalVariablesContainer.add(noExperimentalVariablesDefined);
  }

  private void onVariablesLoading() {
    experimentalVariablesContainer.removeAll();
    var progress = new ProgressBar();
    progress.setIndeterminate(true);
    experimentalVariablesContainer.add(
        new Span("Loading information"),
        progress
    );

  }

  private void onNoGroupsDefined() {
    experimentalGroupsContainer.removeAll();
    experimentalGroupsContainer.add(noExperimentalGroupsDefined);
  }

  private void onGroupsDefined() {
    experimentalGroupsContainer.removeAll();
    experimentalGroupsContainer.add(experimentalGroupsCollection);
  }

  private void onNoConfoundingDefined() {
    confoundingVariablesContainer.removeAll();
    confoundingVariablesContainer.add(noConfoundingVariablesDefined);
  }

  private void onConfoundingDefined() {
    confoundingVariablesContainer.removeAll();
    confoundingVariablesContainer.add(confoundingVariableCollection);
  }

}
