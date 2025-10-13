package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
import life.qbic.application.commons.SortOrder;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.UiHandle;
import life.qbic.datamanager.views.general.MultiSelectLazyLoadingGrid;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.grid.Filter;
import life.qbic.datamanager.views.general.grid.FilterGrid;
import life.qbic.datamanager.views.general.grid.component.FilterGridTab;
import life.qbic.datamanager.views.general.grid.component.FilterGridTabSheet;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.RequestFailedException;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SamplePreviewFilter;
import life.qbic.projectmanagement.application.api.fair.DigitalObject;
import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.domain.model.batch.Batch;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import org.springframework.lang.NonNull;
import org.springframework.util.MimeType;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Sample Details Component
 * <p>
 * Component embedded within the {@link SampleInformationMain}. It allows the user to see the
 * information associated for all {@link Batch} and {@link Sample} of each
 * {@link Experiment within a {@link Project} Additionally it enables the user to register new
 * {@link Batch} and {@link Sample} via the contained
 * {@link
 * life.qbic.datamanager.views.projects.project.samples.registration.batch.RegisterSampleBatchDialog}.
 */

public class SampleDetailsComponent extends PageArea implements Serializable {

  @Serial
  private static final long serialVersionUID = 2893730975944372088L;
  private final Span countSpan;
  private final Grid<SamplePreview> sampleGrid;
  private final transient AsyncProjectService asyncProjectService;
  private final MessageSourceNotificationFactory messageFactory;
  private Context context;

  private final UiHandle uiHandle = new UiHandle();

  public SampleDetailsComponent(AsyncProjectService asyncProjectService,
      MessageSourceNotificationFactory messageFactory) {
    this.messageFactory = Objects.requireNonNull(messageFactory);
    this.asyncProjectService = Objects.requireNonNull(asyncProjectService);
    addClassName("sample-details-component");
    //sampleGrid = createSampleGrid();
    sampleGrid = new Grid<>();
    Div content = new Div();
    content.addClassName("sample-details-content");
    countSpan = new Span();
    countSpan.addClassName("sample-count");
    setSampleCount(0);

    //content.add(countSpan, sampleGrid);
    add(content);

    addAttachListener(event -> {
      uiHandle.bind(event.getUI());
    });

    addDetachListener(ignored -> {
      uiHandle.unbind();
    });
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

  private static MultiSelectLazyLoadingGrid<SamplePreview> createSampleGrid() {
    MultiSelectLazyLoadingGrid<SamplePreview> sampleGrid = new MultiSelectLazyLoadingGrid<>();
    sampleGrid.addColumn(SamplePreview::sampleCode)
        .setHeader("Sample ID")
        .setSortProperty("sampleCode")
        .setAutoWidth(true)
        .setFlexGrow(0)
        .setTooltipGenerator(SamplePreview::sampleCode)
        .setFrozen(true);
    sampleGrid.addColumn(SamplePreview::sampleName)
        .setHeader("Sample Name")
        .setSortProperty("sampleName")
        .setTooltipGenerator(SamplePreview::sampleName)
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(SamplePreview::biologicalReplicate)
        .setHeader("Biological Replicate")
        .setSortProperty("biologicalReplicate")
        .setTooltipGenerator(SamplePreview::biologicalReplicate)
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(SamplePreview::batchLabel)
        .setHeader("Batch")
        .setSortProperty("batchLabel")
        .setTooltipGenerator(SamplePreview::batchLabel)
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(createConditionRenderer())
        .setHeader("Condition")
        .setSortProperty("experimentalGroup")
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(preview -> preview.species().getLabel())
        .setHeader("Species")
        .setSortProperty("species")
        .setTooltipGenerator(preview -> preview.species().formatted())
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(preview -> preview.specimen().getLabel())
        .setHeader("Specimen")
        .setSortProperty("specimen")
        .setTooltipGenerator(preview -> preview.specimen().formatted())
        .setAutoWidth(true);
    sampleGrid.addColumn(preview -> preview.analyte().getLabel())
        .setHeader("Analyte")
        .setSortProperty("analyte")
        .setTooltipGenerator(preview -> preview.analyte().formatted())
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(preview -> preview.analysisMethod().label())
        .setHeader("Analysis to Perform")
        .setSortProperty("analysisMethod")
        .setTooltipGenerator(samplePreview -> samplePreview.analysisMethod().label())
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(SamplePreview::comment)
        .setHeader("Comment")
        .setSortProperty("comment")
        .setTooltipGenerator(SamplePreview::comment)
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addClassName("sample-grid");
    sampleGrid.setColumnReorderingAllowed(true);
    return sampleGrid;
  }

  public void onSearchFieldValueChanged(String searchValue) {
    updateSampleGridDataProvider(context.projectId().orElseThrow(),
        context.experimentId().orElseThrow(), searchValue);
  }

  private void updateSampleGridDataProvider(ProjectId projectId, ExperimentId experimentId,
      String filter) {
    sampleGrid.setItems(query -> {
      List<SortOrder> sortOrders = query.getSortOrders().stream().map(
              it -> new SortOrder(it.getSorted(), it.getDirection().equals(SortDirection.ASCENDING)))
          .collect(Collectors.toList());
      // if no order is provided by the grid order by last modified (least priority)
      sortOrders.add(SortOrder.of("sampleCode").ascending());
      return asyncProjectService.getSamplePreviewsOld(projectId.value(), experimentId.value(),
              query.getOffset(), query.getLimit(), List.copyOf(sortOrders), filter)
          .doOnError(RequestFailedException.class, this::handleRequestFailed).toStream();

    });
    sampleGrid.getLazyDataView().addItemCountChangeListener(
        countChangeEvent -> setSampleCount((int) sampleGrid.getLazyDataView().getItems().count()));
    sampleGrid.recalculateColumnWidths();
  }

  private void handleRequestFailed(RequestFailedException e) {
    getUI().ifPresent(ui -> ui.access(
        () -> messageFactory.toast("sample.query.failed", new Object[]{}, getLocale())));
  }

  /**
   * Propagates the context to internal components.
   *
   * @param context the context in which the user is.
   */
  public void setContext(Context context) {
    if (context.experimentId().isEmpty()) {
      throw new ApplicationException("no experiment id in context " + context);
    }
    if (context.projectId().isEmpty()) {
      throw new ApplicationException("no project id in context " + context);
    }
    this.context = context;

    // FIXME remove before merge
    var projectId = context.projectId().orElseThrow().value();
    var experimentId = context.experimentId().orElseThrow().value();

    var multiSelectGrid = createSampleGrid();

    var filterGrid = new FilterGrid<>(SamplePreview.class, multiSelectGrid,
        DataProvider.fromFilteringCallbacks(query -> {
          var filter = query.getFilter().orElse(new SampleNameFilter(""));
          var offset = query.getOffset();
          var limit = query.getLimit();

          var sampleFilter = createSamplePreviewFilter(filter);

          return asyncProjectService.getSamplePreviews(projectId,
                  experimentId, offset, limit, sampleFilter)
              .collectList().blockOptional().orElse(List.of()).stream();
        }, query -> {
          var sampleFilter = createSamplePreviewFilter(query.getFilter().orElseThrow());

          return asyncProjectService.countSamples(projectId,
              experimentId, sampleFilter).blockOptional().orElse(0);
        }), new SampleNameFilter(""), (filter, term) -> {
      filter.setSearchTerm(term);
      return filter;
    });

    var filterTab = new FilterGridTab<>("Samples", filterGrid);
    var filterTabSheet = new FilterGridTabSheet(filterTab);

    filterTabSheet.setCaptionPrimaryAction("Register Samples");
    filterTabSheet.setCaptionFeatureAction("Export");
    filterTabSheet.hidePrimaryActionButton();

    filterTabSheet.addPrimaryFeatureButtonListener(
        ignored -> filterTabSheet.whenSelectedGrid(SamplePreview.class,
            grid -> grid.selectedElements()));


    removeAll();
    add(filterTabSheet);

    // Update sample counter badge
    asyncProjectService.countSamples(projectId, experimentId)
        .onErrorResume(error -> Mono.just(0))
        .subscribe(count -> uiHandle.onUiAndPush(() -> filterTab.setItemCount(count)));
  }


  private static class SampleNameFilter implements Filter<SamplePreview> {

    private String filter;

    public SampleNameFilter(@NonNull String filter) {
      this.filter = Objects.requireNonNull(filter);
    }

    @Override
    public void setSearchTerm(String searchTerm) {
      filter = Optional.ofNullable(searchTerm).orElse("");
    }

    @Override
    public String searchTerm() {
      return filter;
    }

    @Override
    public boolean test(SamplePreview data) {
      return data.sampleName().toLowerCase().contains(filter.toLowerCase());
    }
  }

  private static SamplePreviewFilter createSamplePreviewFilter(Filter<SamplePreview> filterUI) {
    return new SamplePreviewFilter(filterUI.searchTerm(), List.of());
  }

  private void setSampleCount(int i) {
    countSpan.setText(i + " samples");
  }

}
