package life.qbic.datamanager.views.projects.project.experiments.experiment;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.UiHandle;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.confounding.ConfoundingVariable;
import life.qbic.datamanager.views.general.confounding.ConfoundingVariablesUserInput;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogAction;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.datamanager.views.general.icon.IconFactory;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentSetupMain;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalGroupsSection;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalGroupsSection.ExperimentalGroupContent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.ExperimentalVariablesInput;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableAdded;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableDeleted;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableLevelsChanged;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableRenamed;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.experimentalvariable.VariableChange.VariableUnitChanged;
import life.qbic.datamanager.views.projects.project.samples.SampleInformationMain;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ExperimentalGroupCreationRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ExperimentalGroupDeletionRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ExperimentalGroupDeletionResponse;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ExperimentalGroupUpdateRequest;
import life.qbic.projectmanagement.application.api.AsyncProjectService.ExperimentalGroupUpdateResponse;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ConfoundingVariableInformation;
import life.qbic.projectmanagement.application.confounding.ConfoundingVariableService.ExperimentReference;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService.ExperimentalVariableInformation;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService.UsedVariableLevel;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
import life.qbic.projectmanagement.application.ontology.SpeciesLookupService;
import life.qbic.projectmanagement.application.ontology.TerminologyService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <b>Experiment design component</b>
 *
 * <p>The design surface of an experiment: the experimental variables, the experimental groups built
 * from them, and the confounding variables. Rendered by
 * {@link life.qbic.datamanager.views.projects.project.experiments.ExperimentDesignMain}.</p>
 *
 * <p>The experiment's name and sample origins are <em>not</em> here. They live on
 * {@link ExperimentSetupComponent}, because they exist for one reason — they become the
 * Species/Specimen/Analyte options of the sample-registration template (the template's Condition
 * option comes from the groups below). Both surfaces used to share one route stacked top-to-bottom,
 * which meant a user who came to edit the design had to scroll past the entire setup form to reach
 * it (project owner, 2026-10-19).</p>
 */
@UIScope
@SpringComponent
public class ExperimentDesignComponent extends PageArea {

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";
  private static final Logger log = LoggerFactory.logger(ExperimentDesignComponent.class);
  @Serial
  private static final long serialVersionUID = -8992991642015281245L;
  private final transient ExperimentInformationService experimentInformationService;
  private final transient SampleInformationService sampleInformationService;
  private final Div content = new Div();
  private final Div header = new Div();
  private final Span title = new Span();
  private final TabSheet experimentSheet = new TabSheet();
  private Tab experimentalVariablesTab;
  private Tab experimentalGroupsTab;
  private Tab confoundingVariablesTab;
  private final Div experimentalGroupsContainer = new Div();
  private final Div experimentalVariablesContainer = new Div();
  private final Div confoundingVariablesContainer;
  /**
   * The link to the setup surface, whose route can only be built once the context is known.
   *
   * <p>A {@link RouterLink} needs its {@link RouteParameters} up front — it does not read them from
   * the current location — but the context only arrives in {@link #setContext(Context)}, after this
   * component's constructor has already run. Building the route eagerly in the constructor therefore
   * dereferenced a null context and broke bean creation, so the route is set in {@code setContext}
   * instead and the link is created targetless here.</p>
   */
  private final RouterLink setupLink = new RouterLink();
  private final transient MessageSourceNotificationFactory messageSourceNotificationFactory;
  private final transient ConfoundingVariableService confoundingVariableService;
  private final transient AsyncProjectService asyncProjectService;
  private Context context;
  private final UiHandle uiHandle = new UiHandle();

  @Autowired
  public ExperimentDesignComponent(
      ExperimentInformationService experimentInformationService,
      SampleInformationService sampleInformationService,
      MessageSourceNotificationFactory messageSourceNotificationFactory,
      ConfoundingVariableService confoundingVariableService,
      AsyncProjectService asyncProjectService) {
    this.confoundingVariableService = requireNonNull(confoundingVariableService);
    this.messageSourceNotificationFactory = requireNonNull(messageSourceNotificationFactory,
        "messageSourceNotificationFactory must not be null");
    this.experimentInformationService = requireNonNull(experimentInformationService);
    this.sampleInformationService = sampleInformationService;
    this.addClassName("experiment-design-component");
    confoundingVariablesContainer = new Div();
    confoundingVariablesContainer.addClassNames("width-full", "height-full");

    layoutComponent();
    this.asyncProjectService = asyncProjectService;
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
    uiHandle.bind(attachEvent.getUI());
    reloadExperimentInfo(context.projectId().orElseThrow(), context.experimentId().orElseThrow());
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    super.onDetach(detachEvent);
    uiHandle.unbind();
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

  private void layoutComponent() {
    this.add(header);
    header.addClassName("header");
    this.add(content);
    //Necessary to avoid css collution
    content.addClassName("details-content");
    // The H1 names this surface's job. "Edit experiment" is gone: the experiment now has two
    // editable surfaces, so one generic verb could not say which one the user is on.
    Div titleBlock = new Div(backToOverviewButton(), title);
    titleBlock.addClassName("experiment-design-title-block");
    header.add(titleBlock);
    title.setText("Experimental design");
    title.addClassName("title");
    // The design is the page's primary content now, instead of a band below a setup form that had to
    // be scrolled past. It is the first thing on the page and its first tab is immediately visible.
    content.add(sectionHeader("Experimental design", designPurpose()));
    layoutTabSheet();
  }

  /**
   * Names what the design is for, and the one dependency it has on the other surface. The
   * registration template's Condition column is built from these groups, so a user who has not yet
   * set up sample origins is told where to go instead of being left to guess why their template is
   * still incomplete.
   */
  private Div designPurpose() {
    Div purpose = new Div();
    purpose.addClassName("experiment-design-section-purpose");
    purpose.add(new Span("The experimental variables, the experimental groups built from them, and "
        + "the confounding variables. A group's condition selects one level per variable, and those "
        + "conditions become the Condition options in the sample-registration template. Each tab "
        + "saves its own section. The template's Species, Specimen and Analyte options come from "));
    // The link text is a child, not the RouterLink's own text, so the target can be set later (see
    // the setupLink field).
    setupLink.removeAll();
    setupLink.add(new Span("Experiment setup"));
    setupLink.addClassName("experiment-design-cross-link");
    purpose.add(setupLink, new Span("."));
    return purpose;
  }

  private static Div sectionHeader(String title, Div purpose) {
    Div section = new Div();
    section.addClassName("experiment-design-section-header");
    Div text = new Div();
    text.addClassName("experiment-design-section-text");
    Span titleSpan = new Span(title);
    titleSpan.addClassName("experiment-design-section-title");
    text.add(titleSpan, purpose);
    section.add(text);
    return section;
  }

  /** A top-right action row for a tab's own save action, so every section's primary button aligns. */
  private static Div actionBar(Component... components) {
    Div bar = new Div(components);
    bar.addClassName("inline-section-actions");
    return bar;
  }

  /**
   * The way back to the overview without saving, prominent on the page. The nav's Overview item and
   * the browser's Back both work, but neither is discoverable as "leave this edit". Navigating away
   * discards unsaved inline edits, which is the page's cancel behaviour.
   */
  private Button backToOverviewButton() {
    Button back = new Button("Back to overview", VaadinIcon.ARROW_LEFT.create());
    back.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    back.addClassName("experiment-design-back");
    back.addClickListener(event -> getUI().ifPresent(
        ui -> ui.navigate(ExperimentInformationMain.class, experimentRouteParameters())));
    return back;
  }

  private RouteParameters experimentRouteParameters() {
    return new RouteParameters(
        new RouteParam(PROJECT_ID_ROUTE_PARAMETER, context.projectId().orElseThrow().value()),
        new RouteParam(EXPERIMENT_ID_ROUTE_PARAMETER, context.experimentId().orElseThrow().value()));
  }

  /** Confirms a successful save on the UI thread, the one shared path for every section's save. */
  private void displaySaved(String messageKey) {
    getUI().ifPresent(ui -> ui.access(() -> messageSourceNotificationFactory
        .toast(messageKey, new Object[]{}, getLocale()).open()));
  }

  private void reloadConfoundingVariables() {
    getUI().ifPresent(ui -> ui.access(() -> {
      var projectId = context.projectId().orElseThrow();
      var experimentId = context.experimentId().orElseThrow();
      List<ConfoundingVariable> current = loadConfoundingVariables(projectId.value(), experimentId)
          .stream()
          .map(it -> new ConfoundingVariable(it.id(), it.variableName()))
          .toList();
      ConfoundingVariablesUserInput userInput = new ConfoundingVariablesUserInput();
      userInput.setVariables(new ArrayList<>(current));
      userInput.addClassName("inline-confounding-variables");

      Button save = new Button("Save confounding variables");
      save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      save.addClassName("inline-save");
      save.addClickListener(event -> onConfoundingVariablesSaved(userInput, current));

      confoundingVariablesContainer.removeAll();
      confoundingVariablesContainer.add(actionBar(save), userInput);
      if (confoundingVariablesTab != null) {
        confoundingVariablesTab.setLabel("Confounding Variables (" + current.size() + ")");
      }
    }));
  }

  private void onConfoundingVariablesSaved(ConfoundingVariablesUserInput userInput,
      List<ConfoundingVariable> oldValue) {
    if (!userInput.validate().hasPassed()) {
      return;
    }
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
                    || old.name().equals(newVal.name())))
        .toList();
    if (createdVars.isEmpty() && renamedVars.isEmpty() && deletedVars.isEmpty()) {
      return;
    }
    DialogAction editingAction = () -> {
      editConfoundingVariables(createdVars, renamedVars, deletedVars);
      reloadExperimentInfo(context.projectId().orElseThrow(), context.experimentId().orElseThrow());
      displaySaved("confounding.variables.updated.success");
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

  private void reloadExperimentInfo(ProjectId projectId, ExperimentId experimentId) {
    loadExperimentInformation(projectId, experimentId);
    reloadExperimentalVariables();
    reloadExperimentalGroups();
  }

  private void reloadExperimentalVariables() {
    getUI().ifPresent(ui -> ui.access(() -> {
      ExperimentId experimentId = context.experimentId().orElseThrow();
      String projectId = context.projectId().orElseThrow().value();
      ExperimentalVariablesInput variablesInput = new ExperimentalVariablesInput();
      var usedLevelsByVariable = experimentInformationService.getUsedVariableLevels(projectId,
              experimentId).stream()
          .collect(Collectors.groupingBy(UsedVariableLevel::variableName));

      List<ExperimentalVariableInformation> variablesOfExperiment =
          experimentInformationService.getVariablesOfExperiment(projectId, experimentId);
      var variables = variablesOfExperiment.stream()
          .map(it -> new ExperimentalVariablesInput.ExperimentalVariableInformation(
              it.name(), it.unit(), it.levels()))
          .sorted(Comparator.comparing(
              ExperimentalVariablesInput.ExperimentalVariableInformation::name))
          .toList();
      variables.forEach(variablesInput::addVariable);
      if (variables.isEmpty()) {
        variablesInput.addVariable();
      }

      for (Entry<String, List<UsedVariableLevel>> entry : usedLevelsByVariable.entrySet()) {
        Set<String> usedValues = entry.getValue().stream()
            .map(UsedVariableLevel::value)
            .collect(Collectors.toSet());
        variablesInput.setUsedLevels(entry.getKey(), usedValues);
      }
      if (!usedLevelsByVariable.isEmpty()) {
        variablesInput.setAddVariablesEnabled(false);
      }
      variablesInput.markInitialized();

      Button save = new Button("Save variables");
      save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
      save.addClassName("inline-save");
      save.addClickListener(event -> onVariablesSaved(variablesInput));

      Span fieldStateNote = new Span(
          "Grey fields are editable. A dashed level with a padlock is used by registered samples "
              + "and cannot be changed; the trash icon removes a value.");
      fieldStateNote.addClassName("experimental-variables-note");

      experimentalVariablesContainer.removeAll();
      experimentalVariablesContainer.add(actionBar(save), fieldStateNote, variablesInput);
      if (experimentalVariablesTab != null) {
        experimentalVariablesTab.setLabel(
            "Experimental Variables (" + variablesOfExperiment.size() + ")");
      }
    }));
  }

  private void onVariablesSaved(ExperimentalVariablesInput variablesInput) {
    if (!variablesInput.validate().hasPassed()) {
      return;
    }
    List<VariableChange> changes = variablesInput.getChanges();
    if (changes.isEmpty()) {
      return;
    }
    Map<String, List<VariableChange>> changesByVariable = changes.stream()
        .collect(Collectors.groupingBy(VariableChange::affectedVariable));
    changesByVariable.forEach(this::applyChangesToVariable);
    reloadExperimentalVariables();
    // Variables are the prerequisite for a group's condition, so the groups section has to be
    // re-rendered too. Without this the Groups tab kept showing the "Define variables first"
    // locked state from ExperimentalGroupsSection after variables had been added, until a full
    // page load happened to refresh it.
    reloadExperimentalGroups();
    displaySaved("experimental.variables.updated.success");
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

  private void layoutTabSheet() {
    experimentSheet.addClassName("experimental-sheet");
    // The tab labels carry the count of each section, so the tab strip is a one-glance overview of
    // what the experiment already defines, not just a set of destinations.
    experimentalVariablesTab = experimentSheet.add("Experimental Variables",
        experimentalVariablesContainer);
    experimentalVariablesContainer.addClassName("experimental-groups-container");
    experimentalGroupsTab = experimentSheet.add("Experimental Groups", experimentalGroupsContainer);
    experimentalGroupsContainer.addClassName("experimental-groups-container");
    confoundingVariablesTab = experimentSheet.add("Confounding Variables",
        confoundingVariablesContainer);
    content.add(experimentSheet);
  }

  private List<ExperimentalGroupInput.VariableLevel> experimentalVariableLevels() {
    ExperimentId experimentId = context.experimentId().orElseThrow();
    List<ExperimentalVariableInformation> variables = experimentInformationService
        .getVariablesOfExperiment(context.projectId().orElseThrow().value(), experimentId);
    return variables.stream()
        .flatMap(variable -> variable.levels().stream()
            .map(level -> new ExperimentalGroupInput.VariableLevel(variable.name(), level,
                variable.unit())))
        .toList();
  }

  private boolean samplesRegistered() {
    return sampleInformationService.countPreviews(context.experimentId().orElseThrow(), "") > 0;
  }

  /**
   * Applies the inline group editor's state. Existing groups can only be changed when the section is
   * in edit mode (no samples registered yet); otherwise only new groups are created.
   */
  private void onGroupsSaved(ExperimentalGroupsSection.SaveEvent event) {
    ExperimentalGroupsSection section = event.getSource();
    var groups = section.experimentalGroups();
    var groupsToCreate = groups.stream().filter(group -> group.id() == -1L).toList();
    var groupsToUpdate = groups.stream().filter(group -> group.id() != -1L).toList();
    if (section.isEditMode()) {
      addExperimentalGroups(groupsToCreate);
      updateExperimentalGroups(groupsToUpdate);
      deleteExperimentalGroups(section.groupsToDelete());
    } else {
      addExperimentalGroups(groupsToCreate);
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
      var contents = groups.stream().map(this::toContent).toList();
      var levels = experimentalVariableLevels();
      ExperimentalGroupsSection section;
      if (contents.isEmpty()) {
        section = ExperimentalGroupsSection.empty(levels);
      } else if (samplesRegistered()) {
        section = ExperimentalGroupsSection.nonEditable(levels, contents);
      } else {
        section = ExperimentalGroupsSection.editable(levels, contents);
      }
      section.addSaveListener(this::onGroupsSaved);
      // The locked state replaces the group form, so the page owns where its one action goes: the
      // variables live in a different tab of this same sheet, and selecting it is the page's job
      // because the page built the tabs.
      section.addGoToVariablesListener(event -> {
        if (experimentalVariablesTab != null) {
          experimentSheet.setSelectedTab(experimentalVariablesTab);
        }
      });
      experimentalGroupsContainer.removeAll();
      experimentalGroupsContainer.add(section);
      if (experimentalGroupsTab != null) {
        experimentalGroupsTab.setLabel("Experimental Groups (" + contents.size() + ")");
      }
    }));
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
    setupLink.setRoute(ExperimentSetupMain.class, experimentRouteParameters());
    reloadExperimentInfo(projectId, experimentId);
  }

  private void loadExperimentInformation(ProjectId projectId, ExperimentId experimentId) {
    // Loads the confounding variables for the experiment. The name and sample origins are not read
    // here any more: they belong to ExperimentSetupComponent.
    experimentInformationService.find(projectId.value(), experimentId).orElseThrow();
    reloadConfoundingVariables();
  }

  private List<ConfoundingVariableInformation> loadConfoundingVariables(String projectId,
      ExperimentId experimentId) {
    return confoundingVariableService.listConfoundingVariablesForExperiment(projectId,
        new ExperimentReference(experimentId.value()));
  }

}
