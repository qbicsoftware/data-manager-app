package life.qbic.datamanager.views.projects.project.dataset;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.page.History;
import com.vaadin.flow.component.page.History.HistoryStateChangeEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import life.qbic.application.commons.ApplicationException;
import life.qbic.datamanager.views.AppRoutes.ProjectRoutes;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.pagination.ListState;
import life.qbic.datamanager.views.general.pagination.ListStateCodec;
import life.qbic.datamanager.views.projects.project.ProjectMainLayout;
import life.qbic.datamanager.views.projects.project.samples.SampleInformationMain;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.api.ProjectQueryService;
import life.qbic.projectmanagement.application.query.ExperimentProgress;
import life.qbic.projectmanagement.domain.model.experiment.ExperimentId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The experiment's <b>Datasets</b>: one surface for the two ordered process steps that
 * were previously two pages.
 *
 * <p>A measurement is the metadata describing how a result was produced; the measurement data is
 * the result itself. They belong together for the reader but are produced by different systems:
 * the metadata is registered here and mints the measurement code, the data is produced by the
 * facility, transferred out of band and synced back keyed by that code. Neither page could state
 * that relationship before: the measurements page had no data field, and the data page could not
 * represent a measurement whose data had not arrived, because its views inner-join the data table.
 * </p>
 *
 * <p>The page therefore leads with the datasets: one card per measurement method, each stating how
 * many entries it holds, how many wait for data and how much data has arrived. Choosing a card
 * activates the list below it. The datasets are the structure, not a filter — an earlier revision
 * offered them in a dropdown, and a reader who did not open it reasonably concluded the page showed
 * all measurements.</p>
 *
 * <p>A measurement dataset is the experiment's data for <em>one</em> method, so an experiment has
 * one per method it measured, not one in total.</p>
 *
 * @since 1.21.0
 */
@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/experiments/:experimentId?/dataset",
    layout = ProjectMainLayout.class)
@PermitAll
public class MeasurementDatasetMain extends Main
    implements BeforeEnterObserver, BeforeLeaveObserver {

  @Serial
  private static final long serialVersionUID = -1L;

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";
  public static final String EXPERIMENT_ID_ROUTE_PARAMETER = "experimentId";

  private static final String DATASET_PARAMETER = "dataset";

  private static final Logger log = LoggerFactory.logger(MeasurementDatasetMain.class);

  /**
   * The download service's measurement endpoint.
   *
   * <p>Known debt: the application constructs a data URL from a configured prefix, asserting an
   * endpoint contract it neither owns nor can verify. A later increment replaces construction with
   * discovery; until then the prefix stays a configuration value rather than a hard-coded host.</p>
   */
  @org.springframework.beans.factory.annotation.Value("${server.download.api.measurement.url:}")
  private String downloadEndpoint = "";

  private final transient life.qbic.datamanager.configuration.UploadConfiguration
      uploadConfiguration;

  private final transient ProjectQueryService queryService;
  private final transient life.qbic.projectmanagement.application.api.AsyncProjectService asyncService;
  private final transient life.qbic.projectmanagement.application.measurement.MeasurementService
      measurementService;
  private final transient life.qbic.datamanager.security.UserPermissions userPermissions;
  private final transient life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory
      notifications;
  /**
   * The single bridge back onto the UI thread.
   *
   * <p>Every reactive result is delivered on the scheduler thread, and almost everything this page
   * does with a result touches the UI: opening a toast, triggering a download (which assigns an
   * {@code href} and therefore needs a current UI), rendering the reloaded page. Reaching the UI
   * directly from those callbacks fails with "No currently active UI found".</p>
   */
  private final life.qbic.datamanager.views.UiHandle uiHandle =
      new life.qbic.datamanager.views.UiHandle();

  private final com.vaadin.flow.component.html.Div downloadComponentHost =
      new com.vaadin.flow.component.html.Div();
  private final life.qbic.datamanager.views.general.download.DownloadComponent downloadComponent =
      new life.qbic.datamanager.views.general.download.DownloadComponent();

  private final H1 title = new H1("Datasets");
  private final com.vaadin.flow.component.button.Button registerButton =
      new com.vaadin.flow.component.button.Button("Register measurements",
          com.vaadin.flow.component.icon.VaadinIcon.PLUS.create());
  private final Span statusLine = new Span();
  private final MeasurementDatasetSelector datasetSelector = new MeasurementDatasetSelector();
  private final Div content = new Div();

  private History.HistoryStateChangeHandler routerHistoryStateChangeHandler;
  private final History.HistoryStateChangeHandler listStateHistoryHandler =
      this::onHistoryStateChange;
  private String basePath;
  private boolean suppressUrlWrite;

  private MeasurementDatasetDetailsComponent detailsComponent;
  private Context context;

  public MeasurementDatasetMain(@Autowired ProjectQueryService queryService,
      @Autowired life.qbic.projectmanagement.application.api.AsyncProjectService asyncService,
      @Autowired life.qbic.projectmanagement.application.measurement.MeasurementService
          measurementService,
      @Autowired life.qbic.datamanager.security.UserPermissions userPermissions,
      @Autowired life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory
          notifications,
      @Autowired life.qbic.datamanager.configuration.UploadConfiguration uploadConfiguration) {
    this.queryService = Objects.requireNonNull(queryService, "queryService must not be null");
    this.asyncService = Objects.requireNonNull(asyncService);
    this.measurementService = Objects.requireNonNull(measurementService);
    this.userPermissions = Objects.requireNonNull(userPermissions);
    this.notifications = Objects.requireNonNull(notifications);
    this.uploadConfiguration = Objects.requireNonNull(uploadConfiguration);
    add(downloadComponentHost, downloadComponent);
    addClassName("measurement-dataset");
    addClassNames("measurement-dataset-shell");

    title.addClassName("measurement-dataset-title");
    statusLine.addClassName("measurement-dataset-header-status");
    Div titleBlock = new Div(title, statusLine);
    titleBlock.addClassName("measurement-dataset-header-title-block");
    registerButton.addThemeVariants(
        com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
    registerButton.addClickListener(clicked -> openRegistrationDialog());
    registerButton.setVisible(false);
    Div header = new Div(titleBlock, registerButton);
    header.addClassName("measurement-dataset-header");

    content.add(header);
    content.addClassName("measurement-dataset-content");
    add(content);

    addAttachListener(event -> uiHandle.bind(event.getUI()));
    addDetachListener(event -> uiHandle.unbind());
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    String projectIdValue = event.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER)
        .orElseThrow(() -> new ApplicationException("Missing project id in route"));
    if (!ProjectId.isValid(projectIdValue)) {
      throw new ApplicationException("invalid project id " + projectIdValue);
    }
    ProjectId projectId = ProjectId.parse(projectIdValue);
    String experimentIdValue = event.getRouteParameters().get(EXPERIMENT_ID_ROUTE_PARAMETER)
        .orElseThrow(() -> new ApplicationException("Missing experiment id in route"));
    if (!ExperimentId.isValid(experimentIdValue)) {
      throw new ApplicationException("invalid experiment id " + experimentIdValue);
    }
    context = new Context().with(projectId).with(ExperimentId.parse(experimentIdValue));
    // The human-facing project code, not the UUID. It is what download file names are built from,
    // and without it the fallback emitted the project's internal identifier into every exported
    // file name. Every other project-scoped view resolves it the same way.
    context = context.withProjectCode(asyncService.getProjectCode(projectIdValue)
        .blockOptional()
        .map(life.qbic.projectmanagement.application.api.AsyncProjectService.ProjectCode::value)
        .orElse(null));

    // URL list-state synchronisation (USER-R-03): capture the router handler, install ours, and
    // seed the list from the URL on direct load / reload / shared links.
    History history = UI.getCurrent().getPage().getHistory();
    History.HistoryStateChangeHandler currentHandler = history.getHistoryStateChangeHandler();
    if (currentHandler != listStateHistoryHandler) {
      routerHistoryStateChangeHandler = currentHandler;
    }
    history.setHistoryStateChangeHandler(listStateHistoryHandler);
    basePath = String.format(ProjectRoutes.MEASUREMENT_DATASET, projectIdValue, experimentIdValue);

    suppressUrlWrite = true;
    try {
      render(projectIdValue, experimentIdValue);
      // The active dataset is addressable too: a link to "the proteomics dataset of this
      // experiment" has to survive a reload and a share.
      event.getLocation().getQueryParameters().getSingleParameter(DATASET_PARAMETER)
          .map(MeasurementDatasetSegment::fromUrlValue)
          .filter(segment -> datasetSelectorContains(segment))
          .ifPresent(segment -> {
            datasetSelector.selectSilently(segment);
            detailsComponent.activate(segment);
          });
      if (detailsComponent != null) {
        ListState urlState = ListStateCodec.parse(event.getLocation().getQueryParameters(),
            MeasurementDataSort.DEFAULT, MeasurementDataSort.allowedSortOrders());
        detailsComponent.applyExternalState(urlState);
      }
    } finally {
      suppressUrlWrite = false;
    }
  }

  /**
   * Renders the page for the current experiment: the counts, the outstanding-work band and the
   * list. The experiment's totals come from the project read model, so the page states its own
   * facts in one query instead of counting what the grid happens to render.
   */
  private void render(String projectId, String experimentId) {
    ExperimentProgress progress = queryService.getExperimentProgress(projectId, experimentId)
        .blockOptional()
        .orElseThrow(() -> new ApplicationException(
            "Unknown experiment " + experimentId + " in project " + projectId));

    int total = progress.measurementCount();
    updateHeader(total);
    registerButton.setVisible(canWrite());

    removeDetailsComponent();
    if (total == 0) {
      // No measurements at all: registration is the only path, so the page states the empty
      // condition and offers nothing it cannot do. The list is withheld rather than rendered
      // empty, and the shared empty state explains what a measurement is.
      content.add(emptyState());
      return;
    }

    detailsComponent = new MeasurementDatasetDetailsComponent(queryService, projectId,
        experimentId);
    detailsComponent.addPageLoadedListener(event -> writeUrl(detailsComponent.listState()));

    // Actions and guidance need services, permissions and project context, so the route view owns
    // them and hands them to the list, which owns the selection they act on.
    var actions = new MeasurementDatasetActions(asyncService, measurementService, projectId);
    detailsComponent.setEditSelectionHandler(selection -> openEditDialog(selection));
    detailsComponent.setExportSelectionHandler(
        selection -> exportMetadata(actions, selection));
    detailsComponent.setDeleteSelectionHandler(
        (selection, onConfirm) -> openDeleteDialog(actions, selection, onConfirm));
    actions.editButton().addClickListener(clicked -> detailsComponent.openEditSelection());
    actions.exportButton().addClickListener(clicked -> detailsComponent.exportSelection());
    actions.deleteButton().addClickListener(clicked -> detailsComponent.deleteSelection(
        selection -> deleteMeasurements(actions, selection)));
    detailsComponent.setActions(actions);
    detailsComponent.setWriteAccess(canWrite());

    var guide = new MeasurementDataDownloadGuide();
    // The router resolves the path, so the link survives the servlet context path.
    guide.setTokenSettingsTarget(
        life.qbic.datamanager.views.account.PersonalAccessTokenMain.class);
    guide.addExportRequestedListener(event -> exportLocations(guide, event.scope()));
    detailsComponent.setDownloadGuide(guide);
    actions.getDataButton().addClickListener(clicked -> openDownloadGuide(guide));

    List<MeasurementDatasetSelector.DatasetSummary> summaries =
        MeasurementDatasetSelector.DatasetSummary.of(progress);
    datasetSelector.setDatasets(summaries);
    datasetSelector.addSelectionChangedListener(event -> {
      detailsComponent.activate(event.segment());
      writeUrl(detailsComponent.listState());
    });
    // The first dataset is active by default, so the page never shows cards with nothing below
    // them. A dataset named in the URL is applied afterwards, in beforeEnter, where the query
    // parameters are available.
    MeasurementDatasetSegment initial = summaries.getFirst().segment();
    datasetSelector.selectSilently(initial);
    detailsComponent.activate(initial);

    if (datasetSelector.getParent().isEmpty()) {
      content.add(datasetSelector);
    }
    add(detailsComponent);
  }

  /**
   * States the experiment's totals once, above the selector.
   *
   * <p>The waiting count used to appear both here and in a stage band, which was the same number
   * twice. It now lives in the per-dataset cards, where it is attributable to a dataset instead of
   * being an experiment-wide figure the reader cannot reconcile with the list below it.</p>
   */
  private void updateHeader(int total) {
    if (total == 0) {
      statusLine.setText("No measurement metadata registered yet");
      return;
    }
    statusLine.setText(total == 1
        ? "1 measurement in 1 dataset"
        : "%d measurements".formatted(total));
  }

  /**
   * Whether the current user may change measurements in this project. Fail-closed until the context
   * is known, so a read-only user is never offered a mutation.
   */
  private boolean canWrite() {
    return context != null && context.projectId().map(userPermissions::editProject).orElse(false);
  }

  /**
   * Opens the guidance on fetching the data, as a dialog.
   *
   * <p>On request rather than on the page: the content is needed by the reader who is about to
   * download and is noise for everyone else, and as a page band it sat below the fold in a full
   * list. It is bound to the same selection state as the other actions, so the location export
   * inside it knows what to export.</p>
   */
  private void openDownloadGuide(MeasurementDataDownloadGuide guide) {
    // The scopes are recomputed on every open, so the dialog states what the *current* selection
    // would export rather than what it was when the page was built.
    if (detailsComponent != null) {
      guide.setSelectionCount(detailsComponent.selectionCount());
    }
    var dialog = life.qbic.datamanager.views.general.dialog.AppDialog.medium();
    life.qbic.datamanager.views.general.dialog.DialogHeader.with(dialog, "Getting the data");
    life.qbic.datamanager.views.general.dialog.DialogFooter.withConfirmOnly(dialog, "Close");
    life.qbic.datamanager.views.general.dialog.DialogBody.withoutUserInput(dialog, guide);
    dialog.registerConfirmAction(dialog::close);
    add(dialog);
    dialog.open();
  }

  /** Opens the metadata export for the given selection. */
  private void exportMetadata(MeasurementDatasetActions actions, MeasurementSelection selection) {
    var pending = notifications.pendingTaskToast("measurement.preparing-download",
        life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory.EMPTY_PARAMETERS,
        getLocale());
    pending.open();
    // The callbacks arrive on the scheduler thread; triggering a download assigns an href and
    // therefore requires the UI thread, so the whole outcome is re-entered through the adapter.
    actions.exportMetadata(selection,
        digitalObject -> uiHandle.onUiAndPush(() -> {
          pending.close();
          triggerDownload("measurements", "xlsx", digitalObject);
        }),
        () -> uiHandle.onUiAndPush(() -> {
          pending.close();
          notifications.toast("task.failed", new Object[]{"Measurement export"}, getLocale())
              .open();
        }));
  }

  /** Opens the delete confirmation, which refuses selections that still have data. */
  private void openDeleteDialog(MeasurementDatasetActions actions, MeasurementSelection selection,
      java.util.function.Consumer<MeasurementSelection> onConfirm) {
    var dialog = actions.deleteDialog(this, selection, onConfirm);
    if (dialog != null) {
      add(dialog);
      dialog.open();
    }
  }

  /** Deletes the selection and reloads the page so the counts and cards agree again. */
  private void deleteMeasurements(MeasurementDatasetActions actions,
      MeasurementSelection selection) {
    var pending = notifications.pendingTaskToast("task.in-progress",
        new Object[]{"Measurement deletion"}, getLocale());
    pending.open();
    actions.delete(selection, deleted -> uiHandle.onUiAndPush(() -> {
      pending.close();
      notifications.toast("measurement.deletion.successful",
          new Object[]{deleted}, getLocale()).open();
      // The experiment's totals and the per-dataset cards are now stale, so the page is rebuilt
      // rather than only the grid refreshed.
      render(context.projectId().orElseThrow().value(),
          context.experimentId().orElseThrow().value());
    }), () -> uiHandle.onUiAndPush(() -> {
      pending.close();
      notifications.toast("task.failed", new Object[]{"Measurement deletion"}, getLocale()).open();
    }));
  }

  /**
   * Exports the data locations for the requested scope.
   *
   * <p>The application does not serve the data, so this exports <em>where</em> the files are — one
   * URL per measurement — and the reader fetches them with a personal access token. Constructing the
   * URL requires the configured download endpoint; that coupling is a known debt and the subject of
   * a follow-up increment.</p>
   *
   * <p>The whole-dataset scope is the baseline and needs no selection, because a reader who has
   * selected nothing still needs a way to get all of the data.</p>
   *
   * @param guide the dialog that asked, so it can refresh its own scope state afterwards
   * @param scope which measurements the export covers
   */
  private void exportLocations(MeasurementDataDownloadGuide guide,
      MeasurementDataDownloadGuide.ExportScope scope) {
    java.util.List<String> codes =
        scope == MeasurementDataDownloadGuide.ExportScope.SELECTED
            ? detailsComponent.selectionCodes()
            : detailsComponent.allMeasurementCodes();
    if (codes.isEmpty()) {
      notifications.toast("rawdata.no-dataset-selected", new Object[]{}, getLocale()).open();
      return;
    }
    String endpoint = downloadEndpoint;
    byte[] content = codes.stream()
        .map(code -> endpoint + "/" + code)
        .collect(java.util.stream.Collectors.joining("\n", "", "\n"))
        .getBytes(java.nio.charset.StandardCharsets.UTF_8);
    guide.triggerDownload(locationListProvider("measurement_dataset_locations", "txt", content));
    // The scopes are recomputed from the selection each time an export runs, so a stale count
    // cannot outlive the selection it described.
    guide.setSelectionCount(detailsComponent.selectionCount());
  }

  /** Downloads a produced workbook. */
  private void triggerDownload(String prefix, String suffix,
      life.qbic.projectmanagement.application.api.fair.DigitalObject digitalObject) {
    var provider = new life.qbic.datamanager.files.export.download.DownloadStreamProvider() {
      @Override
      public String getFilename() {
        return life.qbic.application.commons.FileNameFormatter.formatWithTimestampedSimple(
            java.time.LocalDate.now(), projectCode(), prefix, suffix);
      }

      @Override
      public java.io.InputStream getStream() {
        return digitalObject.content();
      }

      @Override
      public String getContentType() {
        return org.springframework.util.MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;
      }

      @Override
      public java.util.Optional<Long> contentLength() {
        return java.util.Optional.empty();
      }
    };
    downloadComponent.trigger(provider);
  }

  /**
   * Builds a plain-text download provider for the location list.
   *
   * <p>Returned rather than triggered: the download is performed by the anchor inside the dialog,
   * which is the only place a click is honoured while a strict modal dialog is open.</p>
   */
  private life.qbic.datamanager.files.export.download.DownloadStreamProvider locationListProvider(
      String prefix, String suffix, byte[] content) {
    return new life.qbic.datamanager.files.export.download.DownloadStreamProvider() {
      @Override
      public String getFilename() {
        return life.qbic.application.commons.FileNameFormatter.formatWithTimestampedSimple(
            java.time.LocalDate.now(), projectCode(), prefix, suffix);
      }

      @Override
      public java.io.InputStream getStream() {
        return new java.io.ByteArrayInputStream(content);
      }

      @Override
      public String getContentType() {
        return org.springframework.util.MimeTypeUtils.TEXT_PLAIN_VALUE;
      }

      @Override
      public java.util.Optional<Long> contentLength() {
        return java.util.Optional.of((long) content.length);
      }
    };
  }

  /** The project code used in download file names. */
  /**
   * The project code used in download file names.
   *
   * <p>Never the project's internal identifier: a UUID in an exported file name is meaningless to
   * the reader and looks like a defect, so an unresolved code degrades to the word "project"
   * rather than to the id.</p>
   */
  private String projectCode() {
    if (context == null) {
      return "project";
    }
    return context.projectCode().orElse("project");
  }

  /**
   * Opens the edit dialog for the given selection.
   *
   * <p>Editing is Excel-based, exactly as it was on the retired measurement page: the sheet is
   * exported pre-filled, the user changes the editable fields, and re-uploading applies the
   * changes. Reusing that workflow keeps one mental model for bulk metadata edits across samples
   * and measurements.</p>
   */
  private void openEditDialog(MeasurementSelection selection) {
    var upload = new life.qbic.datamanager.views.projects.project.measurements.registration.MeasurementUpload(
        asyncService, context,
        life.qbic.datamanager.files.parsing.converters.ConverterRegistry.converterFor(
            life.qbic.projectmanagement.application.api.AsyncProjectService
                .MeasurementUpdateInformationNGS.class),
        notifications, uploadConfiguration);
    var template = new life.qbic.datamanager.views.projects.project.measurements.MeasurementTemplateComponent(
        "Export the measurements you want to edit, change the properties in the sheet and upload "
            + "it below to save the changes.",
        exportButtonLabel(selection), editTemplateRequest(selection), notifications,
        this::projectCode);
    var component = new life.qbic.datamanager.views.projects.project.measurements.MeasurementUpdateComponent(
        template, upload);

    var dialog = life.qbic.datamanager.views.general.dialog.AppDialog.large();
    life.qbic.datamanager.views.general.dialog.DialogHeader.with(dialog, "Edit measurements");
    life.qbic.datamanager.views.general.dialog.DialogFooter.with(dialog, "Cancel", "Apply changes");
    life.qbic.datamanager.views.general.dialog.DialogBody.with(dialog, component, component);
    dialog.registerCancelAction(dialog::close);
    dialog.registerConfirmAction(() -> {
      if (upload.validate().hasPassed()) {
        submitUpdate(upload.getValidationRequestContent());
        dialog.close();
      }
    });
    add(dialog);
    dialog.open();
  }

  /** The label of the edit dialog's export button, stating what it will export. */
  private static String exportButtonLabel(MeasurementSelection selection) {
    return selection.isEmpty()
        ? "Export all measurements in this dataset"
        : "Export %d selected".formatted(selection.total());
  }

  /**
   * The export request behind the edit dialog's button.
   *
   * <p>With no selection, the whole active dataset is exported; with one, exactly that selection.
   * Both go through the same service call the retired page used, so the sheet layout is unchanged.
   * </p>
   */
  private reactor.core.publisher.Mono<
      life.qbic.projectmanagement.application.api.fair.DigitalObject> editTemplateRequest(
      MeasurementSelection selection) {
    return reactor.core.publisher.Mono.defer(() -> {
      String projectId = context.projectId().orElseThrow().value();
      if (!selection.isEmpty()) {
        return switch (firstMethodOf(selection)) {
          case GENOMICS -> asyncService.measurementUpdateNGS(projectId, selection.genomics(),
              OPEN_XML);
          case PROTEOMICS -> asyncService.measurementUpdatePxP(projectId, selection.proteomics(),
              OPEN_XML);
          case IMMUNOPEPTIDOMICS -> asyncService.measurementUpdateIP(projectId,
              selection.immunopeptidomics(), OPEN_XML);
        };
      }
      String experimentId = context.experimentId().orElseThrow().value();
      MeasurementDatasetSegment segment = datasetSelector.selected();
      java.util.List<String> allIds = detailsComponent.allMeasurementIds();
      return switch (segment) {
        case GENOMICS -> asyncService.measurementUpdateNGS(projectId, allIds, OPEN_XML);
        case PROTEOMICS -> asyncService.measurementUpdatePxP(projectId, allIds, OPEN_XML);
        case IMMUNOPEPTIDOMICS -> asyncService.measurementUpdateIP(projectId, allIds, OPEN_XML);
      };
    });
  }

  /** The measurement method of a non-empty selection. */
  private static MeasurementDatasetSegment firstMethodOf(MeasurementSelection selection) {
    if (!selection.genomics().isEmpty()) {
      return MeasurementDatasetSegment.GENOMICS;
    }
    if (!selection.proteomics().isEmpty()) {
      return MeasurementDatasetSegment.PROTEOMICS;
    }
    return MeasurementDatasetSegment.IMMUNOPEPTIDOMICS;
  }

  private static final org.springframework.util.MimeType OPEN_XML =
      org.springframework.util.MimeType.valueOf(
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

  /** Submits a validated update batch. */
  private void submitUpdate(java.util.List<? extends
      life.qbic.projectmanagement.application.api.AsyncProjectService.ValidationRequestBody> bodies) {
    String projectId = context.projectId().orElseThrow().value();
    java.util.List<life.qbic.projectmanagement.application.api.AsyncProjectService
        .MeasurementUpdateRequestBody> merged = new java.util.ArrayList<>();
    merged.addAll(mergeByPool(bodies,
        life.qbic.projectmanagement.application.api.AsyncProjectService
            .MeasurementUpdateInformationNGS.class));
    merged.addAll(mergeByPool(bodies,
        life.qbic.projectmanagement.application.api.AsyncProjectService
            .MeasurementUpdateInformationPxP.class));
    merged.addAll(mergeByPool(bodies,
        life.qbic.projectmanagement.application.api.AsyncProjectService
            .MeasurementUpdateInformationIP.class));
    if (merged.isEmpty()) {
      return;
    }
    var requests = merged.stream()
        .map(body -> new life.qbic.projectmanagement.application.api.AsyncProjectService
            .MeasurementUpdateRequest(projectId, body))
        .toList();
    runWrite("Measurement update",
        asyncService.update(reactor.core.publisher.Flux.fromIterable(requests)).then());
  }

  /**
   * Partitions the request bodies by type and merges each partition with its pooling processor.
   *
   * <p>Pooled measurements arrive as several rows that describe one measurement, so they must be
   * merged before submission; each method has its own processor in the registry.</p>
   */
  private static <T> java.util.List<T> mergeByPool(
      java.util.List<? extends life.qbic.projectmanagement.application.api.AsyncProjectService
          .ValidationRequestBody> bodies, Class<T> type) {
    java.util.List<T> matching = bodies.stream()
        .filter(type::isInstance)
        .map(type::cast)
        .toList();
    if (matching.isEmpty()) {
      return java.util.List.of();
    }
    var processor = life.qbic.datamanager.views.projects.project.measurements.processor
        .ProcessorRegistry.processorFor(type);
    if (processor == null) {
      throw new IllegalStateException("No processor registered for " + type.getSimpleName());
    }
    return processor.process(matching);
  }

  /**
   * Runs a write and reports one outcome for the whole batch.
   *
   * <p>A batch can fail per row, and a silent partial success is the worst outcome for a bulk
   * metadata edit: the user believes the sheet was applied. The page therefore reports what
   * succeeded and what did not, then reloads so the list reflects the true state.</p>
   */
  private void runWrite(String description, reactor.core.publisher.Mono<Void> operation) {
    var pending = notifications.pendingTaskToast("task.in-progress",
        new Object[]{description}, getLocale());
    pending.open();
    operation.subscribe(ignored -> {
      // no per-element handling; the batch outcome is reported once, below
    }, error -> {
      log.error(description + " failed", error);
      uiHandle.onUiAndPush(() -> {
        pending.close();
        notifications.toast("task.failed", new Object[]{description}, getLocale()).open();
      });
    }, () -> uiHandle.onUiAndPush(() -> {
      pending.close();
      notifications.toast("measurement.update.successful", new Object[]{""}, getLocale()).open();
      // Rebuilding touches the component tree, so it must run on the UI thread too.
      reload();
    }));
  }

  /**
   * Reloads the page after a write, so counts, cards and list agree with the database.
   *
   * <p>Rebuilt rather than refreshed in place: a registration or deletion changes the experiment's
   * totals and can add or remove a whole dataset card, so refreshing only the grid would leave the
   * header and the cards stating the previous state.</p>
   */
  private void reload() {
    if (context == null) {
      return;
    }
    render(context.projectId().orElseThrow().value(),
        context.experimentId().orElseThrow().value());
  }

  /**
   * Opens the measurement registration dialog.
   *
   * <p>Registration stays a page-level action: it creates measurements rather than acting on
   * existing ones, and the template it provides is per measurement method, so it cannot be scoped
   * to the currently selected dataset card without forcing a second registration for a sheet that
   * contains two methods.</p>
   */
  private void openRegistrationDialog() {
    if (!canWrite()) {
      notifications.toast("access.denied", new Object[]{}, getLocale()).open();
      return;
    }
    var upload = new life.qbic.datamanager.views.projects.project.measurements.registration.MeasurementUpload(
        asyncService, context,
        life.qbic.datamanager.files.parsing.converters.ConverterRegistry.converterFor(
            life.qbic.projectmanagement.application.api.AsyncProjectService
                .MeasurementRegistrationInformationNGS.class),
        notifications, uploadConfiguration);
    var templateSelection =
        new life.qbic.datamanager.views.projects.project.measurements.MeasurementTemplateSelectionComponent(
            registrationTemplates());
    var component =
        new life.qbic.datamanager.views.projects.project.measurements.MeasurementRegistrationComponent(
            templateSelection, upload,
            life.qbic.datamanager.views.projects.project.measurements
                .MeasurementTemplateSelectionComponent.Domain.Genomics);

    var dialog = life.qbic.datamanager.views.general.dialog.AppDialog.medium();
    life.qbic.datamanager.views.general.dialog.DialogHeader.with(dialog, "Register measurements");
    life.qbic.datamanager.views.general.dialog.DialogFooter.with(dialog, "Cancel", "Register");
    life.qbic.datamanager.views.general.dialog.DialogBody.with(dialog, component, component);
    dialog.registerCancelAction(dialog::close);
    dialog.registerConfirmAction(() -> {
      if (upload.validate().hasPassed()) {
        submitRegistration(upload.getValidationRequestContent());
        dialog.close();
      }
    });
    add(dialog);
    dialog.open();
  }

  /** The downloadable templates, one per measurement method. */
  private java.util.Map<life.qbic.datamanager.views.projects.project.measurements
      .MeasurementTemplateSelectionComponent.Domain,
      life.qbic.datamanager.files.export.download.DownloadStreamProvider> registrationTemplates() {
    return java.util.Map.of(
        life.qbic.datamanager.views.projects.project.measurements
            .MeasurementTemplateSelectionComponent.Domain.Genomics,
        workbookTemplate("ngs_measurement_registration_sheet",
            life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory
                .NGSWorkbooks::createRegistrationWorkbook),
        life.qbic.datamanager.views.projects.project.measurements
            .MeasurementTemplateSelectionComponent.Domain.Proteomics,
        workbookTemplate("pxp_measurement_registration_sheet",
            life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory
                .ProteomicsWorkbooks::createRegistrationWorkbook),
        life.qbic.datamanager.views.projects.project.measurements
            .MeasurementTemplateSelectionComponent.Domain.Immunopeptidomics,
        workbookTemplate("immunopeptidomics_measurement_registration_sheet",
            life.qbic.projectmanagement.infrastructure.template.provider.openxml.factory
                .IPWorkbooks::createRegistrationWorkbook));
  }

  private static life.qbic.datamanager.files.export.download.DownloadStreamProvider workbookTemplate(
      String filename,
      java.util.function.Supplier<org.apache.poi.ss.usermodel.Workbook> supplier) {
    return new life.qbic.datamanager.files.export.download.WorkbookDownloadStreamProvider() {
      @Override
      public String getFilename() {
        return life.qbic.application.commons.FileNameFormatter.formatWithVersion(filename, 1,
            "xlsx");
      }

      @Override
      public java.util.Optional<Long> contentLength() {
        return java.util.Optional.empty();
      }

      @Override
      public org.apache.poi.ss.usermodel.Workbook getWorkbook() {
        return supplier.get();
      }
    };
  }

  /**
   * Submits a validated registration batch.
   *
   * <p>A sheet can contain several measurement methods at once, so the batch is partitioned by
   * request type, each partition merged by its pooling processor, and all of them sent as one
   * stream — the same shape the retired measurement page used.</p>
   */
  private void submitRegistration(java.util.List<? extends
      life.qbic.projectmanagement.application.api.AsyncProjectService.ValidationRequestBody> bodies) {
    String projectId = context.projectId().orElseThrow().value();
    java.util.List<life.qbic.projectmanagement.application.api.AsyncProjectService
        .MeasurementRegistrationRequestBody> merged = new java.util.ArrayList<>();
    merged.addAll(mergeByPool(bodies,
        life.qbic.projectmanagement.application.api.AsyncProjectService
            .MeasurementRegistrationInformationNGS.class));
    merged.addAll(mergeByPool(bodies,
        life.qbic.projectmanagement.application.api.AsyncProjectService
            .MeasurementRegistrationInformationPxP.class));
    merged.addAll(mergeByPool(bodies,
        life.qbic.projectmanagement.application.api.AsyncProjectService
            .MeasurementRegistrationInformationIP.class));
    if (merged.isEmpty()) {
      return;
    }
    var requests = merged.stream()
        .map(body -> new life.qbic.projectmanagement.application.api.AsyncProjectService
            .MeasurementRegistrationRequest(projectId, body))
        .toList();
    runWrite("Measurement registration",
        asyncService.create(reactor.core.publisher.Flux.fromIterable(requests)).then());
  }

  private Div emptyState() {
    Div empty = new Div();
    empty.addClassName("measurement-dataset-empty-state");

    Span heading = new Span("No measurement data yet");
    heading.addClassName("measurement-dataset-empty-heading");

    Span body = new Span(
        "A measurement is the metadata describing how a result was produced, together with "
            + "the data produced by it. Register measurement metadata first: registration assigns "
            + "the measurement code that the data upload is keyed by.");
    body.addClassName("measurement-dataset-empty-body");

    Div actions = new Div();
    actions.addClassName("measurement-dataset-empty-actions");

    boolean hasSamples = hasSamples();
    if (!hasSamples) {
      // The hard dependency chain is experiment design -> samples -> measurements. Saying so is
      // more useful than offering a registration the user cannot complete.
      Span prerequisite = new Span(
          "This experiment has no samples yet. Samples are the subject of a measurement, so they "
              + "have to be registered first.");
      prerequisite.addClassName("measurement-dataset-empty-prerequisite");
      actions.add(prerequisite, linkButton("Go to samples", VaadinIcon.ARROW_RIGHT,
          SampleInformationMain.class));
    } else {
      actions.add(registerInlineButton());
    }

    empty.add(heading, body, actions);
    return empty;
  }

  private boolean hasSamples() {
    if (context == null) {
      return false;
    }
    String projectId = context.projectId().orElseThrow().value();
    String experimentId = context.experimentId().orElseThrow().value();
    return queryService.getExperimentProgress(projectId, experimentId)
        .map(progress -> progress.sampleCount() > 0)
        .blockOptional()
        .orElse(false);
  }

  /**
   * The registration action for the empty state.
   *
   * <p>It opens the dialog in place rather than linking to the retired measurement page, which no
   * longer has a route: a dead link in the only state that offers a way forward is the worst place
   * to have one.</p>
   */
  private com.vaadin.flow.component.button.Button registerInlineButton() {
    var button = new com.vaadin.flow.component.button.Button("Register measurements",
        VaadinIcon.PLUS.create());
    button.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
    button.addClickListener(clicked -> openRegistrationDialog());
    button.setVisible(canWrite());
    return button;
  }

  private com.vaadin.flow.component.button.Button linkButton(String label,
      VaadinIcon icon, Class<? extends com.vaadin.flow.component.Component> target) {
    var button = new com.vaadin.flow.component.button.Button(label, icon.create());
    button.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
    button.addClickListener(event -> getUI().ifPresent(ui -> ui.navigate(target,
        experimentRouteParameters())));
    return button;
  }

  private RouteParameters experimentRouteParameters() {
    return new RouteParameters(
        new RouteParam(PROJECT_ID_ROUTE_PARAMETER, context.projectId().orElseThrow().value()),
        new RouteParam(EXPERIMENT_ID_ROUTE_PARAMETER,
            context.experimentId().orElseThrow().value()));
  }

  private void removeDetailsComponent() {
    if (detailsComponent != null) {
      remove(detailsComponent);
      detailsComponent = null;
    }
    removeAllEmptyStates();
  }

  private void removeAllEmptyStates() {
    content.getChildren()
        .filter(component -> component.getClass() == Div.class)
        .filter(component -> component.getElement().getClassList()
            .contains("measurement-dataset-empty-state"))
        .toList()
        .forEach(content::remove);
  }

  /**
   * Whether the selector currently offers the given dataset.
   *
   * <p>A URL naming a method the experiment never measured must not switch the page to an empty
   * list: it is a stale or hand-crafted link, and the page falls back to a dataset that exists.</p>
   */
  private boolean datasetSelectorContains(MeasurementDatasetSegment segment) {
    return datasetSelector.getRenderedSegments().contains(segment);
  }

  private void writeUrl(ListState state) {
    if (basePath == null || suppressUrlWrite) {
      return;
    }
    UI ui = UI.getCurrent();
    if (ui == null) {
      return;
    }
    Map<String, List<String>> parameters = new LinkedHashMap<>();
    MeasurementDatasetSegment selected = datasetSelector.selected();
    if (selected != null) {
      // First, so the URL reads dataset-then-list-state rather than burying the structure.
      parameters.put(DATASET_PARAMETER, List.of(selected.urlValue()));
    }
    parameters.putAll(ListStateCodec.toQueryParameters(state).getParameters());
    var location = new com.vaadin.flow.router.Location(basePath,
        new com.vaadin.flow.router.QueryParameters(parameters));
    ui.getPage().getHistory().pushState(null, location);
  }

  @Override
  public void beforeLeave(BeforeLeaveEvent event) {
    getUI().ifPresent(ui -> ui.getPage().getHistory()
        .setHistoryStateChangeHandler(routerHistoryStateChangeHandler));
  }

  private void onHistoryStateChange(HistoryStateChangeEvent event) {
    String expectedPath = currentPath();
    if (expectedPath == null || !expectedPath.equals(event.getLocation().getPath())) {
      if (routerHistoryStateChangeHandler != null) {
        routerHistoryStateChangeHandler.onHistoryStateChange(event);
      }
      return;
    }
    if (detailsComponent == null) {
      return;
    }
    suppressUrlWrite = true;
    try {
      ListState urlState = ListStateCodec.parse(event.getLocation().getQueryParameters(),
          MeasurementDataSort.DEFAULT, MeasurementDataSort.allowedSortOrders());
      detailsComponent.applyExternalState(urlState);
    } finally {
      suppressUrlWrite = false;
    }
  }

  private String currentPath() {
    if (context == null || context.projectId().isEmpty() || context.experimentId().isEmpty()) {
      return null;
    }
    return String.format(ProjectRoutes.MEASUREMENT_DATASET,
        context.projectId().orElseThrow().value(),
        context.experimentId().orElseThrow().value());
  }
}
