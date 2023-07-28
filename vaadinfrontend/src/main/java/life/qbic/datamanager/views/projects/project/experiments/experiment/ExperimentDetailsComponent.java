package life.qbic.datamanager.views.projects.project.experiments.experiment;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.ApplicationException.ErrorCode;
import life.qbic.application.commons.ApplicationException.ErrorParameters;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.general.CreationCard;
import life.qbic.datamanager.views.general.DisclaimerCard;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.ToggleDisplayEditComponent;
import life.qbic.datamanager.views.notifications.InformationMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.projects.project.experiments.ExperimentInformationMain;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentInfoComponent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalGroupCardCollection;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalGroupsDialog;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalGroupsDialog.ExperimentalGroupContent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalVariablesComponent;
import life.qbic.datamanager.views.projects.project.experiments.experiment.components.ExperimentalVariablesDialog;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.DeletionService;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ExperimentInformationService.ExperimentalGroupDTO;
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
 * a {@link Project} within the {@link ExperimentInformationMain}
 */
@UIScope
@SpringComponent
public class ExperimentDetailsComponent extends PageArea {

  private static final Logger log = logger(ExperimentDetailsComponent.class);
  @Serial
  private static final long serialVersionUID = -8992991642015281245L;
  private final transient ExperimentInformationService experimentInformationService;
  private final Div content = new Div();
  private final Span title = new Span();
  private final Div tagCollection = new Div();
  private final TabSheet experimentSheet = new TabSheet();
  private final ExperimentalVariablesComponent experimentalVariablesComponent = ExperimentalVariablesComponent.create(
      new ArrayList<>());
  private final Div contentExperimentalGroupsTab = new Div();
  private final Div experimentSummary = new Div();
  private final ExperimentalGroupCardCollection experimentalGroupsCollection = new ExperimentalGroupCardCollection();
  private final ExperimentalVariablesDialog addExperimentalVariablesDialog;
  private final DisclaimerCard noExperimentalVariablesDefined;
  private final CreationCard experimentalGroupCreationCard = CreationCard.create(
      "Add experimental groups");
  private final DisclaimerCard addExperimentalVariablesNote;
  private final DeletionService deletionService;
  private ExperimentId experimentId;


  public ExperimentDetailsComponent(
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired DeletionService deletionService) {
    this.experimentInformationService = Objects.requireNonNull(experimentInformationService);
    this.deletionService = Objects.requireNonNull(deletionService);
    this.addExperimentalVariablesDialog = new ExperimentalVariablesDialog();
    this.noExperimentalVariablesDefined = createNoVariableDisclaimer();
    this.addExperimentalVariablesNote = createNoVariableDisclaimer();
    this.addClassName("experiment-details-component");
    layoutComponent();
    configureComponent();
  }

  private DisclaimerCard createNoVariableDisclaimer() {
    var disclaimer = DisclaimerCard.createWithTitle("Missing variables",
        "No experiment variables defined", "Add");
    disclaimer.subscribe(listener -> displayAddExperimentalVariablesDialog());
    return disclaimer;
  }

  private void layoutComponent() {
    this.add(content);
    content.addClassName("details-content");
    setTitle();
    initTagAndNotesLayout();
    layoutTabSheet();
  }

  private void setTitle() {
    title.addClassName("title");
    addComponentAsFirst(title);
  }

  private void configureComponent() {
    configureExperimentalGroupCreation();
    configureExperimentalGroupsEdit();
    addCancelListenerForAddVariableDialog();
    addConfirmListenerForAddVariableDialog();
    addListenerForNewVariableEvent();
    addEditListenerForExperimentalVariables();
  }

  private void addEditListenerForExperimentalVariables() {
    experimentalVariablesComponent.subscribeToEditEvent(experimentalVariablesEditEvent -> {
      var editDialog = ExperimentalVariablesDialog.prefilled(
          experimentInformationService.getVariablesOfExperiment(experimentId));
      editDialog.subscribeToCancelEvent(
          experimentalVariablesDialogCancelEvent -> editDialog.close());
      editDialog.subscribeToConfirmEvent(experimentalVariablesDialogConfirmEvent -> {
        deleteExistingExperimentalVariables(experimentId);
        registerExperimentalVariables(experimentalVariablesDialogConfirmEvent.getSource());
        editDialog.close();
        reloadExperimentalVariables();
      });
      editDialog.open();
    });
  }

  private void reloadExperimentalVariables() {
    loadExperiment(experimentId);
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

  private void initTagAndNotesLayout() {
    tagCollection.addClassName("tag-collection");
    Span emptyNotes = new Span("Click to add Notes");
    ToggleDisplayEditComponent<Span, TextField, String> experimentNotes = new ToggleDisplayEditComponent<>(
        Span::new, new TextField(), emptyNotes);
    content.add(tagCollection, experimentNotes);
  }

  private void layoutTabSheet() {
    experimentSheet.add("Summary", experimentSummary);
    experimentSummary.addClassName(Display.FLEX);
    experimentSheet.add("Experimental Groups", contentExperimentalGroupsTab);
    content.add(experimentSheet);
    experimentSheet.setSizeFull();
  }

  private void configureExperimentalGroupCreation() {
    experimentalGroupsCollection.subscribeToAddEvents(listener -> {
      List<ExperimentalVariable> variables = experimentInformationService.getVariablesOfExperiment(
          experimentId);
      List<VariableLevel> levels = variables.stream()
          .flatMap(variable -> variable.levels().stream()).toList();
      var dialog = ExperimentalGroupsDialog.empty(levels);
      dialog.subscribeToCancelEvent(cancelEvent -> cancelEvent.getSource().close());
      dialog.subscribeToConfirmEvent(
          confirmEvent -> {
            saveNewGroups(confirmEvent.getSource().experimentalGroups());
            reloadExperimentalGroups();
            dialog.close();
          });
      dialog.open();
    });
  }

  private void configureExperimentalGroupsEdit() {
    experimentalGroupsCollection.subscribeToEditEvents(listener -> {
      List<ExperimentalVariable> variables = experimentInformationService.getVariablesOfExperiment(
          experimentId);
      List<VariableLevel> levels = variables.stream()
          .flatMap(variable -> variable.levels().stream()).toList();
      var experimentalGroups = experimentInformationService.getExperimentalGroups(experimentId)
          .stream().map(this::toContent).toList();
      var dialog = ExperimentalGroupsDialog.prefilled(levels, experimentalGroups);
      dialog.subscribeToCancelEvent(cancelEvent -> cancelEvent.getSource().close());
      dialog.subscribeToConfirmEvent(
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
    deletionService.deleteAllExperimentalGroups(experimentId).onError(error -> {
      throw new ApplicationException("Could not edit experiments because samples are already registered.");
    });
    experimentalGroupContents.stream().map(this::toExperimentalGroupDTO).map(
            experimentalGroupDTO -> experimentInformationService.addExperimentalGroupToExperiment(
                experimentId, experimentalGroupDTO)).filter(Result::isError).findAny()
        .ifPresent(result -> {
          throw new ApplicationException("Could not save one or more experimental groups.");
        });
  }

  private ExperimentalGroupDTO toExperimentalGroupDTO(
      ExperimentalGroupContent experimentalGroupContent) {
    return new ExperimentalGroupDTO(experimentalGroupContent.variableLevels(),
        experimentalGroupContent.size());
  }

  private ExperimentalGroupContent toContent(ExperimentalGroupDTO experimentalGroupDTO) {
    return new ExperimentalGroupContent(experimentalGroupDTO.sampleSize(),
        experimentalGroupDTO.levels());
  }

  private void saveNewGroups(Collection<ExperimentalGroupContent> experimentalGroupContents) {
    experimentalGroupContents.stream()
        .map(content -> new ExperimentalGroupDTO(content.variableLevels(), content.size()))
        .map(this::registerNewGroup)
        .filter(Result::isError).findAny().ifPresent(errorResult -> {
          throw new ApplicationException("Could not save one or more groups.");
        });
  }

  private Result<ExperimentalGroup, ResponseCode> registerNewGroup(
      ExperimentalGroupDTO experimentalGroupDTO) {
    return this.experimentInformationService.addExperimentalGroupToExperiment(experimentId,
        experimentalGroupDTO);
  }

  private void addCancelListenerForAddVariableDialog() {
    addExperimentalVariablesDialog.subscribeToCancelEvent(it -> it.getSource().close());
  }


  private void reloadExperimentalGroups() {
    loadExperimentalGroups();
  }

  private void loadExperimentalGroups() {
    Objects.requireNonNull(experimentId, "Experiment id not set");
    // We load the experimental groups of the experiment and render them as cards
    List<ExperimentalGroupCard> experimentalGroupsCards = experimentInformationService.experimentalGroupsFor(
        experimentId).stream().map(ExperimentalGroupCard::new).toList();

    experimentalGroupsCollection.setContent(experimentalGroupsCards);
  }


  private void handleCreationClickedEvent(
      ExperimentalGroupDeletionEvent experimentalGroupDeletionEvent) {
    experimentInformationService.deleteExperimentGroup(experimentId,
        experimentalGroupDeletionEvent.getSource().groupId());
    experimentalGroupsCollection.remove(experimentalGroupDeletionEvent.getSource());
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
      ExperimentalVariablesDialog experimentalVariablesDialog) {
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
    title.setText(experiment.getName());
    loadTagInformation(experiment);
    loadExperimentInfo(experiment);
    reloadExperimentalGroups();
    if (experiment.variables().isEmpty()) {
      useCaseNoVariablesYet();
    } else {
      removeDisclaimer();
      displayExperimentalGroupsCollection();
    }
  }

  private void loadTagInformation(Experiment experiment) {
    tagCollection.removeAll();
    List<String> tags = new ArrayList<>();
    experiment.getSpecies().forEach(species -> tags.add(species.value()));
    experiment.getSpecimens().forEach(specimen -> tags.add(specimen.value()));
    experiment.getAnalytes().forEach(analyte -> tags.add(analyte.value()));
    tags.stream().map(it -> new Tag(it)).forEach(tagCollection::add);
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

}
