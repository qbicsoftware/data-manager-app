package life.qbic.datamanager.views.projects.project.samples;

import static org.slf4j.LoggerFactory.getLogger;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import life.qbic.application.commons.Result;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.general.DisclaimerCard;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.datamanager.views.projects.project.experiments.experiment.Tag;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationContent;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationEvent;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.SampleRegistrationContent;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService.ResponseCode;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.application.sample.SampleRegistrationService;
import life.qbic.projectmanagement.domain.project.ProjectId;
import life.qbic.projectmanagement.domain.project.experiment.Experiment;
import life.qbic.projectmanagement.domain.project.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Analyte;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Species;
import life.qbic.projectmanagement.domain.project.experiment.vocabulary.Specimen;
import life.qbic.projectmanagement.domain.project.sample.Batch;
import life.qbic.projectmanagement.domain.project.sample.BatchId;
import life.qbic.projectmanagement.domain.project.sample.Sample;
import life.qbic.projectmanagement.domain.project.sample.SampleOrigin;
import life.qbic.projectmanagement.domain.project.sample.SampleRegistrationRequest;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sample Details Component
 * <p>
 * Component embedded within the {@link SampleInformationMain} in the {@link ProjectViewPage}. It
 * allows the user to see the information associated for all {@link Batch} and {@link Sample} of
 * each
 * {@link Experiment within a {@link life.qbic.projectmanagement.domain.project.Project}
 * Additionally it enables the user to register new {@link Batch} and {@link Sample} via the
 * contained {@link BatchRegistrationDialog} and propagates the successful registration to the
 * registered {@link BatchRegistrationListener} within this component.
 */

@SpringComponent
@UIScope
public class SampleDetailsComponent extends PageArea implements Serializable {

  @Serial
  private static final long serialVersionUID = 2893730975944372088L;
  private final Div content = new Div();
  private final Span title = new Span("Samples");
  private final Div buttonAndFieldBar = new Div();
  private final Span fieldBar = new Span();
  private final Span buttonBar = new Span();
  private final TextField searchField = new TextField();
  private String samplePreviewFilter = "";
  public final Button registerButton = new Button("Register");
  private final Button metadataDownloadButton = new Button("Download Metadata");
  private final TabSheet sampleExperimentTabSheet = new TabSheet();
  private final BatchRegistrationDialog batchRegistrationDialog = new BatchRegistrationDialog();
  private static final Logger log = getLogger(SampleDetailsComponent.class);
  private final transient SampleDetailsComponentHandler sampleDetailsComponentHandler;
  private final List<ValueChangeListener<ComponentValueChangeEvent<TextField, String>>> searchFieldListeners = new ArrayList<>();
  private ProjectId projectId;

  public SampleDetailsComponent(@Autowired SampleInformationService sampleInformationService,
      @Autowired BatchRegistrationService batchRegistrationService,
      @Autowired SampleRegistrationService sampleRegistrationService) {
    initSampleView();
    this.sampleDetailsComponentHandler = new SampleDetailsComponentHandler(
        sampleInformationService, batchRegistrationService,
        sampleRegistrationService);
  }

  private void initSampleView() {
    addClassName("sample-details-component");
    initButtonAndFieldBar();
    addComponentAsFirst(title);
    title.addClassName("title");
    add(content);
    content.addClassName("content");
  }

  private void initButtonAndFieldBar() {
    searchField.setPlaceholder("Search");
    searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
    searchField.setValueChangeMode(ValueChangeMode.EAGER);
    fieldBar.add(searchField);
    fieldBar.addClassName("search-bar");
    //Items in layout should be aligned at the end due to searchFieldLabel taking up space
    registerButton.addClassName("primary");
    buttonBar.add(registerButton, metadataDownloadButton);
    buttonBar.addClassName("button-bar");
    //Moves buttonbar to right side of sample grid
    buttonAndFieldBar.add(fieldBar, buttonBar);
    buttonAndFieldBar.addClassName("button-and-search-bar");
  }

  private static ComponentRenderer<Div, SamplePreview> createConditionRenderer() {
    return new ComponentRenderer<>(Div::new, styleConditionValue);
  }

  private static final SerializableBiConsumer<Div, SamplePreview> styleConditionValue = (div, samplePreview) -> samplePreview.experimentalGroup()
      .condition().getVariableLevels().forEach(variableLevel -> {
        div.addClassName("tag-collection");
        String experimentalVariable =
            variableLevel.variableName().value() + ": " + variableLevel.experimentalValue().value();
        if (variableLevel.experimentalValue().unit().isPresent()) {
          experimentalVariable =
              experimentalVariable + " " + variableLevel.experimentalValue().unit().get();
        }
        Tag tag = new Tag(experimentalVariable);
        tag.addClassName("primary");
        tag.setTitle(experimentalVariable);
        div.add(tag);
      });

  /**
   * Provides the {@link ProjectId} of the currently selected project to this component
   * <p>
   * This method provides the {@link ProjectId} necessary for routing within this component.
   *
   * @param projectId ProjectId provided to this component
   */
  public void setProject(ProjectId projectId) {
    this.projectId = projectId;
  }

  /**
   * Provides the collection of {@link Experiment} to this component
   * <p>
   * This method should be used to provide the experiments within the
   * {@link life.qbic.projectmanagement.domain.project.Project} to this component
   *
   * @param experiments collection of experiments to be shown as tabs in the {@link TabSheet} within
   *                    this component
   */

  public void setExperiments(Collection<Experiment> experiments) {
    sampleDetailsComponentHandler.setExperiments(experiments);
  }

  /**
   * Adds the provided {@link BatchRegistrationListener} to the list of listeners which will
   * retrieve notification if a new {@link Batch} was created in this component
   *
   * @param batchRegistrationListener listener to be notified if a batch was registered within this
   *                                  component
   */
  public void addBatchRegistrationListener(BatchRegistrationListener batchRegistrationListener) {
    sampleDetailsComponentHandler.addBatchRegistrationListener(batchRegistrationListener);
  }

  private final class SampleDetailsComponentHandler {

    private final SampleInformationService sampleInformationService;
    private final BatchRegistrationService batchRegistrationService;
    private final SampleRegistrationService sampleRegistrationService;
    private final List<BatchRegistrationListener> registrationListener = new ArrayList<>();

    public SampleDetailsComponentHandler(SampleInformationService sampleInformationService,
        BatchRegistrationService batchRegistrationService,
        SampleRegistrationService sampleRegistrationService) {
      this.sampleInformationService = sampleInformationService;
      this.batchRegistrationService = batchRegistrationService;
      this.sampleRegistrationService = sampleRegistrationService;
      addEventListeners();
      configureSearch();
    }

    public void setExperiments(Collection<Experiment> experiments) {
      resetContent();
      createSampleOverview(experiments);
    }

    private void createSampleOverview(Collection<Experiment> experiments) {
      resetSampleOverview();
      experiments.forEach(this::addExperimentTabToTabSheet);
      setExperimentsInRegistrationDialog(experiments);
      content.add(buttonAndFieldBar);
      content.add(sampleExperimentTabSheet);
    }

    private void resetContent() {
      content.removeAll();
    }

    private void resetSampleOverview() {
      resetTabSheet();
      searchFieldListeners.clear();
    }

    private void resetTabSheet() {
      sampleExperimentTabSheet.getChildren()
          .forEach(component -> component.getElement().removeAllChildren());
    }


    private void setExperimentsInRegistrationDialog(Collection<Experiment> experiments) {
      List<Experiment> experimentsWithGroups = experiments.stream()
          .filter(experiment -> !experiment.getExperimentalGroups().isEmpty()).collect(
              Collectors.toList());
      batchRegistrationDialog.setExperiments(experimentsWithGroups);
    }

    private void addEventListeners() {
      batchRegistrationDialog.addBatchRegistrationEventListener(batchRegistrationEvent -> {
        BatchRegistrationDialog batchRegistrationSource = batchRegistrationEvent.getSource();
        registerBatchAndSamples(batchRegistrationSource.batchRegistrationContent(),
            batchRegistrationSource.sampleRegistrationContent()).onValue(batchId -> {
          fireBatchCreatedEvent(batchRegistrationEvent);
          batchRegistrationDialog.resetAndClose();
          displayRegistrationSuccess();
        });
      });
      registerButton.addClickListener(event -> batchRegistrationDialog.open());
      batchRegistrationDialog.addCancelEventListener(
          event -> batchRegistrationDialog.resetAndClose());
    }

    private void addExperimentTabToTabSheet(Experiment experiment) {
      Div experimentTabContent = new Div();
      SampleExperimentTab experimentTab = new SampleExperimentTab(experiment.getName(),
          0);
      if (!isExperimentGroupInExperiment(experiment)) {
        experimentTabContent.add(createNoGroupsDefinedDisclaimer(experiment));
        sampleExperimentTabSheet.add(experimentTab, experimentTabContent);
        return;
      }
      if (!sampleInformationService.retrieveSamplesForExperiment(experiment.experimentId())
          .isValue()) {
        experimentTabContent.add(createNoSamplesRegisteredDisclaimer(experiment));
        sampleExperimentTabSheet.add(experimentTab, experimentTabContent);
        return;
      }
      //assumption: experimental groups exist, and samples exist for those groups; checked previously
      Grid<SamplePreview> sampleGrid = createSampleGrid();
      //Initialize sampleCount before lazy loading is triggered by user
      experimentTab.setSampleCount(
          getSampleCountForExperiment(experiment.experimentId(), samplePreviewFilter));
      sampleGrid.getLazyDataView()
          .addItemCountChangeListener(event -> experimentTab.setSampleCount(event.getItemCount()));
      setSamplesToGrid(sampleGrid, experiment.experimentId());
      addSearchFieldListener(event -> sampleGrid.getDataProvider().refreshAll());
      sampleGrid.getDataProvider().refreshAll();
      experimentTabContent.add(sampleGrid);
      sampleExperimentTabSheet.add(experimentTab, experimentTabContent);
    }

    private void configureSearch() {
      searchField.setValueChangeMode(ValueChangeMode.LAZY);
      searchField.addValueChangeListener(event -> {
        samplePreviewFilter = event.getValue().trim();
        fireSearchFieldValueChangeEvent(event);
      });
    }

    /**
     * Adds the provided {@link ValueChangeListener} to the list of listeners which will retrieve
     * notification if the searchField value was updated
     */
    public void addSearchFieldListener(
        ValueChangeListener<ComponentValueChangeEvent<TextField, String>> searchFieldListener) {
      searchFieldListeners.add(searchFieldListener);
    }

    private void fireSearchFieldValueChangeEvent(
        ComponentValueChangeEvent<TextField, String> event) {
      searchFieldListeners.forEach(listener -> listener.valueChanged(event));
    }


    private Grid<SamplePreview> createSampleGrid() {
      Grid<SamplePreview> sampleGrid = new Grid<>();
      sampleGrid.addColumn(SamplePreview::sampleCode).setHeader("Sample Id");
      sampleGrid.addColumn(SamplePreview::sampleLabel).setHeader("Sample Label");
      sampleGrid.addColumn(SamplePreview::batchLabel).setHeader("Batch");
      sampleGrid.addColumn(SamplePreview::replicateLabel).setHeader("Biological Replicate");
      sampleGrid.addColumn(createConditionRenderer()).setHeader("Condition").setAutoWidth(true);
      sampleGrid.addColumn(SamplePreview::species).setHeader("Species");
      sampleGrid.addColumn(SamplePreview::specimen).setHeader("Specimen");
      sampleGrid.addColumn(SamplePreview::analyte).setHeader("Analyte");
      sampleGrid.addColumn(SamplePreview::analysisType).setHeader("Analysis to Perform");
      sampleGrid.addColumn(SamplePreview::comment).setHeader("Comment");
      return sampleGrid;
    }

    private int getSampleCountForExperiment(ExperimentId experimentId, String filter) {
      return sampleInformationService.countPreviews(experimentId, filter);
    }

    private void setSamplesToGrid(Grid<SamplePreview> sampleGrid, ExperimentId experimentId) {
      sampleGrid.setItems(query -> {
        sampleGrid.getDataProvider().refreshAll();
        List<SortOrder> sortOrders = query.getSortOrders().stream().map(
                it -> new SortOrder(it.getSorted(), it.getDirection().equals(SortDirection.ASCENDING)))
            .collect(Collectors.toList());
        // if no order is provided by the grid order by last modified (least priority)
        sortOrders.add(SortOrder.of("sampleCode").ascending());
        return sampleInformationService.queryPreview(experimentId, query.getOffset(),
            query.getLimit(), List.copyOf(sortOrders), samplePreviewFilter).stream();
      }, query -> getSampleCountForExperiment(experimentId, samplePreviewFilter));
    }

    private boolean isExperimentGroupInExperiment(Experiment experiment) {
      return !experiment.getExperimentalGroups().isEmpty();
    }

    private DisclaimerCard createNoGroupsDefinedDisclaimer(Experiment experiment) {
      DisclaimerCard noGroupsDefinedCard = DisclaimerCard.createWithTitle(
          "No experimental groups defined",
          "Start the sample registration process by registering the first experimental group",
          "Add Experimental Group");
      String experimentId = experiment.experimentId().value();
      noGroupsDefinedCard.subscribe(event -> routeToExperimentalGroupCreation(event, experimentId));
      return noGroupsDefinedCard;
    }

    private void routeToExperimentalGroupCreation(ComponentEvent<?> componentEvent,
        String experimentId) {
      if (componentEvent.isFromClient()) {
        log.debug(String.format("Rerouting to experiment page for experiment %s of project %s",
            experimentId, projectId.value()));
        String routeToExperimentPage = String.format(Projects.EXPERIMENT, projectId.value(),
            experimentId);
        componentEvent.getSource().getUI().ifPresent(ui ->
            ui.navigate(routeToExperimentPage));
      }
    }

    private DisclaimerCard createNoSamplesRegisteredDisclaimer(Experiment experiment) {
      DisclaimerCard noSamplesDefinedCard = DisclaimerCard.createWithTitle("No samples registered",
          "Register your first samples for this experiment", "Register Samples");
      noSamplesDefinedCard.subscribe(event -> {
        batchRegistrationDialog.setSelectedExperiment(experiment);
        batchRegistrationDialog.open();
      });
      return noSamplesDefinedCard;
    }

    private Result<?, ?> registerBatchAndSamples(BatchRegistrationContent batchRegistrationContent,
        List<SampleRegistrationContent> sampleRegistrationContent) {
      return registerBatchInformation(batchRegistrationContent).onValue(
          batchId -> {
            List<SampleRegistrationRequest> sampleRegistrationsRequests = createSampleRegistrationRequests(
                batchId, batchRegistrationContent.experimentId(), sampleRegistrationContent);
            registerSamples(sampleRegistrationsRequests);
          });
    }

    private Result<BatchId, ResponseCode> registerBatchInformation(
        BatchRegistrationContent batchRegistrationContent) {
      return batchRegistrationService.registerBatch(batchRegistrationContent.batchLabel(),
          batchRegistrationContent.isPilot()).onError(responseCode -> displayRegistrationFailure());
    }

    private List<SampleRegistrationRequest> createSampleRegistrationRequests(BatchId batchId,
        ExperimentId experimentId,
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
                sampleRegistrationContent.biologicalReplicateId(), sampleOrigin,
                sampleRegistrationContent.analysisType(), sampleRegistrationContent.comment());
          }).toList();
    }

    private void registerSamples(List<SampleRegistrationRequest> sampleRegistrationRequests) {
      sampleRegistrationService.registerSamples(sampleRegistrationRequests, projectId)
          .onError(responseCode -> displayRegistrationFailure());
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

    private void addBatchRegistrationListener(BatchRegistrationListener batchRegistrationListener) {
      this.registrationListener.add(batchRegistrationListener);
    }

    private void fireBatchCreatedEvent(BatchRegistrationEvent event) {
      registrationListener.forEach(it -> it.handle(event));
    }
  }

  @FunctionalInterface
  public interface BatchRegistrationListener {

    void handle(BatchRegistrationEvent event);
  }

  private class SampleExperimentTab extends Tab {

    private final Span sampleCountComponent;
    private final Span experimentNameComponent;

    public SampleExperimentTab(String experimentName, int sampleCount) {
      this.experimentNameComponent = new Span(experimentName);
      this.sampleCountComponent = createBadge(sampleCount);
      this.add(experimentNameComponent, sampleCountComponent);
    }

    /**
     * Getter method for retrieving the currently set name of the {@link Experiment} shown in this
     * component
     *
     * @return String containing the experimentName shown in the Tab
     */
    public String getExperimentName() {
      return experimentNameComponent.getText();
    }

    /**
     * Setter method for specifying the name of the {@link Experiment} shown in this component
     *
     * @param experimentName name of the Experiment to be shown in this component
     */
    public void setExperimentName(String experimentName) {
      experimentNameComponent.setText(experimentName);
    }

    /**
     * Getter method for retrieving the currently set number of {@link Sample} associated with the
     * {@link Experiment} shown in this component
     *
     * @return int containing the number of samples shown in the Tab
     */
    public int getSampleCount() {
      return Integer.parseInt(sampleCountComponent.getText());
    }

    /**
     * Setter method for specifying the number of {@link Sample} of the {@link Experiment} shown in
     * this component
     *
     * @param sampleCount number of samples associated with the experiment shown in this component
     */
    public void setSampleCount(int sampleCount) {
      sampleCountComponent.setText(Integer.toString(sampleCount));
    }

    /**
     * Helper method for creating a badge.
     */
    private Span createBadge(int numberOfSamples) {
      Tag tag = new Tag(String.valueOf(numberOfSamples));
      tag.addClassName("contrast");
      return tag;
    }
  }
}
