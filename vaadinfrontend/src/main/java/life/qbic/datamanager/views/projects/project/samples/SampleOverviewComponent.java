package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.layouts.CardLayout;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.ProjectInformationService;
import life.qbic.projectmanagement.application.SampleInformationService;
import life.qbic.projectmanagement.application.SampleInformationService.Sample;
import life.qbic.projectmanagement.domain.project.Project;
import life.qbic.projectmanagement.domain.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <class short description - One Line!>
 * <p>
 * <More detailed description - When to use, what it solves, etc.>
 *
 * @since <version tag>
 */

@SpringComponent
@UIScope
public class SampleOverviewComponent extends CardLayout implements Serializable {

  private final String TITLE = "Samples";
  @Serial
  private static final long serialVersionUID = 2893730975944372088L;
  private final VerticalLayout noBatchDefinedLayout = new VerticalLayout();
  private final Button registerBatchButton = new Button("Register Batch");
  private final VerticalLayout sampleContentLayout = new VerticalLayout();
  private final HorizontalLayout buttonAndFieldBar = new HorizontalLayout();
  private final HorizontalLayout fieldBar = new HorizontalLayout();
  private final HorizontalLayout buttonBar = new HorizontalLayout();
  private final ComboBox<Sample> searchField = new ComboBox<>();
  private final Select<TabSheet> tabFilterSelect = new Select<>();
  private final Button registerButton = new Button("Register");
  private final Button metadataDownloadButton = new Button("Download Metadata");

  //ToDo Remove showEmptyViewButton once sample information can be loaded
  private final Button showEmptyViewButton = new Button("Empty View");
  private TabSheet sampleExperimentTabSheet;
  private static ProjectId projectId;
  private final transient SampleOverviewComponentHandler sampleOverviewComponentHandler;

  public SampleOverviewComponent(@Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired SampleInformationService sampleInformationService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    Objects.requireNonNull(sampleInformationService);
    addTitle(TITLE);
    initEmptyView();
    initSampleView();
    setSizeFull();
    this.sampleOverviewComponentHandler = new SampleOverviewComponentHandler(
        projectInformationService, experimentInformationService, sampleInformationService);
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
    addFields(noBatchDefinedLayout);
  }

  private void initSampleView() {
    sampleExperimentTabSheet = new TabSheet();
    sampleExperimentTabSheet.setSizeFull();
    initButtonAndFieldBar();
    sampleContentLayout.add(buttonAndFieldBar);
    sampleContentLayout.add(sampleExperimentTabSheet);
    addFields(sampleContentLayout);
    sampleContentLayout.setSizeFull();
    sampleContentLayout.setVisible(false);
  }

  private void initButtonAndFieldBar() {
    searchField.setPlaceholder("Search");
    searchField.setClassName("searchbox");
    tabFilterSelect.setLabel("Search in");
    registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    fieldBar.add(searchField, tabFilterSelect);
    //Items in layout should be aligned at the end due to searchFieldLabel taking up space
    buttonBar.add(showEmptyViewButton, registerButton, metadataDownloadButton);
    fieldBar.setAlignSelf(Alignment.START, buttonAndFieldBar);
    buttonBar.setAlignSelf(Alignment.END, buttonAndFieldBar);
    fieldBar.setAlignItems(Alignment.END);
    buttonBar.setAlignItems(Alignment.END);
    buttonAndFieldBar.add(fieldBar, buttonBar);

    buttonAndFieldBar.setWidthFull();
  }

  private void createSampleTab(String experimentName, Collection<Sample> experimentSamples) {
    Tab experimentSampleTab = new Tab(new Span(experimentName),
        createBadge(experimentSamples.size()));
    //Todo How to provide information from different services in the vaadin grid?
    Grid<Sample> sampleGrid = new Grid<>(Sample.class, false);
    sampleGrid.addColumn(createSampleIdComponentRenderer()).setSortProperty("id")
        .setHeader("Sample Id");
    sampleGrid.addColumn(Sample::label, "label").setHeader("Sample Label");
    sampleGrid.addColumn(Sample::batch, "batch").setHeader("Batch");
    sampleGrid.addColumn(createSampleStatusComponentRenderer()).setSortProperty("status")
        .setHeader("Status");
    sampleGrid.addColumn(Sample::experiment, "experiment").setHeader("Experiment");
    sampleGrid.addColumn(Sample::source, "source").setHeader("Sample Source");
    sampleGrid.addColumn(Sample::condition1, "condition1").setHeader("Brushing Time");
    sampleGrid.addColumn(Sample::condition2, "condition2").setHeader("Tooth Paste");
    sampleGrid.addColumn(Sample::species, "species").setHeader("Species");
    sampleGrid.addColumn(Sample::specimen, "specimen").setHeader("Specimen");
    //ToDo make this virtual list with data Providers and implement lazy loading?
    sampleGrid.setItems(experimentSamples);
    sampleExperimentTabSheet.add(experimentSampleTab, sampleGrid);
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
    private ProjectId projectId;

    public SampleOverviewComponentHandler(ProjectInformationService projectInformationService,
        ExperimentInformationService experimentInformationService,
        SampleInformationService sampleInformationService) {
      this.projectInformationService = projectInformationService;
      this.experimentInformationService = experimentInformationService;
      this.sampleInformationService = sampleInformationService;
      registerSamplesListener();
    }

    public void setProjectId(ProjectId projectId) {
      this.projectId = projectId;
      projectInformationService.find(projectId).ifPresent(this::getSampleDataForProject);
    }

    private void getSampleDataForProject(Project project) {
      generateExperimentTabs(project);
    }

    //ToDo Replace with received samples from SampleInformationService
    private void registerSamplesListener() {
      registerBatchButton.addClickListener(event -> showSamplesView());
      showEmptyViewButton.addClickListener(event -> showEmptyView());
    }

    private void generateExperimentTabs(Project project) {
      resetTabSheet();
      project.experiments().forEach(experimentId -> {
        //ToDo retrieve sample information as soon as it's clear how they are linked
        Collection<Sample> samples = sampleInformationService.retrieveSamplesForExperiment(
            experimentId);
        experimentInformationService.find(experimentId)
            .ifPresent(experiment -> createSampleTab(experiment.getName(), samples));
      });
    }

    private void resetTabSheet() {
      sampleExperimentTabSheet.getChildren().toList()
          .forEach(component -> sampleExperimentTabSheet.remove(component));
    }

    private void showEmptyView() {
      sampleContentLayout.setVisible(false);
      noBatchDefinedLayout.setVisible(true);
    }

    private void showSamplesView() {
      noBatchDefinedLayout.setVisible(false);
      sampleContentLayout.setVisible(true);
    }
  }


}
