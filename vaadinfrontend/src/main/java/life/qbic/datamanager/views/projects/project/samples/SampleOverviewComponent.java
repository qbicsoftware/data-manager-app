package life.qbic.datamanager.views.projects.project.samples;

import static org.slf4j.LoggerFactory.getLogger;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.layouts.PageComponent;
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
import life.qbic.projectmanagement.application.SampleInformationService.Sample;
import life.qbic.projectmanagement.application.SampleRegistrationService;
import life.qbic.projectmanagement.application.SampleRegistrationService.ResponseCode;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.sample.BatchId;
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

  //ToDo Remove showEmptyViewButton once sample information can be loaded
  private final Button showEmptyViewButton = new Button("Empty View");
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
    buttonBar.add(showEmptyViewButton, registerButton, metadataDownloadButton);
    fieldBar.setAlignSelf(Alignment.START, buttonAndFieldBar);
    buttonBar.setAlignSelf(Alignment.END, buttonAndFieldBar);
    fieldBar.setAlignItems(Alignment.END);
    buttonBar.setAlignItems(Alignment.END);
    //Moves buttonbar to right side of sample grid
    fieldBar.setWidthFull();
    buttonAndFieldBar.add(fieldBar, buttonBar);
    buttonAndFieldBar.setWidthFull();
  }

  private void createSampleTabs(Map<Experiment, Collection<Sample>> experimentSamples) {
    experimentSamples.forEach((experiment, samples) -> {
      Span experimentName = new Span(experiment.getName());
      Tab experimentSampleTab = new Tab(experimentName);
      //Todo How to provide information from different services in the vaadin grid?
      Grid<Sample> sampleGrid = new Grid<>(Sample.class, false);
      sampleGrid.addColumn(createSampleIdComponentRenderer()).setComparator(Sample::id)
          .setHeader("Sample Id");
      sampleGrid.addColumn(Sample::label, "label").setHeader("Sample Label");
      sampleGrid.addColumn(Sample::batch, "batch").setHeader("Batch");
      sampleGrid.addColumn(createSampleStatusComponentRenderer()).setComparator(Sample::status)
          .setHeader("Status");
      sampleGrid.addColumn(Sample::experiment, "experiment").setHeader("Experiment");
      sampleGrid.addColumn(Sample::source, "source").setHeader("Sample Source");
      sampleGrid.addColumn(Sample::condition1, "condition1").setHeader("Brushing Time");
      sampleGrid.addColumn(Sample::condition2, "condition2").setHeader("Tooth Paste");
      sampleGrid.addColumn(Sample::species, "species").setHeader("Species");
      sampleGrid.addColumn(Sample::specimen, "specimen").setHeader("Specimen");
      //ToDo make this virtual list with data Providers and implement lazy loading?
      GridListDataView<Sample> sampleGridDataView = sampleGrid.setItems(samples);
      sampleOverviewComponentHandler.setupSearchFieldForExperimentTabs(experiment.getName(),
          sampleGridDataView);
      //Update Number count in tab if user searches for value
      sampleGridDataView.addItemCountChangeListener(event -> {
        Span sampleCount = new Span(createBadge(sampleGridDataView.getItemCount()));
        experimentSampleTab.removeAll();
        experimentSampleTab.add(experimentName, sampleCount);
      });
      sampleExperimentTabSheet.add(experimentSampleTab, sampleGrid);
    });
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

  private static ComponentRenderer<Span, Sample> createSampleStatusComponentRenderer() {
    return new ComponentRenderer<>(Span::new, styleSampleStatusSpan);
  }

  private static final SerializableBiConsumer<Span, Sample> styleSampleStatusSpan = (span, sample) -> {
    String theme = setBadgeThemeBasedOnSampleStatus(sample);
    span.getElement().setAttribute("theme", theme);
    span.setText(sample.status());
  };

  private static String setBadgeThemeBasedOnSampleStatus(Sample sample) {
    if (sample.status().equals("Data Available")) {
      return "badge success";
    }
    if (sample.status().equals("QC Failed")) {
      return "badge error";
    } else {
      return "badge ";
    }
  }

  private static ComponentRenderer<Anchor, Sample> createSampleIdComponentRenderer() {
    return new ComponentRenderer<>(Anchor::new, styleSampleIdAnchor);
  }

  private static final SerializableBiConsumer<Anchor, Sample> styleSampleIdAnchor = (anchor, sample) -> {
    //ToDo maybe the projectId could be read from the UI URL?
    String anchorURL = String.format(Projects.MEASUREMENT, projectId.value(), sample.id());
    anchor.setHref(anchorURL);
    anchor.setText(sample.id());
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
      configureBatchRegistrationDialog();
    }

    public void setProjectId(ProjectId projectId) {
      this.projectId = projectId;
      Optional<Project> potentialProject = projectInformationService.find(projectId);
      if(potentialProject.isPresent()) {
        Project project = potentialProject.get();

        generateExperimentTabs(project);
        Optional<Experiment> potentialExperiment = experimentInformationService.find(
            project.activeExperiment());
        potentialExperiment.ifPresent(
            batchRegistrationDialog::setActiveExperiment);
      }
    }

    private boolean hasExperimentalGroupsDefined() {
      Optional<Project> potentialProject = projectInformationService.find(projectId);
      if (potentialProject.isPresent()) {
        Project project = potentialProject.get();
        return !experimentInformationService.getExperimentalGroups(project.activeExperiment())
            .isEmpty();
      }
      return false;
    }

    private void registerSamplesListener() {
      registerBatchButton.addClickListener(event -> {
        if(hasExperimentalGroupsDefined()) {
          batchRegistrationDialog.open();
        } else {
          InformationMessage infoMessage = new InformationMessage("No experimental groups are defined",
              "You need to define experimental groups before adding samples.");
          StyledNotification notification = new StyledNotification(infoMessage);
          notification.open();
        }
      });
      //ToDo Replace with received samples from SampleInformationService
      showEmptyViewButton.addClickListener(event -> showEmptyView());
    }

    private void configureBatchRegistrationDialog() {

      batchRegistrationDialog.addBatchRegistrationEventListener(
          event -> processBatchRegistration(event.getSource().batchRegistrationContent()));
      batchRegistrationDialog.addSampleRegistrationEventListener(
          event -> processSampleRegistration(event.getSource().sampleRegistrationContent()));
      batchRegistrationDialog.addCancelEventListener(
          event -> batchRegistrationDialog.resetAndClose());
    }

    private void generateExperimentTabs(Project project) {
      resetTabSheet();
      resetTabSelect();
      List<Experiment> foundExperiments = new LinkedList<>();
      project.experiments().forEach(experimentId -> experimentInformationService.find(experimentId)
          .ifPresent(foundExperiments::add));
      Map<Experiment, Collection<Sample>> experimentToSampleDict = new HashMap<>();
      //ToDo retrieve sample information as soon as it's clear how they are linked
      for (Experiment experiment : foundExperiments) {
        experimentToSampleDict.put(experiment,
            sampleInformationService.retrieveSamplesForExperiment(experiment.experimentId()));
      }
      addExperimentsToTabSelect(experimentToSampleDict.keySet().stream().toList());
      createSampleTabs(experimentToSampleDict);
    }

    private void resetTabSheet() {
      sampleExperimentTabSheet.getChildren().toList().forEach(sampleExperimentTabSheet::remove);
    }

    private void resetTabSelect() {
      tabFilterSelect.removeAll();
    }

    private void showEmptyView() {
      sampleContentLayout.setVisible(false);
      noBatchDefinedLayout.setVisible(true);
    }

    private void showSamplesView() {
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


    private void processBatchRegistration(BatchRegistrationContent batchRegistrationContent) {
      //Todo add Batch name here and trigger processSampleCreation() method
      Result<BatchId, BatchRegistrationService.ResponseCode> batch = batchRegistrationService.registerBatch(
          batchRegistrationContent.batchLabel(), batchRegistrationContent.isPilot());
      batch
          .onValue(result -> {
            batchRegistrationDialog.resetAndClose();
            //ToDo Replace Values
          })
          .onError(e -> {
            //ToDo What should happen here?
          });
    }

    private void processSampleRegistration(SampleRegistrationContent sampleRegistrationContent) {

      sampleRegistrationContent.sampleRegistrationRequests().forEach(sampleRegistrationRequest -> {
        Result<life.qbic.projectmanagement.domain.project.sample.Sample, ResponseCode> registrationResult = sampleRegistrationService.registerSample(
            sampleRegistrationRequest, projectId);
        registrationResult.onError(e -> {
          //Todo What should happen here
        });
      });
      showSamplesView();
      displaySuccessfulBatchRegistrationNotification();
    }

    private void displaySuccessfulBatchRegistrationNotification() {
      SuccessMessage successMessage = new SuccessMessage("Batch registration succeeded.", "");
      StyledNotification notification = new StyledNotification(successMessage);
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


}
