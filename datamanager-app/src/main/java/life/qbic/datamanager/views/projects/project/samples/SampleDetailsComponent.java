package life.qbic.datamanager.views.projects.project.samples;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.shared.Registration;
import java.io.InputStream;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import life.qbic.application.commons.FileNameFormatter;
import life.qbic.application.commons.time.DateTimeFormat;
import life.qbic.datamanager.files.export.download.DownloadStreamProvider;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.UiHandle;
import life.qbic.datamanager.views.general.PageArea;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.download.DownloadComponent;
import life.qbic.datamanager.views.general.pagination.ListState;
import life.qbic.datamanager.views.general.pagination.PaginatedGrid;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.projectmanagement.application.api.AsyncProjectService;
import life.qbic.projectmanagement.application.api.AsyncProjectService.SamplePreviewFilter;
import life.qbic.projectmanagement.application.sample.SamplePreview;
import life.qbic.projectmanagement.domain.model.experiment.Experiment;
import life.qbic.projectmanagement.domain.model.project.Project;
import life.qbic.projectmanagement.domain.model.sample.Sample;
import life.qbic.projectmanagement.domain.model.sample.SampleId;
import org.jspecify.annotations.NonNull;
import org.springframework.util.MimeType;

/**
 * Sample Details Component
 * <p>
 * Component embedded within the {@link SampleInformationMain}. It allows the user to see the
 * information associated for all {@link Sample} of each {@link Experiment} within a
 * {@link Project} Additionally it enables the user to register and edit {@link Sample} via the
 * contained
 * {@link
 * life.qbic.datamanager.views.projects.project.samples.registration.batch.RegisterSampleBatchDialog}.
 * <p>
 * The sample list is rendered by a reusable {@link PaginatedGrid} (FEAT-PAG-LIST-02): only the
 * current page is fetched and rendered, a pager reports location and total, and an identifier-based
 * cross-page {@link life.qbic.datamanager.views.general.pagination.Selection} of sample IDs survives
 * page, filter, and sort changes. Bulk actions (export / edit / delete) apply to the full cross-page
 * selection. The list state is mirrored into the browser URL by the owning route view (USER-R-03).
 */
public class SampleDetailsComponent extends PageArea implements Serializable {

  @Serial
  private static final long serialVersionUID = 2893730975944372088L;
  private final transient AsyncProjectService asyncProjectService;
  private final MessageSourceNotificationFactory messageFactory;

  private final UiHandle uiHandle = new UiHandle();

  private final DownloadComponent downloadComponent = new DownloadComponent();

  private final AtomicReference<String> clientTimeZone = new AtomicReference<>("UTC");
  private PaginatedGrid<SamplePreview> paginatedGrid;
  private String projectId;
  private String experimentId;
  private String projectCode;
  private final Button exportButton = new Button("Export", VaadinIcon.DOWNLOAD.create());
  private final Button editButton = new Button("Edit", VaadinIcon.EDIT.create());
  private final Button deleteButton = new Button("Delete", VaadinIcon.TRASH.create());

  public SampleDetailsComponent(
      @NonNull AsyncProjectService asyncProjectService,
      @NonNull MessageSourceNotificationFactory messageFactory,
      @NonNull Context context) {
    this.messageFactory = Objects.requireNonNull(messageFactory);
    this.asyncProjectService = Objects.requireNonNull(asyncProjectService);
    this.projectId = context.projectId().orElseThrow().value();
    this.experimentId = context.experimentId().orElseThrow().value();
    this.projectCode = context.projectCode().orElseThrow();
    add(downloadComponent);
    addClassNames("sample-details-component", "sample-details-content");

    addAttachListener(event -> {
      uiHandle.bind(event.getUI());
      event.getUI().getPage().getExtendedClientDetails().refresh(
          receiver -> {
            clientTimeZone.set(receiver.getTimeZoneId());
          });
    });

    addDetachListener(ignored -> uiHandle.unbind());

    Grid<SamplePreview> sampleGrid = createSamplePreviewGrid();
    paginatedGrid = new PaginatedGrid<>(sampleGrid, this::loadPage,
        preview -> preview.sampleId().value(), "sample", SampleSort.DEFAULT);
    paginatedGrid.setSearchPlaceholder("Search samples");
    paginatedGrid.addSelectionChangeListener(
        event -> updateActionButtons());
    paginatedGrid.addClassNames("width-full");

    Div actions = createActionsBar();
    add(actions, paginatedGrid);

    updateActionButtons();
  }

  private Div createActionsBar() {
    Button registerButton = new Button("Register Samples", VaadinIcon.PLUS.create());
    registerButton.addClassName("button-color-primary");
    registerButton.addClickListener(clicked -> fireEvent(new SampleRegistrationRequested(this, true)));

    exportButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    exportButton.addClickListener(clicked -> onExportClicked());

    editButton.addClickListener(clicked -> onEditClicked());

    deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
    deleteButton.addClickListener(clicked -> onDeleteClicked());

    Div actions = new Div(registerButton, exportButton, editButton, deleteButton);
    actions.addClassName("sample-actions");
    return actions;
  }

  private void updateActionButtons() {
    boolean hasSelection = paginatedGrid.selectedIds().size() > 0;
    exportButton.setEnabled(hasSelection);
    editButton.setEnabled(hasSelection);
    deleteButton.setEnabled(hasSelection);
  }

  private void onExportClicked() {
    Set<String> selectedSampleIds = paginatedGrid.selectedIds();
    if (selectedSampleIds.isEmpty()) {
      messageFactory.toast("sample.no-sample-selected", new Object[]{}, getLocale())
          .open();
      return;
    }
    triggerSampleMetadataDownload(selectedSampleIds, projectId, experimentId, projectCode);
  }

  private void onEditClicked() {
    List<SampleId> selectedSampleIds = selectedSampleIds().stream()
        .toList();
    if (selectedSampleIds.isEmpty()) {
      messageFactory.toast("sample.no-sample-selected", new Object[]{}, getLocale()).open();
      return;
    }
    fireEvent(new SampleEditRequested(selectedSampleIds, this, true));
  }

  private void onDeleteClicked() {
    List<SampleId> selectedSampleIds = selectedSampleIds().stream()
        .toList();
    if (selectedSampleIds.isEmpty()) {
      messageFactory.toast("sample.no-sample-selected", new Object[]{}, getLocale()).open();
      return;
    }
    fireEvent(new SampleDeletionRequested(selectedSampleIds, this, true));
  }

  private Set<SampleId> selectedSampleIds() {
    return paginatedGrid.selectedIds().stream()
        .map(SampleId::parse)
        .collect(Collectors.toSet());
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

  private Grid<SamplePreview> createSamplePreviewGrid() {
    Grid<SamplePreview> sampleGrid = new Grid<>();
    var sampleIdColumn = sampleGrid.addColumn(SamplePreview::sampleCode)
        .setHeader("Sample ID")
        .setSortProperty("sampleId")
        .setComparator(SamplePreview::sampleCode)
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
        .setSortProperty("batch")
        .setTooltipGenerator(SamplePreview::batchLabel)
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(createConditionRenderer())
        .setHeader("Condition")
        .setSortProperty("condition")
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
    sampleGrid.addColumn(preview -> formatTime(preview.registrationTime(),
            DateTimeFormat.SIMPLE_DATE_TIME_SHORT))
        .setHeader("Registration time")
        .setSortProperty("registrationTime")
        .setComparator(SamplePreview::registrationTime)
        .setAutoWidth(true)
        .setResizable(true);
    sampleGrid.addColumn(preview -> formatTime(preview.lastModified(),
            DateTimeFormat.SIMPLE_DATE_TIME_SHORT))
        .setHeader("Modification time")
        .setSortProperty("lastModified")
        .setComparator(SamplePreview::lastModified)
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
    sampleGrid.sort(GridSortOrder.asc(sampleIdColumn).build());
    return sampleGrid;
  }

  private String formatTime(Instant instant, DateTimeFormat dateTimeFormat) {
    if (instant == null) {
      return "";
    }
    return DateTimeFormat.asJavaFormatter(dateTimeFormat, ZoneId.of(clientTimeZone.get()))
        .format(instant);
  }

  /**
   * Loads a single page of samples for the {@link PaginatedGrid}. Translates the list state into
   * the backend lookup and returns the page items together with the total count of samples matching
   * the active filter.
   */
  private PaginatedGrid.Page<SamplePreview> loadPage(ListState state) {
    int offset = (state.page() - 1) * state.pageSize();
    int limit = state.pageSize();
    // The backend applies the sort orders as given, so append the deterministic sample-code
    // tie-break to keep offset/limit pagination stable across page boundaries (equal sort values
    // would otherwise duplicate or drop rows).
    List<life.qbic.projectmanagement.application.api.AsyncProjectService.SortOrder<AsyncProjectService.SamplePreviewSortKey>> sortOrders =
        new java.util.ArrayList<>();
    sortOrders.add(SampleSort.toApiSortOrder(state.sort()));
    if (!state.sort().propertyName().equals("sampleId")) {
      sortOrders.add(new life.qbic.projectmanagement.application.api.AsyncProjectService.SortOrder<>(
          AsyncProjectService.SamplePreviewSortKey.SAMPLE_ID,
          life.qbic.projectmanagement.application.api.AsyncProjectService.SortDirection.ASC));
    }
    SamplePreviewFilter filter = new SamplePreviewFilter(state.filter(), sortOrders);
    int total = asyncProjectService.countSamples(projectId, experimentId, filter).blockOptional()
        .orElse(0);
    List<SamplePreview> page = asyncProjectService.getSamplePreviews(projectId, experimentId,
            offset, limit, filter)
        .collectList().blockOptional().orElse(List.of());
    return new PaginatedGrid.Page<>(page, total);
  }

  private void triggerSampleMetadataDownload(
      @NonNull Set<String> sampleIds,
      String projectId,
      String experimentId,
      String projectCode) {

    var pendingTaskToast = messageFactory.pendingTaskToast("sample.fetching-metadata",
        new Object[]{sampleIds.size()}, getLocale());
    pendingTaskToast.open();
    asyncProjectService.sampleInformationTemplate(projectId, experimentId, sampleIds,
            MimeType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .subscribe(digitalObject -> uiHandle
            .onUiAndPush(() -> {
              pendingTaskToast.close();
              messageFactory.toast("sample.metadata-fetched", new Object[]{sampleIds.size()},
                  getLocale()).open();
              downloadComponent.trigger(new DownloadStreamProvider() {
                @Override
                public String getFilename() {
                  return FileNameFormatter.formatWithTimestampedSimple(LocalDate.now(), projectCode,
                      "sample_metadata", "xlsx");
                }

                @Override
                public InputStream getStream() {
                  return digitalObject.content();
                }

                @Override
                public String getContentType() {
                  return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                }

                @Override
                public Optional<Long> contentLength() {
                  return Optional.empty();
                }
              });
            }));
  }

  /**
   * Applies an externally provided list state (initial load, URL back/forward, shared link) and
   * reloads the page.
   */
  public void applyExternalState(ListState state) {
    paginatedGrid.applyExternalState(state);
  }

  /**
   * Reloads the current page with the current list state (e.g. after registration, edit or
   * deletion).
   */
  public void refresh() {
    paginatedGrid.refresh();
  }

  /**
   * @return the currently applied list state
   */
  public ListState listState() {
    return paginatedGrid.listState();
  }

  /**
   * Registers a listener notified whenever a page is loaded, so the owning route view can mirror
   * the list state into the URL.
   */
  public void addPageLoadedListener(ComponentEventListener<PaginatedGrid.PageLoadedEvent> listener) {
    paginatedGrid.addPageLoadedListener(listener);
  }

  /**
   * @return the wrapped {@link PaginatedGrid}
   */
  public PaginatedGrid<SamplePreview> paginatedGrid() {
    return paginatedGrid;
  }

  /**
   * Register a {@link ComponentEventListener} that gets informed with a
   * {@link SampleRegistrationRequested} as soon as a user wants to register samples.
   *
   * @param listener a listener on the sample registration trigger
   */
  public Registration addSampleRegistrationListener(
      ComponentEventListener<SampleRegistrationRequested> listener) {
    return addListener(SampleRegistrationRequested.class, listener);
  }

  /**
   * Register a {@link ComponentEventListener} that gets informed with a {@link SampleEditRequested}
   * as soon as a user wants to edit samples.
   *
   * @param listener a listener on the sample edit trigger
   */
  public Registration addSampleEditListener(ComponentEventListener<SampleEditRequested> listener) {
    return addListener(SampleEditRequested.class, listener);
  }

  /**
   * Register a {@link ComponentEventListener} that gets informed with a
   * {@link SampleDeletionRequested} as soon as a user wants to delete selected samples.
   *
   * @param listener a listener on the sample deletion trigger
   */
  public Registration addSampleDeletionListener(
      ComponentEventListener<SampleDeletionRequested> listener) {
    return addListener(SampleDeletionRequested.class, listener);
  }

  /**
   * <b>Sample Registration Requested</b>
   *
   * <p>Indicates that a user wants to register samples within the {@link SampleDetailsComponent} of
   * a project.</p>
   */
  public static class SampleRegistrationRequested extends ComponentEvent<SampleDetailsComponent> {

    @Serial
    private static final long serialVersionUID = 8039568599366236205L;

    public SampleRegistrationRequested(SampleDetailsComponent source, boolean fromClient) {
      super(source, fromClient);
    }
  }

  /**
   * <b>Sample Edit Requested</b>
   *
   * <p>Indicates that a user wants to edit the samples within the {@link SampleDetailsComponent} of
   * a project.</p>
   */
  public static class SampleEditRequested extends ComponentEvent<SampleDetailsComponent> {

    @Serial
    private static final long serialVersionUID = -2696352875376621564L;
    private final List<SampleId> sampleIds;

    public SampleEditRequested(List<SampleId> sampleIds, SampleDetailsComponent source,
        boolean fromClient) {
      super(source, fromClient);
      this.sampleIds = sampleIds;
    }

    public List<SampleId> sampleIds() {
      return sampleIds;
    }
  }

  /**
   * <b>Sample Deletion Requested</b>
   *
   * <p>Indicates that a user wants to delete the selected samples within the
   * {@link SampleDetailsComponent} of a project.</p>
   */
  public static class SampleDeletionRequested extends ComponentEvent<SampleDetailsComponent> {

    @Serial
    private static final long serialVersionUID = 3051871590996074855L;
    private final List<SampleId> sampleIds;

    public SampleDeletionRequested(List<SampleId> sampleIds, SampleDetailsComponent source,
        boolean fromClient) {
      super(source, fromClient);
      this.sampleIds = sampleIds;
    }

    public List<SampleId> sampleIds() {
      return sampleIds;
    }
  }
}