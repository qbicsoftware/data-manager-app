package life.qbic.datamanager.views.project.view.sample;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
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
public class SampleOverviewComponent extends Composite<CardLayout> implements Serializable {

  private final String TITLE = "Samples";
  @Serial
  private static final long serialVersionUID = 2893730975944372088L;
  private final transient SampleOverviewComponentHandler sampleOverviewComponentHandler;
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

  public SampleOverviewComponent(@Autowired ProjectInformationService projectInformationService,
      @Autowired ExperimentInformationService experimentInformationService,
      @Autowired SampleInformationService sampleInformationService) {
    Objects.requireNonNull(projectInformationService);
    Objects.requireNonNull(experimentInformationService);
    Objects.requireNonNull(sampleInformationService);
    getContent().addTitle(TITLE);
    initEmptyView();
    initSampleView();
    getContent().setSizeFull();
    this.sampleOverviewComponentHandler = new SampleOverviewComponentHandler(
        projectInformationService, experimentInformationService, sampleInformationService);
  }

  public void projectId(String parameter) {
    this.sampleOverviewComponentHandler.setProjectId(ProjectId.parse(parameter));
  }

  private void initEmptyView() {
    Span templateText = new Span("No Samples Registered");
    registerBatchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    noBatchDefinedLayout.add(templateText, registerBatchButton);
    noBatchDefinedLayout.setAlignItems(Alignment.CENTER);
    noBatchDefinedLayout.setSizeFull();
    getContent().addFields(noBatchDefinedLayout);
  }

  private void initSampleView() {
    sampleExperimentTabSheet = new TabSheet();
    sampleExperimentTabSheet.setSizeFull();
    initButtonAndFieldBar();
    sampleContentLayout.add(buttonAndFieldBar);
    sampleContentLayout.add(sampleExperimentTabSheet);
    getContent().addFields(sampleContentLayout);
    sampleContentLayout.setVisible(false);
  }

  private void initButtonAndFieldBar() {
    searchField.setPlaceholder("Search");
    searchField.setClassName("searchbox");
    tabFilterSelect.setLabel("Search in");
    registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    fieldBar.add(searchField, tabFilterSelect);
    fieldBar.setAlignItems(Alignment.END);
    buttonBar.add(showEmptyViewButton, registerButton, metadataDownloadButton);
    buttonAndFieldBar.add(fieldBar, buttonBar);
    buttonAndFieldBar.setVerticalComponentAlignment(Alignment.END, buttonBar);
    buttonAndFieldBar.setVerticalComponentAlignment(Alignment.START, fieldBar);
    buttonAndFieldBar.setWidthFull();
  }

  private void createSampleTab(String experimentName, Collection<Sample> experimentSamples) {
    Tab experimentSampleTab = new Tab(new Span(experimentName),
        createBadge(experimentSamples.size()));
    //Todo Load Sample Information
    Grid<Sample> sampleGrid = new Grid<>(Sample.class, false);
    sampleGrid.addColumn(Sample::id, "id").setHeader("id");
    sampleGrid.addColumn(Sample::label, "label").setHeader("label");
    sampleGrid.addColumn(Sample::batch, "batch").setHeader("batch");
    sampleGrid.addColumn(Sample::status, "status").setHeader("status");
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

  public void setStyles(String... componentStyles) {
    getContent().addClassNames(componentStyles);
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
      projectInformationService.find(projectId.value())
          .ifPresentOrElse(this::getSampleDataForProject, this::emptyAction);
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
      //ToDo Currently we set the ProjectId multiple times leading to an addition of tabs within the tabSheet
      project.experiments().forEach(experimentId -> {
        //ToDo retrieve sample information as soon as it's clear how they are linked
        Collection<Sample> samples = sampleInformationService.retrieveSamplesForExperiment(
            experimentId);
        experimentInformationService.find(experimentId.value())
            .ifPresent(experiment -> createSampleTab(experiment.getName(), samples));
      });
    }

    private void showEmptyView() {
      sampleContentLayout.setVisible(false);
      noBatchDefinedLayout.setVisible(true);
    }

    private void showSamplesView() {
      noBatchDefinedLayout.setVisible(false);
      sampleContentLayout.setVisible(true);
    }

    //ToDo what should happen in the UI if neither projects or samples have been found?
    private void emptyAction() {
    }
  }


}
