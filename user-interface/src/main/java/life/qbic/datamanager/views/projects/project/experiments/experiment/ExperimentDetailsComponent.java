package life.qbic.datamanager.views.projects.project.experiments.experiment;

import static java.util.Objects.requireNonNull;
import static life.qbic.datamanager.views.projects.project.experiments.experiment.SampleOriginType.ANALYTE;
import static life.qbic.datamanager.views.projects.project.experiments.experiment.SampleOriginType.SPECIES;
import static life.qbic.datamanager.views.projects.project.experiments.experiment.SampleOriginType.SPECIMEN;

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
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.ConfirmEvent;
import life.qbic.datamanager.views.general.Disclaimer;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.notifications.Toast;
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
import life.qbic.projectmanagement.application.DeletionService;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService.ExperimentalGroupDTO;
import life.qbic.projectmanagement.application.ontology.OntologyLookupService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.domain.model.OntologyTerm;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalDesign;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalVariable;
import life.qbic.projectmanagement.domain.model.experiment.VariableLevel;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.SampleOrigin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

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
  @Serial
  private static final long serialVersionUID = -8992991642015281245L;
  private final transient ExperimentInformationService experimentInformationService;
  private final SampleInformationService sampleInformationService;
  private final transient OntologyLookupService ontologyTermInformationService;
  private final Div content = new Div();
  private final Div header = new Div();
  private final Span title = new Span();
  private final Span buttonBar = new Span();
  private final Span sampleSourceComponent = new Span();
  private final TabSheet experimentSheet = new TabSheet();
  private final Div experimentalGroups = new Div();
  private final Div experimentalVariables = new Div();
  private final CardCollection experimentalGroupsCollection = new CardCollection("GROUPS");
  private final CardCollection experimentalVariableCollection = new CardCollection("VARIABLES");
  private final Disclaimer noExperimentalVariablesDefined;
  private final Disclaimer noExperimentalGroupsDefined;
  private final Disclaimer addExperimentalVariablesNote;
  private final DeletionService deletionService;
  private final MessageSource messageSource;
  private Context context;
  private int experimentalGroupCount;


  public ExperimentDetailsComponent(
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired SampleInformationService sampleInformationService,
      @Autowired DeletionService deletionService,
      @Autowired OntologyLookupService ontologyTermInformationService,
      MessageSource messageSource) {
    this.messageSource = requireNonNull(messageSource, "messageSource must not be null");
    this.experimentInformationService = requireNonNull(experimentInformationService);
    this.sampleInformationService = sampleInformationService;
    this.deletionService = requireNonNull(deletionService);
    this.ontologyTermInformationService = requireNonNull(ontologyTermInformationService);
    this.noExperimentalVariablesDefined = createNoVariableDisclaimer();
    this.noExperimentalGroupsDefined = createNoGroupsDisclaimer();
    this.addExperimentalVariablesNote = createNoVariableDisclaimer();
    this.addClassName("experiment-details-component");
    layoutComponent();
    configureComponent();
  }

  private static ComponentRenderer<Span, OntologyTerm> createOntologyRenderer() {
    return new ComponentRenderer<>(ontologyClassDTO -> {
      Span ontology = new Span();
      Span ontologyLabel = new Span(ontologyClassDTO.getLabel());
      /*Ontology terms are delimited by a column, the underscore is only used in the web environment*/
      String ontologyLinkName = ontologyClassDTO.getName().replace("_", ":");
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

    String message = messageSource.getMessage("routing.experiment.to.samples.message", null,
        getLocale());
    String linkText = messageSource.getMessage("routing.experiment.to.samples.link-text", null,
        getLocale());
    return Toast.createWithRouting(message, linkText, SampleInformationMain.class,
        new RouteParameters(projectRouteParam, experimentRouteParam));
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
    Optional<Experiment> optionalExperiment = experimentInformationService.find(
        context.projectId().orElseThrow().value(), experimentId);
    if (optionalExperiment.isEmpty()) {
      throw new ApplicationException(
          "Experiment information could not be retrieved from service");
    }

    Map<SampleOriginType, Set<OntologyTerm>> usedTerms = getOntologyTermsUsedInSamples(experimentId);

    optionalExperiment.ifPresent(experiment -> {
      EditExperimentDialog editExperimentDialog = new EditExperimentDialog(
          ontologyTermInformationService);

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
      editExperimentDialog.addCancelListener(event -> event.getSource().close());
      editExperimentDialog.open();
    });
  }

  private Map<SampleOriginType, Set<OntologyTerm>> getOntologyTermsUsedInSamples(ExperimentId experimentId) {
    Map<SampleOriginType, Set<OntologyTerm>> result = new EnumMap<>(SampleOriginType.class);
    result.put(SPECIES, new HashSet<>());
    result.put(SPECIMEN, new HashSet<>());
    result.put(ANALYTE, new HashSet<>());
    sampleInformationService.retrieveSamplesForExperiment(experimentId).onValue(samples -> {
      samples.forEach(sample -> {
        SampleOrigin origin = sample.sampleOrigin();
        result.get(SPECIES).add(origin.getSpecies());
        result.get(SPECIMEN).add(origin.getSpecimen());
        result.get(ANALYTE).add(origin.getAnalyte());
      });
    });
    return result;
  }

  private void onExperimentUpdateEvent(ExperimentUpdateEvent event) {
    ExperimentId experimentId = context.experimentId().orElseThrow();

    ExperimentDraft experimentDraft = event.getExperimentDraft();
    experimentInformationService.editExperimentInformation(
        context.projectId().orElseThrow().value(),
        experimentId,
        experimentDraft.getExperimentName(),
        experimentDraft.getSpecies(),
        experimentDraft.getSpecimens(),
        experimentDraft.getAnalytes(),
        experimentDraft.getSpeciesIcon().getLabel(),
        experimentDraft.getSpecimenIcon().getLabel());
    reloadExperimentInfo(experimentId);
    event.getSource().close();
  }

  private void listenForExperimentalVariablesComponentEvents() {
    experimentalVariableCollection.addAddListener(addEvent -> openExperimentalVariablesAddDialog());
    experimentalVariableCollection.addEditListener(
        editEvent -> openExperimentalVariablesEditDialog());
  }

  private void deleteExistingExperimentalVariables() {
    ExperimentId experimentId = context.experimentId().orElseThrow();
    ProjectId projectId = context.projectId().orElseThrow();
    var result = deletionService.deleteAllExperimentalVariables(experimentId, projectId);
    result.onError(responseCode -> {
      throw new ApplicationException("variable deletion failed: " + responseCode);
    });
  }

  private void reloadExperimentInfo(ExperimentId experimentId) {
    experimentInformationService.find(context.projectId().orElseThrow().value(), experimentId)
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
        experimentInformationService.getVariablesOfExperiment(
            context.projectId().orElseThrow().value(), experimentId));
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
        numOfExperimentalGroups, messageSource);
    existingGroupsPreventVariableEdit.addConfirmListener(
        confirmEvent -> {
          experimentSheet.setSelectedIndex(1);
          confirmEvent.getSource().close();
        });
    existingGroupsPreventVariableEdit.addCancelListener(
        cancelEvent -> cancelEvent.getSource().close());
    existingGroupsPreventVariableEdit.open();
  }

  private void addSampleSourceInformationComponent() {
    sampleSourceComponent.addClassName("sample-source-display");
    content.add(sampleSourceComponent);
  }

  private Div createSampleSourceList(String titleText, AbstractIcon<?> icon,
      List<OntologyTerm> ontologyClasses) {
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
    List<OntologyTerm> speciesTags = new ArrayList<>(experiment.getSpecies());
    List<OntologyTerm> specimenTags = new ArrayList<>(experiment.getSpecimens());
    List<OntologyTerm> analyteTags = new ArrayList<>(experiment.getAnalytes());

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
    List<ExperimentalGroup> groups = experimentInformationService.experimentalGroupsFor(
        context.projectId().orElseThrow().value(),
        context.experimentId().orElseThrow());
    Comparator<String> natOrder = Comparator.naturalOrder();
    List<ExperimentalGroupCard> experimentalGroupsCards = groups.stream()
        .sorted((g1, g2) -> natOrder.compare(g1.name(), g2.name()))
        .map(ExperimentalGroupCard::new)
        .toList();
    experimentalGroupsCollection.setContent(experimentalGroupsCards);
    this.experimentalGroupCount = experimentalGroupsCards.size();
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
    ExperimentId experimentId = context.experimentId()
        .orElseThrow(() -> new ApplicationException("no experiment id in context " + context));
    context.projectId()
        .orElseThrow(() -> new ApplicationException("no project id in context " + context));
    this.context = context;
    reloadExperimentInfo(experimentId);
  }

  private void loadExperimentInformation(Experiment experiment) {
    title.setText(experiment.getName());
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

  private void loadExperimentalVariables(Experiment experiment) {
    this.experimentalVariables.removeAll();
    // We load the experimental variables of the experiment and render them as cards
    List<ExperimentalVariable> variables = experiment.variables();
    Comparator<String> natOrder = Comparator.naturalOrder();
    List<ExperimentalVariableCard> experimentalVariableCards = variables.stream()
        .sorted((var1, var2) -> natOrder.compare(var1.name().value(), var2.name().value()))
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

  /**
   * Describes a species, specimen or analyte icon with label, type and source. Note that the icon
   * source is stored instead of the icon itself, because mixing enums and Components causes trouble
   * at runtime.
   */
  public enum BioIcon {
    DEFAULT_SPECIES("default", SampleSourceType.SPECIES, VaadinIcon.BUG),
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
    DEFAULT_SPECIMEN("default", SampleSourceType.SPECIMEN, VaadinIcon.DROP),
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
    DEFAULT_ANALYTE("default", SampleSourceType.ANALYTE, VaadinIcon.CLUSTER);
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

    public String getLabel() {
      return label;
    }

    public SampleSourceType getType() {
      return type;
    }

    public IconResource getIconResource() {
      return iconResource;
    }

    public static List<BioIcon> getOptionsForType(SampleSourceType type) {
      return Arrays.stream(BioIcon.values()).filter(o ->
          o.getType().equals(type)).toList();
    }

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

  /**
   * Describes the source level of a sample: species, specimen or analyte
   */
  public enum SampleSourceType {
    SPECIES, SPECIMEN, ANALYTE;
  }
}
