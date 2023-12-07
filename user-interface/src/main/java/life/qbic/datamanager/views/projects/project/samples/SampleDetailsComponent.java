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
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Disclaimer;
import life.qbic.datamanager.views.general.DisclaimerConfirmedEvent;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.projects.project.experiments.experiment.Tag;
import life.qbic.datamanager.views.projects.project.samples.registration.batch.BatchRegistrationDialog;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.ExperimentInformationService;
import life.qbic.projectmanagement.application.SortOrder;
import life.qbic.projectmanagement.application.sample.SampleInformationService;
import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.domain.model.Ontology;
import life.qbic.projectmanagement.domain.model.batch.Batch;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.experiment.vocabulary.OntologyClassDTO;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sample Details Component
 * <p>
 * Component embedded within the {@link SampleInformationMain}. It allows the user to see the
 * information associated for all {@link Batch} and {@link Sample} of each
 * {@link Experiment within a {@link Project} Additionally it enables the user to register new
 * {@link Batch} and {@link Sample} via the contained {@link BatchRegistrationDialog}.
 */

@SpringComponent
@UIScope
public class SampleDetailsComponent extends PageArea implements Serializable {

  @Serial
  private static final long serialVersionUID = 2893730975944372088L;
  private static final Logger log = logger(SampleDetailsComponent.class);
  private final SampleExperimentTab experimentTab;
  private Context context;
  private final TextField searchField;
  private final Disclaimer noGroupsDefinedDisclaimer;
  private final Disclaimer noSamplesRegisteredDisclaimer;
  private final Grid<SamplePreview> sampleGrid;
  private final ExperimentInformationService experimentInformationService;
  private final SampleInformationService sampleInformationService;

  public SampleDetailsComponent(@Autowired SampleInformationService sampleInformationService,
      @Autowired ExperimentInformationService experimentInformationService) {
    this.experimentInformationService = experimentInformationService;
    this.sampleInformationService = sampleInformationService;
    addClassName("sample-details-component");

    this.searchField = createSearchField();
    searchField.addValueChangeListener(valueChangeEvent -> {
    });
    searchField.addValueChangeListener(this::onSearchFieldChanged);
    Span fieldBar = new Span();
    fieldBar.addClassName("search-bar");
    fieldBar.add(searchField);

    Span buttonBar = new Span();
    buttonBar.addClassName("button-bar");
    Button metadataDownloadButton = new Button("Download Metadata");
    buttonBar.add(metadataDownloadButton);

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

    Div content = new Div();
    content.addClassName("sample-details-content");

    content.add(buttonAndFieldBar, sampleExperimentTabSheet);
    add(content);

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

  private static TextField createSearchField() {
    TextField textField = new TextField();
    textField.setPlaceholder("Search");
    textField.setPrefixComponent(VaadinIcon.SEARCH.create());
    textField.setValueChangeMode(ValueChangeMode.EAGER);
    return textField;
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
   * Adds the provided listener
   *
   * @param batchRegistrationListener listener notified if the user intends to create a batch
   */
  public void addCreateBatchListener(
      ComponentEventListener<RegisterBatchClicked> batchRegistrationListener) {
    addListener(RegisterBatchClicked.class, batchRegistrationListener);
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
    noSamplesDefinedCard.addDisclaimerConfirmedListener(
        event -> fireEvent(new RegisterBatchClicked(this, event.isFromClient())));
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
    sampleGrid.addColumn(preview -> preview.species().getLabel()).setHeader("Species").setSortProperty("species")
        .setTooltipGenerator(preview -> ontologyToolTipGenerator(preview.species()));
    sampleGrid.addColumn(preview -> preview.specimen().getLabel()).setHeader("Specimen")
        .setSortProperty("specimen").setTooltipGenerator(preview -> ontologyToolTipGenerator(preview.specimen()));
    sampleGrid.addColumn(preview -> preview.analyte().getLabel()).setHeader("Analyte").setSortProperty("analyte")
        .setTooltipGenerator(preview -> ontologyToolTipGenerator(preview.analyte()));
    sampleGrid.addColumn(SamplePreview::analysisMethod).setHeader("Analysis to Perform")
        .setSortProperty("analysisMethod").setTooltipGenerator(SamplePreview::analysisMethod);
    sampleGrid.addColumn(SamplePreview::comment).setHeader("Comment").setSortProperty("comment")
        .setTooltipGenerator(SamplePreview::comment);
    sampleGrid.addClassName("sample-grid");
    sampleGrid.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
    return sampleGrid;
  }

  private static String ontologyToolTipGenerator(OntologyClassDTO ontologyClass) {
    StringBuilder builder = new StringBuilder();
    builder.append(ontologyClass.getName());
    if(Ontology.findOntologyByAbbreviation(ontologyClass.getOntology())!=null) {
      builder.append(" (");
      builder.append(Ontology.findOntologyByAbbreviation(ontologyClass.getOntology()).getName()+")");
    }
    return builder.toString();
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

  /**
   * <b>Create Batch Event</b>
   *
   * <p>Indicates that a user wants to create a {@link Batch}
   * within the {@link SampleDetailsComponent} of a project</p>
   */
  public static class RegisterBatchClicked extends ComponentEvent<SampleDetailsComponent> {

    @Serial
    private static final long serialVersionUID = 5351296685318048598L;

    public RegisterBatchClicked(SampleDetailsComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }
}
