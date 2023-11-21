package life.qbic.datamanager.views.projects.project.samples;

import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
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
import life.qbic.datamanager.views.general.DisclaimerConfirmedEvent;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.notifications.ErrorMessage;
import life.qbic.datamanager.views.notifications.StyledNotification;
import life.qbic.datamanager.views.notifications.SuccessMessage;
import life.qbic.datamanager.views.projects.project.experiments.experiment.Tag;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog.ConfirmEvent;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog.SampleInfo;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.application.batch.BatchRegistrationService;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.application.sample.SampleRegistrationService;
import life.qbic.projectmanagement.domain.model.batch.Batch;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleOrigin;
import life.qbic.projectmanagement.domain.model.sample.SampleRegistrationRequest;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sample Details Component
 * <p>
 * Component embedded within the {@link SampleInformationMain}. It allows the user to see the
 * information associated for all {@link Batch} and {@link Sample} of each
 * {@link Experiment within a {@link Project } Additionally it enables the user to register new
 * {@link Batch} and {@link Sample} via the contained {@link BatchRegistrationDialog}.
 */

@SpringComponent
@UIScope
public class SampleDetailsComponent extends PageArea implements Serializable {

  @Serial
  private static final long serialVersionUID = 2893730975944372088L;
  private static final Logger log = logger(SampleDetailsComponent.class);
  private final SampleInformationService sampleInformationService;
  private final SampleExperimentTab experimentTab;
  private Context context;
  private final SampleRegistrationService sampleRegistrationService;
  private final ExperimentInformationService experimentInformationService;
  private final BatchRegistrationService batchRegistrationService;


  private final Div content;
  //  private final Div buttonAndFieldBar;
  private final TextField searchField;
  public final Button registerButton;
  private Disclaimer noGroupsDefinedDisclaimer;
  private final Disclaimer noSamplesRegisteredDisclaimer;
  private final Grid<SamplePreview> sampleGrid;



  public SampleDetailsComponent(@Autowired SampleInformationService sampleInformationService,
      @Autowired BatchRegistrationService batchRegistrationService,
      @Autowired SampleRegistrationService sampleRegistrationService,
      @Autowired ExperimentInformationService experimentInformationService) {
    this.experimentInformationService = experimentInformationService;
    this.batchRegistrationService = batchRegistrationService;
    this.sampleRegistrationService = sampleRegistrationService;
    this.sampleInformationService = sampleInformationService;
    addClassName("sample-details-component");

    this.searchField = createSearchField();
    searchField.addValueChangeListener(valueChangeEvent -> {

    });
    this.registerButton = createRegisterButton();

    Span fieldBar = new Span();
    fieldBar.addClassName("search-bar");
    fieldBar.add(searchField);

    Span buttonBar = new Span();
    buttonBar.addClassName("button-bar");
    Button metadataDownloadButton = new Button("Download Metadata");
    buttonBar.add(registerButton, metadataDownloadButton);

    Div buttonAndFieldBar = new Div();
    buttonAndFieldBar.addClassName("button-and-search-bar");
    buttonAndFieldBar.add(fieldBar, buttonBar);

    Span title = new Span("Samples");
    title.addClassName("title");

    addComponentAsFirst(title);

    Div experimentTabContent = new Div();
    experimentTabContent.addClassName("sample-tab-content");
    experimentTab = new SampleExperimentTab("", 0);

    sampleGrid = createSampleGrid();

    noGroupsDefinedDisclaimer = createNoGroupsDefinedDisclaimer();
    noGroupsDefinedDisclaimer.setVisible(false);

    noSamplesRegisteredDisclaimer = createNoSamplesRegisteredDisclaimer();
    noSamplesRegisteredDisclaimer.setVisible(false);

    experimentTabContent.add(sampleGrid, noGroupsDefinedDisclaimer, noSamplesRegisteredDisclaimer);

    TabSheet sampleExperimentTabSheet = new TabSheet();
    sampleExperimentTabSheet.add(experimentTab, experimentTabContent);
    sampleExperimentTabSheet.setHeightFull();

    content = new Div();
    content.addClassName("sample-details-content");

    content.add(buttonAndFieldBar, sampleExperimentTabSheet);
    add(content);

    searchField.addValueChangeListener(this::onSearchFieldChanged);
  }

  private void onSearchFieldChanged(ComponentValueChangeEvent<TextField, String> valueChangeEvent) {
    updateSampleGridDataProvider(context.experimentId().orElseThrow(), valueChangeEvent.getValue());
  }

  private void updateSampleGridDataProvider(ExperimentId experimentId, String filter) {
    sampleGrid.setItems(query -> {
      List<SortOrder> sortOrders = query.getSortOrders().stream().map(
              it -> new SortOrder(it.getSorted(), it.getDirection().equals(SortDirection.ASCENDING)))
          .collect(Collectors.toList());
      // if no order is provided by the grid order by last modified (least priority)
      sortOrders.add(SortOrder.of("sampleCode").ascending());
      return sampleInformationService.queryPreview(experimentId, query.getOffset(),
          query.getLimit(), List.copyOf(sortOrders), filter).stream();
    }, query -> SampleDetailsComponent.this.sampleInformationService.countPreviews(
        experimentId,
        filter));
    sampleGrid.getLazyDataView()
        .addItemCountChangeListener(
            countChangeEvent -> experimentTab.setSampleCount(countChangeEvent.getItemCount()));
  }

  private Button createRegisterButton() {
    Button button = new Button("Register");
    button.addClassName("primary");
    button.addClickListener(event -> onRegisterButtonClicked());
    return button;
  }

  private static TextField createSearchField() {
    TextField textField = new TextField();
    textField.setPlaceholder("Search");
    textField.setPrefixComponent(VaadinIcon.SEARCH.create());
    textField.setValueChangeMode(ValueChangeMode.EAGER);
    return textField;
  }

  private void onRegisterButtonClicked() {
    Experiment experiment = context.experimentId()
        .flatMap(experimentInformationService::find)
        .orElseThrow();

    if (noExperimentGroupsInExperiment(experiment)) {
      return;
    }

    BatchRegistrationDialog dialog = new BatchRegistrationDialog(
        experiment.getName(), new ArrayList<>(experiment.getSpecies()),
        new ArrayList<>(experiment.getSpecimens()), new ArrayList<>(experiment.getAnalytes()),
        experiment.getExperimentalGroups());
    dialog.addCancelListener(cancelEvent -> cancelEvent.getSource().close());
    dialog.addConfirmListener(this::onRegisterDialogConfirmed);
    dialog.open();
  }

  private void onRegisterDialogConfirmed(ConfirmEvent confirmEvent) {
    String batchLabel = confirmEvent.getData().batchName();
    List<SampleInfo> samples = confirmEvent.getData().samples();

    List<SampleRegistrationRequest> sampleRegistrationRequests = batchRegistrationService.registerBatch(
            batchLabel, false,
            context.projectId().orElseThrow())
        .map(batchId -> samples.stream()
            .map(sample -> new SampleRegistrationRequest(
                sample.getSampleLabel(),
                batchId,
                context.experimentId().orElseThrow(),
                sample.getExperimentalGroup().id(),
                sample.getBiologicalReplicate().id(),
                SampleOrigin.create(sample.getSpecies(), sample.getSpecimen(), sample.getAnalyte()),
                sample.getAnalysisToBePerformed(),
                sample.getCustomerComment()))
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
    return new ComponentRenderer<>(SampleDetailsComponent::createTagCollection,
        SampleDetailsComponent::fillTagCollection);
  }

  private static void fillTagCollection(Div div, SamplePreview samplePreview) {
    samplePreview
        .experimentalGroup()
        .condition()
        .getVariableLevels().stream()
        .map(variableLevel -> "%s: %s %s".formatted(variableLevel.variableName().value(),
                variableLevel.experimentalValue().value(),
                variableLevel.experimentalValue().unit().orElse(""))
            .trim())
        .map(s -> {
          Tag tag = new Tag(s);
          tag.setTitle(s);
          return tag;
        })
        .forEach(div::add);
  }

  private static Div createTagCollection() {
    Div tagCollection = new Div();
    tagCollection.addClassName("tag-collection");
    return tagCollection;
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
    setExperiment(
        experimentInformationService.find(context.experimentId().orElseThrow()).orElseThrow());
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

  private void routeToExperimentalGroupCreation(ComponentEvent<?> componentEvent,
      String experimentId) {
    if (componentEvent.isFromClient()) {
      String routeToExperimentPage = String.format(Projects.EXPERIMENT,
          context.projectId().orElseThrow().value(),
          experimentId);
      log.debug(String.format(
          "Rerouting to experiment page for experiment %s of project %s: " + routeToExperimentPage,
          experimentId, context.projectId().orElseThrow().value()));
      componentEvent.getSource().getUI().ifPresent(ui -> ui.navigate(routeToExperimentPage));
    }
  }

  private void setExperiment(Experiment experiment) {
    experimentTab.setExperimentName(experiment.getName());

    if (noExperimentGroupsInExperiment(experiment)) {
      sampleGrid.setVisible(false);
      noSamplesRegisteredDisclaimer.setVisible(false);
      noGroupsDefinedDisclaimer.setVisible(true);
      return;
    }
    if (noSamplesRegisteredInExperiment(experiment)) {
      sampleGrid.setVisible(false);
      noSamplesRegisteredDisclaimer.setVisible(true);
      noGroupsDefinedDisclaimer.setVisible(false);
      return;
    }
    updateSampleGridDataProvider(context.experimentId().orElseThrow(), searchField.getValue());

    sampleGrid.setVisible(true);
    noSamplesRegisteredDisclaimer.setVisible(false);
    noGroupsDefinedDisclaimer.setVisible(false);
  }

  private void onNoGroupsDefinedClicked(DisclaimerConfirmedEvent event) {
    routeToExperimentalGroupCreation(event, context.experimentId().orElseThrow().value());
  }

  private static boolean noExperimentGroupsInExperiment(Experiment experiment) {
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

  private Disclaimer createNoSamplesRegisteredDisclaimer() {
    Disclaimer noSamplesDefinedCard = Disclaimer.createWithTitle(
        "Manage your samples in one place",
        "Start your project by registering the first sample batch", "Register batch");
    noSamplesDefinedCard.addDisclaimerConfirmedListener(event -> onRegisterButtonClicked());
    return noSamplesDefinedCard;
  }

  private Disclaimer createNoGroupsDefinedDisclaimer() {
    Disclaimer noGroupsDefindedDisclaimer = Disclaimer.createWithTitle(
        "Design your experiment first",
        "Start the sample registration process by defining experimental groups",
        "Add groups");
    noGroupsDefindedDisclaimer.addDisclaimerConfirmedListener(this::onNoGroupsDefinedClicked);
    return noGroupsDefindedDisclaimer;
  }

  private static Grid<SamplePreview> createSampleGrid() {
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

    private final Span countBadge;
    private final Span experimentNameComponent;

    public SampleExperimentTab(String experimentName, int sampleCount) {
      experimentNameComponent = new Span();
      this.countBadge = createBadge();
      Span sampleCountComponent = new Span();
      sampleCountComponent.add(countBadge);
      this.add(experimentNameComponent, sampleCountComponent);

      setExperimentName(experimentName);
      setSampleCount(sampleCount);
    }

    /**
     * Setter method for specifying the number of {@link Sample} of the {@link Experiment} shown in
     * this component
     *
     * @param sampleCount number of samples associated with the experiment shown in this component
     */
    public void setSampleCount(int sampleCount) {
      countBadge.setText(String.valueOf(sampleCount));
    }

    public void setExperimentName(String experimentName) {
      this.experimentNameComponent.setText(experimentName);
    }

    /**
     * Helper method for creating a badge.
     */
    private static Span createBadge() {
      Tag tag = new Tag(String.valueOf(0));
      tag.addClassName("contrast");
      return tag;
    }
  }
}
