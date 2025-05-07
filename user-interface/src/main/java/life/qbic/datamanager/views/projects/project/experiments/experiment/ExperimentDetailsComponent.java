package life.qbic.datamanager.views.projects.project.experiments.experiment;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static life.qbic.datamanager.views.projects.project.experiments.experiment.SampleOriginType.ANALYTE;
import static life.qbic.datamanager.views.projects.project.experiments.experiment.SampleOriginType.SPECIES;
import static life.qbic.datamanager.views.projects.project.experiments.experiment.SampleOriginType.SPECIMEN;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.AbstractIcon;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
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
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExistingGroupsPreventVariableEdit;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExistingSamplesPreventVariableEdit;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalGroupsDialog;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalGroupsDialog.ExperimentalGroupContent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalVariableContent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalVariablesDialog;
import life.qbic.datamanager.views.projects.project.experiments.experiment.update.EditExperimentDialog;
import life.qbic.datamanager.views.projects.project.experiments.experiment.update.EditExperimentDialog.ExperimentDraft;
import life.qbic.datamanager.views.projects.project.experiments.experiment.update.EditExperimentDialog.ExperimentUpdateEvent;
import life.qbic.datamanager.views.projects.project.samples.SampleInformationMain;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.DeletionService;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ExperimentDeletionRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ExperimentDeletionResponse;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ExperimentUpdateRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ExperimentalVariableAdditions;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ExperimentalVariableDeletions;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ExperimentalVariables;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableInformation;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ExperimentReference;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService.ExperimentalGroupDTO;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.OntologyTermV1;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalDesign;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalVariable;
import life.qbic.projectmanagement.domain.model.experiment.VariableLevel;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import org.springframework.beans.factory.annotation.Autowired;
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

  private static final Logger log = LoggerFactory.logger(ExperimentDetailsComponent.class);
  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
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
  private final transient DeletionService deletionService;
  private final transient TerminologyService terminologyService;
  private final transient MessageSourceNotificationFactory messageSourceNotificationFactory;
  private final transient CancelConfirmationDialogFactory cancelConfirmationDialogFactory;
  private final transient ConfoundingVariableService confoundingVariableService;
  private final AsyncProjectService asyncProjectService;
  private Context context;
  private int experimentalGroupCount;


  public ExperimentDetailsComponent(
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired SampleInformationService sampleInformationService,
      @Autowired DeletionService deletionService,
      @Autowired SpeciesLookupService ontologyTermInformationService,
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
    this.deletionService = requireNonNull(deletionService);
    this.ontologyTermInformationService = requireNonNull(ontologyTermInformationService);
    this.noExperimentalVariablesDefined = createNoVariableDisclaimer();
    this.noExperimentalGroupsDefined = createNoGroupsDisclaimer();
    this.noConfoundingVariablesDefined = createNoConfoundingVariablesDisclaimer();
    this.terminologyService = terminologyService;
    this.cancelConfirmationDialogFactory = requireNonNull(cancelConfirmationDialogFactory);
    this.addClassName("experiment-details-component");
    confoundingVariablesContainer = new Div();
    confoundingVariablesContainer.addClassNames("full-width", "full-height");
    layoutComponent();
    configureComponent();
    this.asyncProjectService = asyncProjectService;
  }


  private static ComponentRenderer<Span, OntologyTermV1> createOntologyRenderer() {
    return new ComponentRenderer<>(ontologyClassDTO -> {
      Span ontology = new Span();
      Span ontologyLabel = new Span(ontologyClassDTO.getLabel());
      /*Ontology terms are delimited by a column, the underscore is only used in the web environment*/
      String ontologyLinkName = ontologyClassDTO.getOboId().replace("_", ":");
      Span ontologyLink = new Span(ontologyLinkName);
      ontologyLink.addClassName("ontology-link");
      Anchor ontologyClassIri = new Anchor(ontologyClassDTO.getClassIri(), ontologyLink);
      ontologyClassIri.setTarget(AnchorTarget.BLANK);
      ontology.add(ontologyLabel, ontologyClassIri);
      ontology.addClassName("ontology");
      return ontology;
    });
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
    var disclaimer = Disclaimer.createWithTitle("Define Confoungind Variables",
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

    Map<SampleOriginType, Set<OntologyTermV1>> usedTerms = getOntologyTermsUsedInSamples(
        experimentId);

    optionalExperiment.ifPresent(experiment -> {
      EditExperimentDialog editExperimentDialog = new EditExperimentDialog(
          ontologyTermInformationService, terminologyService);

      ExperimentDraft experimentDraft = new ExperimentDraft();
      experimentDraft.setExperimentName(experiment.getName());
      experimentDraft.setSpecies(experiment.getSpecies());
      experimentDraft.setSpecimens(experiment.getSpecimens());
      experimentDraft.setAnalytes(experiment.getAnalytes());
      experimentDraft.setSpeciesIcon(BioIcon.getTypeWithNameOrDefault(SampleSourceType.SPECIES,
          experiment.getSpeciesIconName()));
      experimentDraft.setSpecimenIcon(BioIcon.getTypeWithNameOrDefault(SampleSourceType.SPECIMEN,
          experiment.getSpecimenIconName()));

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

  private Map<SampleOriginType, Set<OntologyTermV1>> getOntologyTermsUsedInSamples(
      ExperimentId experimentId) {
    Map<SampleOriginType, Set<OntologyTermV1>> result = new EnumMap<>(SampleOriginType.class);
    Collection<Sample> samples = sampleInformationService.retrieveSamplesForExperiment(experimentId)
        .valueOrElse(new ArrayList<>());

    Set<OntologyTermV1> speciesSet = samples.stream()
        .map(sample -> sample.sampleOrigin().getSpecies())
        .collect(Collectors.toSet());
    result.put(SPECIES, speciesSet);

    Set<OntologyTermV1> specimenSet = samples.stream()
        .map(sample -> sample.sampleOrigin().getSpecimen())
        .collect(Collectors.toSet());
    result.put(SPECIMEN, specimenSet);

    Set<OntologyTermV1> analyteSet = samples.stream()
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
    experimentalVariableCollection.addAddListener(addEvent -> openExperimentalVariablesAddDialog());
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

  private static AppDialog createConfoundingVarsDeleteConfirmDialog(
      List<ConfoundingVariable> deletedVars, DialogAction onConfirmAction) {
    var confirmDialog = AppDialog.small();
    life.qbic.datamanager.views.general.dialog.DialogHeader.withIcon(confirmDialog,
        "Delete confounding variables?",
        IconFactory.warningIcon());
    String deletedVariableNames = deletedVars.stream().map(ConfoundingVariable::name)
        .collect(Collectors.joining(", "));
    DialogBody.withoutUserInput(confirmDialog, new Div(
        "Deleting a confounding variable will delete all levels of the confounding variable from annotated samples. "
            + "Do you want to delete the following confounding variables: " + deletedVariableNames
            + " ?"));
    life.qbic.datamanager.views.general.dialog.DialogFooter.with(confirmDialog, "Continue editing",
        "Delete " + deletedVars.size() + " confounding variables");
    confirmDialog.registerConfirmAction(onConfirmAction);
    confirmDialog.registerCancelAction(confirmDialog::close);
    return confirmDialog;
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

  private Mono<ExperimentDeletionResponse> deleteExistingExperimentalVariables() {
    ExperimentId experimentId = context.experimentId().orElseThrow();
    ProjectId projectId = context.projectId().orElseThrow();

    List<AsyncProjectService.ExperimentalVariable> variablesOfExperiment = experimentInformationService.getVariablesOfExperiment(
            projectId.value(), experimentId).stream()
        .map(
            variable -> {
              Optional<String> unit = variable.levels().stream()
                  .map(l -> l.experimentalValue()
                      .unit())
                  .findAny().flatMap(Function.identity());
              return new AsyncProjectService.ExperimentalVariable(variable.name().value(),
                  new HashSet(variable.levels()), unit.orElse(null));
            })
        .toList();

    return asyncProjectService.delete(
        new ExperimentDeletionRequest(projectId.value(), experimentId.value(),
            new ExperimentalVariableDeletions(variablesOfExperiment)));
  }

  private void reloadExperimentInfo(ProjectId projectId, ExperimentId experimentId) {
    loadExperimentInformation(projectId, experimentId);
  }

  private void openExperimentalVariablesAddDialog() {
    if (editVariablesNotAllowed()) {
      return;
    }
    var addDialog = new ExperimentalVariablesDialog();
    addDialog.addCancelEventListener(cancelEvent -> showCancelConfirmationDialog(addDialog, true));
    addDialog.setEscAction(() -> showCancelConfirmationDialog(addDialog, true));
    addDialog.addConfirmEventListener(this::onExperimentalVariablesAddConfirmed);
    addDialog.open();
  }

  private void onExperimentalVariablesAddConfirmed(
      ExperimentalVariablesDialog.ConfirmEvent confirmEvent) {

    List<ExperimentalVariableContent> variableContents = confirmEvent.getSource()
        .definedVariables();
    List<AsyncProjectService.ExperimentalVariable> variables = variableContents.stream()
        .map(it -> new AsyncProjectService.ExperimentalVariable(it.name(),
            new HashSet<>(it.levels()), it.unit()))
        .toList();

    ExperimentalVariableAdditions body = new ExperimentalVariableAdditions(variables);
    ProjectId projectId = context.projectId().orElseThrow();
    ExperimentId experimentId = context.experimentId().orElseThrow();
    var ui = UI.getCurrent();

    ExperimentUpdateRequest request = new ExperimentUpdateRequest(
        projectId.value(),
        experimentId.value(), body);

    asyncProjectService.update(request)
        .doOnNext(it -> ui.access(() -> {
          confirmEvent.getSource().close();
          reloadExperimentInfo(projectId,
              experimentId);
          if (hasExperimentalGroups()) {
            showSampleRegistrationPossibleNotification();
          }
        }))
        .subscribe();
  }

  private void openExperimentalVariablesEditDialog() {
    if (editVariablesNotAllowed()) {
      return;
    }
    ExperimentId experimentId = context.experimentId().orElseThrow();
    var editDialog = ExperimentalVariablesDialog.prefilled(
        experimentInformationService.getVariablesOfExperiment(
            context.projectId().orElseThrow().value(), experimentId));
    editDialog.addCancelEventListener(
        cancelEvent -> showCancelConfirmationDialog(editDialog, false));
    editDialog.setEscAction(() -> showCancelConfirmationDialog(editDialog, false));
    editDialog.addConfirmEventListener(this::onExperimentalVariablesEditConfirmed);
    editDialog.open();
  }

  private void showCancelConfirmationDialog(ExperimentalVariablesDialog editDialog,
      boolean isCreate) {
    var key = isCreate ? "experiment.variables.create" : "experiment.variables.edit";
    cancelConfirmationDialogFactory.cancelConfirmationDialog(it -> editDialog.close(),
            key, getLocale())
        .open();
  }

  private void onExperimentalVariablesEditConfirmed(
      ExperimentalVariablesDialog.ConfirmEvent confirmEvent) {

    List<ExperimentalVariableContent> variableContents = confirmEvent.getSource()
        .definedVariables();
    List<AsyncProjectService.ExperimentalVariable> variables = variableContents.stream()
        .map(it -> new AsyncProjectService.ExperimentalVariable(it.name(),
            new HashSet<>(it.levels()), it.unit()))
        .toList();

    ExperimentalVariableAdditions body = new ExperimentalVariableAdditions(variables);
    ProjectId projectId = context.projectId().orElseThrow();
    ExperimentId experimentId = context.experimentId().orElseThrow();
    var ui = UI.getCurrent();

    ExperimentUpdateRequest request = new ExperimentUpdateRequest(
        projectId.value(),
        experimentId.value(), body);

    deleteExistingExperimentalVariables()
        .doOnNext(it -> log.info(
            "Removed variables: " + ((ExperimentalVariables) it.body()).experimentalVariables()
                .stream()
                .map(AsyncProjectService.ExperimentalVariable::name)
                .collect(Collectors.joining(", "))))
        .flatMap(it ->
            asyncProjectService.update(request))
        .doOnNext(it -> ui.access(() -> {
          confirmEvent.getSource().close();
          reloadExperimentInfo(projectId,
              experimentId);
          if (hasExperimentalGroups()) {
            showSampleRegistrationPossibleNotification();
          }
        }))
        .doOnNext(it -> log.info(
            "Added variables: " + ((ExperimentalVariables) it.body()).experimentalVariables()
                .stream()
                .map(AsyncProjectService.ExperimentalVariable::name)
                .collect(Collectors.joining(", "))))
        .subscribe();

  }

  private boolean editVariablesNotAllowed() {
    int numberOfRegisteredSamples = sampleInformationService.countPreviews(
        context.experimentId().orElseThrow(), "");
    if (numberOfRegisteredSamples > 0) {
      showExistingSamplesPreventVariableEdit(numberOfRegisteredSamples);
      return true;
    }
    int numOfExperimentalGroups = experimentInformationService.getExperimentalGroups(
        context.projectId().orElseThrow().value(),
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

  private void addSampleSourceInformationComponent() {
    sampleSourceComponent.addClassName("sample-source-display");
    content.add(sampleSourceComponent);
  }

  private Div createSampleSourceList(String titleText, AbstractIcon<?> icon,
      List<OntologyTermV1> ontologyClasses) {
    icon.addClassName("primary");
    Div sampleSource = new Div();
    sampleSource.addClassName("sample-source");
    Span title = new Span(titleText);
    Span header = new Span(icon, title);
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
    List<OntologyTermV1> speciesTags = new ArrayList<>(experiment.getSpecies());
    List<OntologyTermV1> specimenTags = new ArrayList<>(experiment.getSpecimens());
    List<OntologyTermV1> analyteTags = new ArrayList<>(experiment.getAnalytes());

    BioIcon speciesIcon = BioIcon.getOptionsForType(SampleSourceType.SPECIES).stream()
        .filter(icon -> icon.label.equals(experiment.getSpeciesIconName())).findFirst()
        .orElse(BioIcon.DEFAULT_SPECIES);
    BioIcon specimenIcon = BioIcon.getOptionsForType(SampleSourceType.SPECIMEN).stream()
        .filter(icon -> icon.label.equals(experiment.getSpecimenIconName())).findFirst()
        .orElse(BioIcon.DEFAULT_SPECIMEN);
    BioIcon analyteIcon = BioIcon.getOptionsForType(SampleSourceType.ANALYTE).stream()
        .filter(icon -> icon.label.equals(experiment.getAnalyteIconName())).findFirst()
        .orElse(BioIcon.DEFAULT_ANALYTE);

    sampleSourceComponent.add(
        createSampleSourceList("Species", speciesIcon.iconResource.createIcon(), speciesTags));
    sampleSourceComponent.add(
        createSampleSourceList("Specimen", specimenIcon.iconResource.createIcon(), specimenTags));
    sampleSourceComponent.add(
        createSampleSourceList("Analytes", analyteIcon.iconResource.createIcon(), analyteTags));
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
    List<ExperimentalVariable> variables = experimentInformationService.getVariablesOfExperiment(
        context.projectId().orElseThrow().value(),
        experimentId);
    List<VariableLevel> levels = variables.stream()
        .flatMap(variable -> variable.levels().stream())
        .toList();
    var groups = experimentInformationService.getExperimentalGroups(
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
      addExperimentalGroups(groupContents);

      reloadExperimentalGroups();
      dialog.close();
    }
  }

  private void openExperimentalGroupEditDialog() {
    if (editGroupsNotAllowed()) {
      return;
    }
    ExperimentId experimentId = context.experimentId().orElseThrow();
    List<ExperimentalVariable> variables = experimentInformationService.getVariablesOfExperiment(
        context.projectId().orElseThrow().value(),
        experimentId);
    List<VariableLevel> levels = variables.stream()
        .flatMap(variable -> variable.levels().stream()).toList();
    var groups = experimentInformationService.getExperimentalGroups(
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
      var groupDTOs = dialog.experimentalGroups().stream()
          .map(this::toExperimentalGroupDTO).toList();
      ExperimentId experimentId = context.experimentId().orElseThrow();
      experimentInformationService.updateExperimentalGroupsOfExperiment(
          context.projectId().orElseThrow().value(), experimentId, groupDTOs);
      reloadExperimentalGroups();
      confirmEvent.getSource().close();
    }
  }

  private void addExperimentalGroups(
      Collection<ExperimentalGroupContent> experimentalGroupContents) {
    List<ExperimentalGroupDTO> experimentalGroupDTOS = experimentalGroupContents.stream()
        .map(this::toExperimentalGroupDTO).toList();
    ExperimentId experimentId = context.experimentId().orElseThrow();
    experimentInformationService.updateExperimentalGroupsOfExperiment(
        context.projectId().orElseThrow().value(),
        experimentId, experimentalGroupDTOS);
  }

  private ExperimentalGroupDTO toExperimentalGroupDTO(
      ExperimentalGroupContent experimentalGroupContent) {
    return new ExperimentalGroupDTO(experimentalGroupContent.id(), experimentalGroupContent.name(),
        experimentalGroupContent.variableLevels(), experimentalGroupContent.size());
  }

  private ExperimentalGroupContent toContent(ExperimentalGroupDTO experimentalGroupDTO) {
    return new ExperimentalGroupContent(experimentalGroupDTO.id(), experimentalGroupDTO.name(),
        experimentalGroupDTO.replicateCount(), experimentalGroupDTO.levels());
  }

  private void showSampleRegistrationPossibleNotification() {
    Notification notification = createSampleRegistrationPossibleNotification();
    notification.open();
  }

  private void reloadExperimentalGroups() {
    List<ExperimentalGroup> groups = loadExperimentalGroups();
    fillExperimentalGroupCollection(groups);
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

  private List<ExperimentalGroup> loadExperimentalGroups() {
    // We load the experimental groups of the experiment and render them as cards
    return experimentInformationService.experimentalGroupsFor(
        context.projectId().orElseThrow().value(),
        context.experimentId().orElseThrow());
  }

  private void addExperimentalVariables(
      List<ExperimentalVariableContent> experimentalVariableContents) {
    experimentalVariableContents.forEach(
        experimentalVariableContent -> experimentInformationService.addVariableToExperiment(
            context.projectId().orElseThrow().value(),
            context.experimentId().orElseThrow(),
            experimentalVariableContent.name(), experimentalVariableContent.unit(),
            experimentalVariableContent.levels()));
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
    // We load the experimental variables of the experiment and render them as cards
    var experimentalVariables = experiment.variables();
    fillExperimentalVariablesCollection(experimentalVariables);
    if (experiment.variables().isEmpty()) {
      onNoVariablesDefined();
    } else {
      onVariablesDefined();
    }

    var experimentalGroups = loadExperimentalGroups();
    fillExperimentalGroupCollection(experimentalGroups);
    if (experiment.getExperimentalGroups().isEmpty()) {
      onNoGroupsDefined();
    } else {
      onGroupsDefined();
    }

    var confoundingVariables = loadConfoundingVariables(projectId.value(), experimentId);
    fillConfoundingVariablesCollection(confoundingVariables);
    if (confoundingVariables.isEmpty()) {
      onNoConfoundingDefined();
    } else {
      onConfoundingDefined();
    }
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

  private void fillExperimentalVariablesCollection(List<ExperimentalVariable> variables) {
    Comparator<String> natOrder = Comparator.naturalOrder();
    List<ExperimentalVariableCard> experimentalVariableCards = variables.stream()
        .sorted((var1, var2) -> natOrder.compare(var1.name().value(), var2.name().value()))
        .map(ExperimentalVariableCard::new).toList();

    experimentalVariableCollection.setContent(experimentalVariableCards);
  }

  private void fillConfoundingVariablesCollection(
      List<ConfoundingVariableInformation> confoundingVariables) {
    var cards = new ArrayList<Component>();
    for (ConfoundingVariableInformation confoundingVariable : confoundingVariables) {
      Card card = new Card();
      card.addClassNames("padding-left-right-05", "padding-top-bottom-05");
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

  /**
   * Describes a species, specimen or analyte icon with label, type and source. Note that the icon
   * source is stored instead of the icon itself, because mixing enums and Components causes trouble
   * at runtime.
   */
  public enum BioIcon {
    DEFAULT_SPECIES(Constants.DEFAULT_LABEL, SampleSourceType.SPECIES, VaadinIcon.BUG),
    HUMAN("Human", SampleSourceType.SPECIES, VaadinIcon.MALE),
    //Mouse by Graphic Mall
    MOUSE("Mouse", SampleSourceType.SPECIES, new StreamResource("mouse.svg",
        () -> BioIcon.class.getResourceAsStream("/icons/mouse.svg"))),
    PLANT("Plant", SampleSourceType.SPECIES, new StreamResource("plant.svg",
        () -> BioIcon.class.getResourceAsStream("/icons/plant.svg"))),
    //Mushroom by Jemis Mali on IconScout
    FUNGI("Fungi", SampleSourceType.SPECIES, new StreamResource("mushroom.svg",
        () -> BioIcon.class.getResourceAsStream("/icons/mushroom.svg"))),
    BACTERIA("Bacteria", SampleSourceType.SPECIES, new StreamResource("bacteria.svg",
        () -> BioIcon.class.getResourceAsStream("/icons/bacteria.svg"))),
    DEFAULT_SPECIMEN(Constants.DEFAULT_LABEL, SampleSourceType.SPECIMEN, VaadinIcon.DROP),
    //Kidneys by Daniel Burka on IconScout
    KIDNEY("Kidney", SampleSourceType.SPECIMEN, new StreamResource("kidneys.svg",
        () -> BioIcon.class.getResourceAsStream("/icons/kidneys.svg"))),
    //Liver by Daniel Burka on IconScout
    LIVER("Liver", SampleSourceType.SPECIMEN, new StreamResource("liver.svg",
        () -> BioIcon.class.getResourceAsStream("/icons/liver.svg"))),
    //Heart by Vector Stall on IconScout
    HEART("Heart", SampleSourceType.SPECIMEN, new StreamResource("heart.svg",
        () -> BioIcon.class.getResourceAsStream("/icons/heart.svg"))),
    //Leaf by Phosphor Icons on IconScout
    LEAF("Leaf", SampleSourceType.SPECIMEN, new StreamResource("leaf.svg",
        () -> BioIcon.class.getResourceAsStream("/icons/leaf.svg"))),
    EYE("Eye", SampleSourceType.SPECIMEN, VaadinIcon.EYE),
    DEFAULT_ANALYTE(Constants.DEFAULT_LABEL, SampleSourceType.ANALYTE, VaadinIcon.CLUSTER);
    private final String label;
    private final SampleSourceType type;
    private final IconResource iconResource;

    BioIcon(String label, SampleSourceType type, VaadinIcon icon) {
      this.label = label;
      this.type = type;
      this.iconResource = new IconResource(icon);
    }

    BioIcon(String label, SampleSourceType type, StreamResource svgResource) {
      this.label = label;
      this.type = type;
      this.iconResource = new IconResource(svgResource);
    }

    public static BioIcon getTypeWithNameOrDefault(SampleSourceType sampleSourceType,
        String iconName) {
      Optional<BioIcon> searchResult = getOptionsForType(sampleSourceType).stream()
          .filter(icon -> icon.label.equals(iconName)).findFirst();
      return searchResult.orElseGet(() -> getDefaultBioIcon(sampleSourceType));
    }

    public static BioIcon getDefaultBioIcon(SampleSourceType sampleSourceType) {
      return switch (sampleSourceType) {
        case SPECIES -> DEFAULT_SPECIES;
        case SPECIMEN -> DEFAULT_SPECIMEN;
        case ANALYTE -> DEFAULT_ANALYTE;
      };
    }

    public static List<BioIcon> getOptionsForType(SampleSourceType type) {
      return Arrays.stream(BioIcon.values()).filter(o ->
          o.getType().equals(type)).toList();
    }

    public String getLabel() {
      return label;
    }

    public SampleSourceType getType() {
      return type;
    }

    public IconResource getIconResource() {
      return iconResource;
    }

    private static class Constants {

      public static final String DEFAULT_LABEL = "default";
    }
  }

  /**
   * Describes the source level of a sample: species, specimen or analyte
   */
  public enum SampleSourceType {
    SPECIES, SPECIMEN, ANALYTE
  }

  /**
   * Wrapper class for different icon resources, e.g. VaadinIcons or custom SVGs. Provides a method
   * to create the respective Icon component.
   */
  public static class IconResource {

    private StreamResource streamResource = null;
    private VaadinIcon vaadinIconResource = null;

    public IconResource(VaadinIcon vaadinIconResource) {
      this.vaadinIconResource = vaadinIconResource;
    }

    public IconResource(StreamResource streamResource) {
      this.streamResource = streamResource;
    }

    public AbstractIcon createIcon() {
      if (streamResource != null) {
        return new SvgIcon(streamResource);
      } else {
        return vaadinIconResource.create();
      }
    }
  }
}
