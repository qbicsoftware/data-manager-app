package life.qbic.datamanager.views.projects.project.samples;

import static org.slf4j.LoggerFactory.getLogger;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.Tabs;
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
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Disclaimer;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.datamanager.views.projects.project.experiments.experiment.Tag;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog.ConfirmEvent;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog.ConfirmEvent.Data;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.application.sample.SampleRegistrationService;
import life.qbic.projectmanagement.domain.model.batch.Batch;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalGroup;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleOrigin;
import life.qbic.projectmanagement.domain.model.sample.SampleRegistrationRequest;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sample Details Component
 * <p>
 * Component embedded within the {@link SampleInformationMain}. It allows the user to see the
 * information associated for all {@link Batch} and {@link Sample} of each
 * {@link Experiment within a {@link Project } Additionally it enables the user to register new
 * {@link Batch} and {@link Sample} via the contained {@link BatchRegistrationDialog} and propagates
 * the successful registration to the registered {@link BatchRegistrationListener} within this
 * component.
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
  private final SampleRegistrationService sampleRegistrationService;
  private String samplePreviewFilter = "";
  public final Button registerButton = new Button("Register");
  private final Button metadataDownloadButton = new Button("Download Metadata");
  private final TabSheet sampleExperimentTabSheet = new TabSheet();
  private static final Logger log = getLogger(SampleDetailsComponent.class);
  private final transient SampleDetailsComponentHandler sampleDetailsComponentHandler;
  private final List<ValueChangeListener<ComponentValueChangeEvent<TextField, String>>> searchFieldListeners = new ArrayList<>();
  private Context context;
  private final ExperimentInformationService experimentInformationService;
  private final BatchRegistrationService batchRegistrationService;

  public SampleDetailsComponent(@Autowired SampleInformationService sampleInformationService,
      @Autowired BatchRegistrationService batchRegistrationService,
      @Autowired SampleRegistrationService sampleRegistrationService,
      @Autowired ExperimentInformationService experimentInformationService) {
    this.experimentInformationService = experimentInformationService;
    initSampleView();
    this.batchRegistrationService = batchRegistrationService;
    this.sampleRegistrationService = sampleRegistrationService;
    this.sampleDetailsComponentHandler = new SampleDetailsComponentHandler(
        sampleInformationService
    );
  }

  private void initSampleView() {
    addClassName("sample-details-component");
    initButtonAndFieldBar();
    addComponentAsFirst(title);
    title.addClassName("title");
    add(content);
    content.addClassName("sample-details-content");
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

  private void onRegisterButtonClicked() {
    Experiment experiment = context.experimentId()
        .flatMap(experimentInformationService::find)
        .orElseThrow();

    BatchRegistrationDialog dialog = new BatchRegistrationDialog(
        experiment.getName(), new ArrayList<>(experiment.getSpecies()),
        new ArrayList<>(experiment.getSpecimens()), new ArrayList<>(experiment.getAnalytes()),
        experiment.getExperimentalGroups());

    dialog.addCancelListener(cancelEvent -> cancelEvent.getSource().close());
    dialog.addConfirmListener(this::onRegisterDialogConfirmed);
    dialog.open();
  }

  private void onRegisterDialogConfirmed(ConfirmEvent confirmEvent) {
    Data data = confirmEvent.getData();
    List<SampleRegistrationRequest> sampleRegistrationRequests = batchRegistrationService.registerBatch(
            data.batchName(), false,
            context.projectId().orElseThrow())
        .map(batchId -> data.samples().stream()
            .map(sample -> new SampleRegistrationRequest(
                data.batchName(), batchId,
                context.experimentId().orElseThrow(),
                sample.getExperimentalGroup().id(), sample.getBiologicalReplicate().id(),
                SampleOrigin.create(sample.getSpecies(), sample.getSpecimen(),
                    sample.getAnalyte()),
                sample.getAnalysisToBePerformed(), sample.getCustomerComment()))
            .toList())
        .onError(responseCode -> displayRegistrationFailure())
        .valueOrElseThrow(() ->
            new ApplicationException("Could not create sample registration requests"));
    sampleRegistrationService.registerSamples(sampleRegistrationRequests,
            context.projectId().orElseThrow())
        .onError(responseCode -> displayRegistrationFailure())
        .onValue(ignored -> fireEvent(new BatchRegisteredEvent(this, false)))
        .onValue(ignored -> confirmEvent.getSource().close())
        .onValue(batchId -> displayRegistrationSuccess());
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

  private static ComponentRenderer<Div, SamplePreview> createConditionRenderer() {
    return new ComponentRenderer<>(Div::new, styleConditionValue);
  }

  private static final SerializableBiConsumer<Div, SamplePreview> styleConditionValue = (div, samplePreview) -> collectVariableLevelsInExperimentalGroup(
      samplePreview.experimentalGroup()).forEach(
      experimentalVariable -> {
        Tag tag = new Tag(experimentalVariable);
        tag.setTitle(experimentalVariable);
        div.add(tag);
        div.addClassName("tag-collection");
      });

  private static List<String> collectVariableLevelsInExperimentalGroup(
      ExperimentalGroup experimentalGroup) {
    List<String> variableLevels = new ArrayList<>();
    experimentalGroup
        .condition().getVariableLevels().forEach(variableLevel -> {
          String experimentalVariable =
              variableLevel.variableName().value() + ": " + variableLevel.experimentalValue().value();
          if (variableLevel.experimentalValue().unit().isPresent()) {
            experimentalVariable =
                experimentalVariable + " " + variableLevel.experimentalValue().unit().get();
          }
          variableLevels.add(experimentalVariable);
        });
    variableLevels.sort(null);
    return variableLevels;
  }


  /**
   * Propagates the context to internal components.
   *
   * @param context the context in which the user is.
   */
  public void setContext(Context context) {
    context.experimentId()
        .orElseThrow(() -> new ApplicationException("no experiment id in context " + context));
    context.projectId()
        .orElseThrow(() -> new ApplicationException("no project id in context " + context));
    this.context = context;
  }

  /**
   * Provides the collection of {@link Experiment} to this component
   * <p>
   * This method should be used to provide the experiments within the {@link Project} to this
   * component
   *
   * @param experiments collection of experiments to be shown as tabs in the {@link TabSheet} within
   *                    this component
   */

  public void setExperiments(Collection<Experiment> experiments) {
    sampleDetailsComponentHandler.setExperiments(experiments);
  }

  /**
   * Sets the experiment tab within the tabsheet to the experimentId
   *
   * @param experimentId {@link ExperimentId} id of the experiment provided within the URL linking
   *                     to this component
   */

  public void setSelectedExperiment(ExperimentId experimentId) {
    Tabs experimentTabs = sampleExperimentTabSheet.getChildren()
        .filter(component -> component instanceof Tabs)
        .map(component -> (Tabs) component)
        .findFirst().orElseThrow();
    List<SampleExperimentTab> sampleExperimentTabList = experimentTabs.getChildren()
        .filter(component -> component instanceof SampleExperimentTab)
        .map(component -> (SampleExperimentTab) component).toList();
    SampleExperimentTab selectedSampleTab = sampleExperimentTabList.stream()
        .filter(sampleExperimentTab -> sampleExperimentTab.experimentId.equals(experimentId))
        .findFirst().orElseThrow();
    sampleExperimentTabSheet.setSelectedTab(selectedSampleTab);
  }

  /**
   * Adds the provided {@link BatchRegistrationListener} to the list of listeners which will
   * retrieve notification if a new {@link Batch} was created in this component
   *
   * @param batchRegistrationListener listener to be notified if a batch was registered within this
   *                                  component
   */
  public void addBatchRegistrationListener(
      ComponentEventListener<BatchRegisteredEvent> batchRegistrationListener) {
    addListener(BatchRegisteredEvent.class, batchRegistrationListener);
  }

  private final class SampleDetailsComponentHandler {

    private final SampleInformationService sampleInformationService;

    public SampleDetailsComponentHandler(SampleInformationService sampleInformationService) {
      this.sampleInformationService = sampleInformationService;
      registerButton.addClickListener(event -> onRegisterButtonClicked());
      configureSearch();
    }

    public void setExperiments(Collection<Experiment> experiments) {
      resetContent();
      createSampleOverview(experiments);
    }

    private void createSampleOverview(Collection<Experiment> experiments) {
      resetSampleOverview();
      experiments.forEach(this::addExperimentTabToTabSheet);
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

    private void addExperimentTabToTabSheet(Experiment experiment) {
      Div experimentTabContent = new Div();
      experimentTabContent.addClassName("sample-tab-content");
      SampleExperimentTab experimentTab = new SampleExperimentTab(experiment.experimentId(),
          experiment.getName(),
          0);
      sampleExperimentTabSheet.setHeightFull();
      if (noExperimentGroupsInExperiment(experiment)) {
        experimentTabContent.add(createNoGroupsDefinedDisclaimer(experiment));
        sampleExperimentTabSheet.add(experimentTab, experimentTabContent);
        return;
      }
      if (noSamplesRegisteredInExperiment(experiment)) {
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
      Grid<SamplePreview> sampleGrid = new Grid<>(SamplePreview.class);
      sampleGrid.addColumn(SamplePreview::sampleCode).setHeader("Sample Id")
          .setSortProperty("sampleCode").setAutoWidth(true).setFlexGrow(0)
          .setTooltipGenerator(SamplePreview::sampleCode);
      sampleGrid.addColumn(SamplePreview::sampleLabel).setHeader("Sample Label")
          .setSortProperty("sampleLabel").setTooltipGenerator(SamplePreview::sampleLabel);
      sampleGrid.addColumn(SamplePreview::batchLabel).setHeader("Batch")
          .setSortProperty("batchLabel").setTooltipGenerator(SamplePreview::batchLabel);
      sampleGrid.addColumn(SamplePreview::replicateLabel).setHeader("Biological Replicate")
          .setSortProperty("bioReplicateLabel").setTooltipGenerator(SamplePreview::replicateLabel);
      sampleGrid.addColumn(createConditionRenderer()).setHeader("Condition")
          .setSortProperty("experimentalGroup").setAutoWidth(true).setFlexGrow(0);
      sampleGrid.addColumn(SamplePreview::species).setHeader("Species").setSortProperty("species")
          .setTooltipGenerator(SamplePreview::species);
      sampleGrid.addColumn(SamplePreview::specimen).setHeader("Specimen")
          .setSortProperty("specimen").setTooltipGenerator(SamplePreview::specimen);
      sampleGrid.addColumn(SamplePreview::analyte).setHeader("Analyte").setSortProperty("analyte")
          .setTooltipGenerator(SamplePreview::analyte);
      sampleGrid.addColumn(SamplePreview::analysisMethod).setHeader("Analysis to Perform")
          .setSortProperty("analysisMethod").setTooltipGenerator(SamplePreview::analysisMethod);
      sampleGrid.addColumn(SamplePreview::comment).setHeader("Comment").setSortProperty("comment")
          .setTooltipGenerator(SamplePreview::comment);
      sampleGrid.addClassName("sample-grid");
      sampleGrid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
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

    private boolean noExperimentGroupsInExperiment(Experiment experiment) {
      return experiment.getExperimentalGroups().isEmpty();
    }

    private boolean noSamplesRegisteredInExperiment(Experiment experiment) {
      return sampleInformationService.retrieveSamplesForExperiment(experiment.experimentId())
          .map(Collection::isEmpty)
          .onError(error -> {
            throw new ApplicationException("Unexpected response code : " + error);
          })
          .getValue();
    }

    private Disclaimer createNoGroupsDefinedDisclaimer(Experiment experiment) {
      Disclaimer noGroupsDefinedCard = Disclaimer.createWithTitle(
          "Design your experiment first",
          "Start the sample registration process by defining experimental groups",
          "Add groups");
      String experimentId = experiment.experimentId().value();
      noGroupsDefinedCard.addDisclaimerConfirmedListener(
          event -> routeToExperimentalGroupCreation(event, experimentId));
      return noGroupsDefinedCard;
    }

    private void routeToExperimentalGroupCreation(ComponentEvent<?> componentEvent,
        String experimentId) {
      if (componentEvent.isFromClient()) {
        log.debug(String.format("Rerouting to experiment page for experiment %s of project %s",
            experimentId, context.projectId().orElseThrow().value()));
        String routeToExperimentPage = String.format(Projects.EXPERIMENT,
            context.projectId().orElseThrow().value(),
            experimentId);
        componentEvent.getSource().getUI().ifPresent(ui ->
            ui.navigate(routeToExperimentPage));
      }
    }

    private Disclaimer createNoSamplesRegisteredDisclaimer(Experiment experiment) {
      Disclaimer noSamplesDefinedCard = Disclaimer.createWithTitle(
          "Manage your samples in one place",
          "Start your project by registering the first sample batch", "Register batch");
      noSamplesDefinedCard.addDisclaimerConfirmedListener(event -> onRegisterButtonClicked());
      return noSamplesDefinedCard;
    }
  }

  public static class BatchRegisteredEvent extends ComponentEvent<SampleDetailsComponent> {


    /**
     * Creates a new event using the given source and indicator whether the event originated from
     * the client side or the server side.
     *
     * @param source     the source component
     * @param fromClient <code>true</code> if the event originated from the client
     *                   side, <code>false</code> otherwise
     */
    public BatchRegisteredEvent(SampleDetailsComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  public static class SampleExperimentTab extends Tab {

    private final Span sampleCountComponent = new Span();
    private final ExperimentId experimentId;

    public SampleExperimentTab(ExperimentId experimentId, String experimentName, int sampleCount) {
      this.experimentId = experimentId;
      Span experimentNameComponent = new Span(experimentName);
      sampleCountComponent.add(createBadge(sampleCount));
      this.add(experimentNameComponent, sampleCountComponent);
    }

    /**
     * Setter method for specifying the number of {@link Sample} of the {@link Experiment} shown in
     * this component
     *
     * @param sampleCount number of samples associated with the experiment shown in this component
     */
    public void setSampleCount(int sampleCount) {
      sampleCountComponent.removeAll();
      sampleCountComponent.add(createBadge(sampleCount));
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
