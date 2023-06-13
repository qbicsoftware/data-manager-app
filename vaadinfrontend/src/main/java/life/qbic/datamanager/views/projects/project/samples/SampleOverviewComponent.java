package life.qbic.datamanager.views.projects.project.samples;

import static org.slf4j.LoggerFactory.getLogger;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.beans.PropertyDescriptor;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.layouts.PageComponent;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.InformationMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.datamanager.views.projects.project.ProjectViewPage;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationContent;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleRegistrationContent;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.SampleInformationService;
import life.qbic.projectmanagement.application.SampleRegistrationService;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService.ResponseCode;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.project.experiment.VariableLevel;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;
import life.qbic.projectmanagement.domain.project.sample.BatchId;
import life.qbic.projectmanagement.domain.project.sample.Sample;
import life.qbic.projectmanagement.domain.project.sample.SampleOrigin;
import life.qbic.projectmanagement.domain.project.sample.SampleRegistrationRequest;
import org.slf4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sample Overview Component
 * <p>
 * Component embedded within the {@link SampleInformationPage} in the {@link ProjectViewPage}. It
 * allows the user to see the information associated for all {@link Sample} for each
 * {@link Experiment within a {@link Project}
 */

@SpringComponent
@UIScope
public class SampleOverviewComponent extends PageComponent implements Serializable {

  private final String TITLE = "Samples";
  @Serial
  private static final long serialVersionUID = 2893730975944372088L;
  private final VerticalLayout noBatchDefinedLayout = new VerticalLayout();
  private final Button registerBatchButton = new Button("Register Batch");
  private final VerticalLayout sampleContentLayout = new VerticalLayout();
  private final HorizontalLayout buttonAndFieldBar = new HorizontalLayout();
  private final HorizontalLayout fieldBar = new HorizontalLayout();
  private final HorizontalLayout buttonBar = new HorizontalLayout();
  private final TextField searchField = new TextField();
  private final Select<String> tabFilterSelect = new Select<>();
  private final Button registerButton = new Button("Register");
  private final Button metadataDownloadButton = new Button("Download Metadata");
  private final TabSheet sampleExperimentTabSheet = new TabSheet();
  private static ProjectId projectId;
  private static final Logger log = getLogger(SampleOverviewComponent.class);
  private final BatchRegistrationDialog batchRegistrationDialog = new BatchRegistrationDialog();
  private final transient SampleOverviewComponentHandler sampleOverviewComponentHandler;

  public SampleOverviewComponent(@Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired SampleInformationService sampleInformationService,
      @Autowired SampleRegistrationService sampleRegistrationService,
      @Autowired BatchRegistrationService batchRegistrationService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    Objects.requireNonNull(sampleInformationService);
    Objects.requireNonNull(batchRegistrationService);
    addTitle(TITLE);
    initEmptyView();
    initSampleView();
    setSizeFull();
    this.sampleOverviewComponentHandler = new SampleOverviewComponentHandler(
        projectInformationService, experimentInformationService, sampleInformationService,
        sampleRegistrationService, batchRegistrationService);
  }

  public void projectId(ProjectId projectId) {
    SampleOverviewComponent.projectId = projectId;
    this.sampleOverviewComponentHandler.setProjectId(projectId);
  }

  private void initEmptyView() {
    Span templateHeader = new Span("No Samples Registered");
    templateHeader.addClassName("font-bold");
    Span templateText = new Span("Start your project by registering the first sample batch");
    registerBatchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    noBatchDefinedLayout.add(templateHeader, templateText, registerBatchButton);
    noBatchDefinedLayout.setAlignItems(Alignment.CENTER);
    noBatchDefinedLayout.setJustifyContentMode(JustifyContentMode.CENTER);
    noBatchDefinedLayout.setSizeFull();
    noBatchDefinedLayout.setMinWidth(100, Unit.PERCENTAGE);
    noBatchDefinedLayout.setMinHeight(100, Unit.PERCENTAGE);
    addContent(noBatchDefinedLayout);
  }

  private void initSampleView() {
    sampleExperimentTabSheet.setSizeFull();
    initButtonAndFieldBar();
    sampleContentLayout.add(buttonAndFieldBar);
    sampleContentLayout.add(sampleExperimentTabSheet);
    addContent(sampleContentLayout);
    sampleContentLayout.setSizeFull();
    sampleContentLayout.setVisible(false);
  }

  private void initButtonAndFieldBar() {
    searchField.setPlaceholder("Search");
    searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
    searchField.setValueChangeMode(ValueChangeMode.EAGER);
    tabFilterSelect.setLabel("Search in");
    tabFilterSelect.setEmptySelectionAllowed(true);
    tabFilterSelect.setEmptySelectionCaption("All tabs");
    registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    fieldBar.add(searchField, tabFilterSelect);
    //Items in layout should be aligned at the end due to searchFieldLabel taking up space
    buttonBar.add(registerButton, metadataDownloadButton);
    fieldBar.setAlignSelf(Alignment.START, buttonAndFieldBar);
    buttonBar.setAlignSelf(Alignment.END, buttonAndFieldBar);
    fieldBar.setAlignItems(Alignment.END);
    buttonBar.setAlignItems(Alignment.END);
    //Moves buttonbar to right side of sample grid
    fieldBar.setWidthFull();
    buttonAndFieldBar.add(fieldBar, buttonBar);
    buttonAndFieldBar.setWidthFull();
  }

  private Grid<Sample> createSampleGrid() {
    Grid<Sample> sampleGrid = new Grid<>(Sample.class, false);
    sampleGrid.addColumn(createSampleIdComponentRenderer()).setHeader("Sample Id");
    sampleGrid.addColumn(Sample::label).setHeader("Sample Label");
    sampleGrid.addColumn(sample -> sample.assignedBatch().value()).setHeader("Batch");
    sampleGrid.addColumn(sample -> sample.getBiologicalReplicateId().id())
        .setHeader("Sample Source");
    sampleGrid.addColumn(sample -> sample.sampleOrigin().getSpecies().value()).setHeader("Species");
    sampleGrid.addColumn(sample -> sample.sampleOrigin().getSpecimen().value())
        .setHeader("Specimen");
    sampleGrid.addColumn(sample -> sample.sampleOrigin().getAnalyte().value()).setHeader("Analyte");
    //ToDo Replace with Experimental Group information
    sampleGrid.addColumn(Sample::getExperimentalGroupId).setHeader("Experimental Group Id");
    //ToDo make this virtual list with data Providers and implement lazy loading?
    return sampleGrid;
  }

  private void createExperimentTabs(Map<Experiment, Collection<Sample>> experimentSamples) {
    experimentSamples.forEach((experiment, samples) -> {
      SampleExperimentTab experimentTab = new SampleExperimentTab(experiment.getName(),
          samples.size());
      Grid<Sample> sampleGrid = createSampleGrid();
      generateConditionColumnHeaders(experiment);
      GridListDataView<Sample> sampleGridDataView = sampleGrid.setItems(samples);
      sampleOverviewComponentHandler.setupSearchFieldForExperimentTabs(experiment.getName(),
          sampleGridDataView);
      sampleExperimentTabSheet.add(experimentTab, sampleGrid);
      //Update Number count in tab if user searches for value
      sampleGridDataView.addItemCountChangeListener(
          event -> experimentTab.setSampleCount(event.getItemCount()));
    });
  }

  private List<String> generateConditionColumnHeaders(Experiment experiment) {
    Set<String> uniqueVariableNames = new HashSet<>();
    for (ExperimentalGroup experimentalGroup : experiment.getExperimentalGroups()) {
      for (VariableLevel level : experimentalGroup.condition().getVariableLevels()) {
        uniqueVariableNames.add(level.variableName().value());
      }
    }
    return uniqueVariableNames.stream().toList();
  }

  private static ComponentRenderer<Anchor, Sample> createSampleIdComponentRenderer() {
    return new ComponentRenderer<>(Anchor::new, styleSampleIdAnchor);
  }

  private static final SerializableBiConsumer<Anchor, Sample> styleSampleIdAnchor = (anchor, sample) -> {
    String anchorURL = String.format(Projects.MEASUREMENT, projectId.value(),
        sample.sampleId().value());
    anchor.setHref(anchorURL);
    anchor.setText(sample.sampleCode().code());
  };

  public void setStyles(String... componentStyles) {
    addClassNames(componentStyles);
  }

  private final class SampleOverviewComponentHandler {

    private final ProjectInformationService projectInformationService;
    private final ExperimentInformationService experimentInformationService;
    private final SampleInformationService sampleInformationService;
    private final SampleRegistrationService sampleRegistrationService;
    private final BatchRegistrationService batchRegistrationService;
    private ProjectId projectId;
    private ExperimentId experimentId;

    //ToDo Replace with Call to service
    private final Map<Experiment, Collection<Sample>> experimentToSampleDict = new HashMap<>();

    public SampleOverviewComponentHandler(ProjectInformationService projectInformationService,
        ExperimentInformationService experimentInformationService,
        SampleInformationService sampleInformationService,
        SampleRegistrationService sampleRegistrationService,
        BatchRegistrationService batchRegistrationService) {
      this.projectInformationService = projectInformationService;
      this.experimentInformationService = experimentInformationService;
      this.sampleInformationService = sampleInformationService;
      this.sampleRegistrationService = sampleRegistrationService;
      this.batchRegistrationService = batchRegistrationService;
      registerSamplesListener();
      addEventListeners();
    }

    public void setProjectId(ProjectId projectId) {
      this.projectId = projectId;
      Optional<Project> potentialProject = projectInformationService.find(projectId);
      if (potentialProject.isPresent()) {
        Project project = potentialProject.get();
        generateExperimentTabs(project);
        Optional<Experiment> potentialExperiment = experimentInformationService.find(
            project.activeExperiment());
        potentialExperiment.ifPresent(experiment -> {
          batchRegistrationDialog.setActiveExperiment(experiment);
          this.experimentId = experiment.experimentId();
        });
      }
    }

    private boolean hasExperimentalGroupsDefined() {
      boolean areExperimentalGroupsDefined = experimentToSampleDict.keySet().forEach(
          experiment -> experimentInformationService.getExperimentalGroups(experimentId).stream()
              .findFirst().isPresent());
      //ToDo check if any experiment has an experimentalgroup.
    }

    private void registerSamplesListener() {
      registerBatchButton.addClickListener(this::openBatchRegistrationDialog);
      registerButton.addClickListener(this::openBatchRegistrationDialog);
    }

    private void openBatchRegistrationDialog(ComponentEvent<?> componentEvent) {
      if (hasExperimentalGroupsDefined()) {
        batchRegistrationDialog.open();
      } else {
        InformationMessage infoMessage = new InformationMessage(
            "No experimental groups are defined",
            "You need to define experimental groups before adding samples.");
        StyledNotification notification = new StyledNotification(infoMessage);
        notification.open();
      }
    }

    private void addEventListeners() {
      batchRegistrationDialog.addBatchRegistrationEventListener(batchRegistrationEvent -> {
        BatchRegistrationDialog batchRegistrationSource = batchRegistrationEvent.getSource();
        registerBatchAndSamples(batchRegistrationSource.batchRegistrationContent(),
            batchRegistrationSource.sampleRegistrationContent()).onValue(batchId -> {
          batchRegistrationDialog.resetAndClose();
          displayRegistrationSuccess();
          displaySampleView();
        });
      });
      batchRegistrationDialog.addCancelEventListener(
          event -> batchRegistrationDialog.resetAndClose());
    }

    private void generateExperimentTabs(Project project) {
      resetTabSheet();
      resetTabSelect();
      experimentToSampleDict.clear();
      List<Experiment> foundExperiments = new LinkedList<>();
      project.experiments().forEach(experimentId -> experimentInformationService.find(experimentId)
          .ifPresent(foundExperiments::add));
      for (Experiment experiment : foundExperiments) {
        sampleInformationService.retrieveSamplesForExperiment(experiment.experimentId())
            .onValue(samples -> {
              if (samples.size() > 0) {
                experimentToSampleDict.put(experiment, samples);
              }
            });
      }
      addExperimentsToTabSelect(experimentToSampleDict.keySet().stream().toList());
      createExperimentTabs(experimentToSampleDict);
      displaySampleView();
    }

    private boolean areSamplesInExperiments() {
      return !experimentToSampleDict.isEmpty();
    }

    private void resetTabSelect() {
      tabFilterSelect.removeAll();
    }

    private void resetTabSheet() {
      sampleExperimentTabSheet.getChildren()
          .forEach(component -> component.getElement().removeAllChildren());
    }

    private void displaySampleView() {
      if (areSamplesInExperiments()) {
        displaySampleGrid();
      } else {
        displayEmptyView();
      }
    }

    private void displayEmptyView() {
      sampleContentLayout.setVisible(false);
      noBatchDefinedLayout.setVisible(true);
    }

    private void displaySampleGrid() {
      noBatchDefinedLayout.setVisible(false);
      sampleContentLayout.setVisible(true);
    }

    private void addExperimentsToTabSelect(List<Experiment> experimentList) {
      tabFilterSelect.setItems(experimentList.stream().map(Experiment::getName).toList());
    }

    private void setupSearchFieldForExperimentTabs(String experimentName,
        GridListDataView<Sample> sampleGridDataView) {
      searchField.addValueChangeListener(e -> sampleGridDataView.refreshAll());
      sampleGridDataView.addFilter(sample -> {
        String searchTerm = searchField.getValue().trim();
        //Only filter grid if selected in filterSelect or if no filter was selected
        if (tabFilterSelect.getValue() == null || tabFilterSelect.getValue()
            .equals(experimentName)) {
          return isInSample(sample, searchTerm);
        } else {
          return true;
        }
      });
    }


    private Result<?, ?> registerBatchAndSamples(BatchRegistrationContent batchRegistrationContent,
        List<SampleRegistrationContent> sampleRegistrationContent) {
      return registerBatchInformation(batchRegistrationContent).onValue(
          batchId -> {
            List<SampleRegistrationRequest> sampleRegistrationsRequests = createSampleRegistrationRequests(
                batchId, sampleRegistrationContent);
            registerSamples(sampleRegistrationsRequests);
          });
    }

    private Result<BatchId, ResponseCode> registerBatchInformation(
        BatchRegistrationContent batchRegistrationContent) {
      return batchRegistrationService.registerBatch(batchRegistrationContent.batchLabel(),
          batchRegistrationContent.isPilot()).onError(responseCode -> displayRegistrationFailure());
    }

    private void registerSamples(List<SampleRegistrationRequest> sampleRegistrationRequests) {
      sampleRegistrationService.registerSamples(sampleRegistrationRequests, projectId)
          .onError(responseCode -> displayRegistrationFailure());
    }

    private List<SampleRegistrationRequest> createSampleRegistrationRequests(BatchId batchId,
        List<SampleRegistrationContent> sampleRegistrationContents) {
      return sampleRegistrationContents.stream()
          .map(sampleRegistrationContent -> {
            Analyte analyte = new Analyte(sampleRegistrationContent.analyte());
            Specimen specimen = new Specimen(sampleRegistrationContent.specimen());
            Species species = new Species(sampleRegistrationContent.species());
            SampleOrigin sampleOrigin = SampleOrigin.create(species, specimen, analyte);
            return new SampleRegistrationRequest(sampleRegistrationContent.label(), batchId,
                experimentId,
                sampleRegistrationContent.experimentalGroupId(),
                sampleRegistrationContent.biologicalReplicateId(), sampleOrigin);
          }).toList();
    }

    private void displayRegistrationSuccess() {
      SuccessMessage successMessage = new SuccessMessage("Batch registration succeeded.", "");
      StyledNotification notification = new StyledNotification(successMessage);
      notification.open();
    }

    private void displayRegistrationFailure() {
      ErrorMessage errorMessage = new ErrorMessage("Batch registration failed.", "");
      StyledNotification notification = new StyledNotification(errorMessage);
      notification.open();
    }
  }

  private boolean isInSample(Sample sample, String searchTerm) {
    boolean result = false;
    for (PropertyDescriptor descriptor : BeanUtils.getPropertyDescriptors(Sample.class)) {
      if (!descriptor.getName().equals("class")) {
        try {
          String value = descriptor.getReadMethod().invoke(sample).toString();
          result |= matchesTerm(value, searchTerm);
        } catch (IllegalAccessException | InvocationTargetException e) {
          log.info("Could not invoke " + descriptor.getName()
              + " getter when filtering samples. Ignoring property.");
        }
      }
    }
    return result;
  }

  private boolean matchesTerm(String fieldValue, String searchTerm) {
    return fieldValue.toLowerCase().contains(searchTerm.toLowerCase());
  }


  private class SampleExperimentTab extends Tab {

    private final Span sampleCountComponent;
    private final Span experimentNameComponent;

    public SampleExperimentTab(String experimentName, int sampleCount) {
      this.experimentNameComponent = new Span(experimentName);
      this.sampleCountComponent = createBadge(sampleCount);
      this.add(experimentNameComponent, sampleCountComponent);
    }

    public String getExperimentName() {
      return experimentNameComponent.getText();
    }

    public void setExperimentName(String experimentName) {
      experimentNameComponent.setText(experimentName);
    }

    public int getSampleCount() {
      return Integer.parseInt(sampleCountComponent.getText());
    }

    public void setSampleCount(int sampleCount) {
      sampleCountComponent.setText(Integer.toString(sampleCount));
    }

    /**
     * Helper method for creating a badge.
     */
    private Span createBadge(int numberOfSamples) {
      Span badge = new Span(String.valueOf(numberOfSamples));
      badge.getElement().getThemeList().add("badge small contrast");
      badge.getStyle().set("margin-inline-start", "var(--lumo-space-xs)");
      return badge;
    }
  }
}
