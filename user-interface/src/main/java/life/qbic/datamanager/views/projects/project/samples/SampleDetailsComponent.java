package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import life.qbic.application.commons.ApplicationException;
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
import life.qbic.projectmanagement.application.api.AsyncProjectService.SamplePreviewFilter;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SamplePreviewSortKey;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SortDirection;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SortOrder;
import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.domain.model.batch.Batch;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Mono;

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
    addClassName("sample-details-content");
    countSpan = new Span();
    countSpan.addClassName("sample-count");
    setSampleCount(0);

    //content.add(countSpan, sampleGrid);

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
        .setSortProperty(UiSortKey.SAMPLE_ID.value())
        .setAutoWidth(true)
        .setFlexGrow(0)
        .setTooltipGenerator(SamplePreview::sampleCode)
        .setFrozen(true);
    sampleGrid.addColumn(SamplePreview::sampleName)
        .setHeader("Sample Name")
        .setSortProperty(UiSortKey.SAMPLE_NAME.value())
        .setTooltipGenerator(SamplePreview::sampleName)
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(SamplePreview::biologicalReplicate)
        .setHeader("Biological Replicate")
        .setSortProperty(UiSortKey.BIOLOGICAL_REPLICATE.value())
        .setTooltipGenerator(SamplePreview::biologicalReplicate)
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(SamplePreview::batchLabel)
        .setHeader("Batch")
        .setSortProperty(UiSortKey.BATCH.value())
        .setTooltipGenerator(SamplePreview::batchLabel)
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(createConditionRenderer())
        .setHeader("Condition")
        .setSortProperty(UiSortKey.CONDITION.value())
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(preview -> preview.species().getLabel())
        .setHeader("Species")
        .setSortProperty(UiSortKey.SPECIES.value())
        .setTooltipGenerator(preview -> preview.species().formatted())
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(preview -> preview.specimen().getLabel())
        .setHeader("Specimen")
        .setSortProperty(UiSortKey.SPECIMEN.value())
        .setTooltipGenerator(preview -> preview.specimen().formatted())
        .setAutoWidth(true);
    sampleGrid.addColumn(preview -> preview.analyte().getLabel())
        .setHeader("Analyte")
        .setSortProperty(UiSortKey.ANALYTE.value())
        .setTooltipGenerator(preview -> preview.analyte().formatted())
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(preview -> preview.analysisMethod().label())
        .setHeader("Analysis to Perform")
        .setSortProperty(UiSortKey.ANALYSIS_METHOD.value())
        .setTooltipGenerator(samplePreview -> samplePreview.analysisMethod().label())
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(SamplePreview::comment)
        .setHeader("Comment")
        .setSortProperty(UiSortKey.COMMENT.value())
        .setTooltipGenerator(SamplePreview::comment)
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addClassName("sample-grid");
    sampleGrid.setColumnReorderingAllowed(true);
    return sampleGrid;
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
          var sortOrders = sortOrdersToApi(query.getSortOrders());

          var sampleFilter = createSamplePreviewFilter(filter, sortOrders);

          return asyncProjectService.getSamplePreviews(projectId,
                  experimentId, offset, limit, sampleFilter)
              .collectList().blockOptional().orElse(List.of()).stream();
        }, query -> {

          var sortOrders = sortOrdersToApi(query.getSortOrders());
          var sampleFilter = createSamplePreviewFilter(query.getFilter().orElseThrow(), sortOrders);

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
            FilterGrid::selectedElements));

    removeAll();
    add(filterTabSheet);

    // Update sample counter badge
    asyncProjectService.countSamples(projectId, experimentId)
        .onErrorResume(error -> Mono.just(0))
        .subscribe(count -> uiHandle.onUiAndPush(() -> filterTab.setItemCount(count)));
  }

  private static List<SortOrder<SamplePreviewSortKey>> sortOrdersToApi(
      List<com.vaadin.flow.data.provider.QuerySortOrder> uiSortOrders) throws IllegalArgumentException{
    return uiSortOrders.stream()
        .map(SampleDetailsComponent::sortOrdersToApi)
        .toList();
  }

  private static SortOrder<SamplePreviewSortKey> sortOrdersToApi(QuerySortOrder uiSortOrder) throws IllegalArgumentException{
    var uiSortKeyValue = uiSortOrder.getSorted();
    var uiSortKey = UiSortKey.from(uiSortKeyValue).orElseThrow(() -> new IllegalArgumentException("No ui sort key provided for value: " + uiSortKeyValue));
    var apiKey = SORT_KEY_MAP.get(uiSortKey);
    if  (apiKey == null) {
      throw new IllegalArgumentException("No api key provided for value: " + uiSortKey);
    }
    return new SortOrder<>(apiKey,sortDirectionToApi(uiSortOrder.getDirection()));
  }

  private static SortDirection sortDirectionToApi(com.vaadin.flow.data.provider.SortDirection uiSortDirection) {
    return uiSortDirection == com.vaadin.flow.data.provider.SortDirection.ASCENDING ? SortDirection.ASC : SortDirection.DESC;
  }

  private static final Map<UiSortKey, SamplePreviewSortKey> SORT_KEY_MAP = new EnumMap<>(UiSortKey.class);

  static {
    SORT_KEY_MAP.put(UiSortKey.SAMPLE_ID, SamplePreviewSortKey.SAMPLE_ID);
    SORT_KEY_MAP.put(UiSortKey.SAMPLE_NAME, SamplePreviewSortKey.SAMPLE_NAME);
    SORT_KEY_MAP.put(UiSortKey.BIOLOGICAL_REPLICATE, SamplePreviewSortKey.BIOLOGICAL_REPLICATE);
    SORT_KEY_MAP.put(UiSortKey.BATCH, SamplePreviewSortKey.BATCH);
    SORT_KEY_MAP.put(UiSortKey.CONDITION, SamplePreviewSortKey.CONDITION);
    SORT_KEY_MAP.put(UiSortKey.SPECIES, SamplePreviewSortKey.SPECIES);
    SORT_KEY_MAP.put(UiSortKey.SPECIMEN, SamplePreviewSortKey.SPECIMEN);
    SORT_KEY_MAP.put(UiSortKey.ANALYTE, SamplePreviewSortKey.ANALYTE);
    SORT_KEY_MAP.put(UiSortKey.ANALYSIS_METHOD, SamplePreviewSortKey.ANALYSIS_METHOD);
    SORT_KEY_MAP.put(UiSortKey.COMMENT, SamplePreviewSortKey.COMMENT);
  }

  enum UiSortKey {
    SAMPLE_ID("sampleId"),
    SAMPLE_NAME("sampleName"),
    BIOLOGICAL_REPLICATE("biologicalReplicate"),
    BATCH("batch"),
    CONDITION("condition"),
    SPECIES("species"),
    SPECIMEN("specimen"),
    ANALYTE("analyte"),
    ANALYSIS_METHOD("analysisMethod"),
    COMMENT("comment");


    private static final Map<String, UiSortKey> LOOKUP = Arrays.stream(UiSortKey.values()).collect(
        Collectors.toMap(UiSortKey::value, Function.identity()));

    private final String value;

    UiSortKey(String value) {
      this.value = value;
    }

    static Optional<UiSortKey> from(String value) {
      return Optional.ofNullable(LOOKUP.getOrDefault(value, null));
    }

    String value() {
      return value;
    }
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

  private static SamplePreviewFilter createSamplePreviewFilter(
      Filter<SamplePreview> filterUI,
      List<SortOrder<SamplePreviewSortKey>> sortOrders) {
    return new SamplePreviewFilter(filterUI.searchTerm(), sortOrders);
  }

  private void setSampleCount(int i) {
    countSpan.setText(i + " samples");
  }

}
