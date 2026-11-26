package life.qbic.datamanager.views.projects.project.datasets;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import life.qbic.datamanager.security.UserPermissions;
import life.qbic.datamanager.views.Context;
import life.qbic.datamanager.views.UiHandle;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.dialog.AlertDialog;
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory;
import life.qbic.datamanager.views.notifications.Toast;
import life.qbic.datamanager.views.projects.project.ProjectMainLayout;
import life.qbic.datamanager.views.general.section.Section;
import life.qbic.datamanager.views.general.section.SectionHeader;
import life.qbic.datamanager.views.general.section.SectionTitle;
import life.qbic.identity.api.AuthenticationToUserIdTranslator;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.api.ProjectQueryService;
import life.qbic.projectmanagement.application.associated_dataset.AssociatedDatasetService;
import life.qbic.projectmanagement.application.associated_dataset.ConnectedDatasetView;
import life.qbic.projectmanagement.application.associated_dataset.ExternalCredentialService;
import life.qbic.projectmanagement.application.associated_dataset.RemoveDatasetError;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * <b>Datasets view</b>
 *
 * <p>The project's datasets, both halves of them. The page is the umbrella: a project has
 * <em>primary</em> datasets -- the measurement data of its experiments, one per experiment and
 * assay type -- and <em>connected</em> datasets, which are hosted elsewhere and linked here and
 * can be anything from an analysis result to a data management plan, a poster or a project
 * report. "Datasets" without a qualifier means both, which is why neither half is named here
 * without one.</p>
 *
 * <p>Structure (top to bottom): the page band naming the page; the primary datasets
 * ({@link PrimaryDatasetsComponent}, read from the project's experiment progress); then the
 * connected datasets ({@link ConnectedResourcesComponent}), which keeps its own action bar for
 * connecting, syncing and removing.</p>
 *
 * <p>The route keeps its {@code .../datasets} path and its shell: the page used to show only the
 * connected half.</p>
 *
 * <p>The connection feature itself (FEAT-DATSET-01) also provides the sliding connect-datasets
 * sidebar that searches InvenioRDM instances, lets the user pick results to connect, and
 * optionally associates them with a project experiment.</p>
 *
 * @since 1.12.0
 */
@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/datasets", layout = ProjectMainLayout.class)
@PermitAll
public class ProjectDatasetsMain extends Main implements BeforeEnterObserver {

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";

  private final transient AssociatedDatasetService associatedDatasetService;
  private final transient ExperimentInformationService experimentInformationService;
  private final transient life.qbic.projectmanagement.application.ProjectInformationService
      projectInformationService;
  private final transient UserPermissions userPermissions;
  private final transient MessageSourceNotificationFactory notificationFactory;
  private final transient ExternalCredentialService externalCredentialService;

  private final PrimaryDatasetsComponent primaryDatasetsComponent;
  private final ConnectedResourcesComponent connectedResourcesComponent;
  private final AuthenticationToUserIdTranslator authenticationToUserIdTranslator;
  private ConnectDatasetSidebar connectDatasetSidebar;
  private SyncResultsSidebar syncResultsSidebar;

  private Context context = new Context();
  private final UiHandle uiHandle = new UiHandle();

  /**
   * The band this navigation asked for, or {@code null} for the top of the page.
   *
   * <p>Set in {@link #beforeEnter} from the URL fragment and consumed once the content that carries
   * the anchor has actually been rendered. It cannot be done in {@code beforeEnter}: both bands are
   * populated asynchronously (the primary half through {@link ProjectQueryService}), so at
   * navigation time the element with that id does not exist yet and the browser has nothing to
   * scroll to. Doing it here and honouring it from the render callbacks is what turns the link into
   * a landing rather than a page load.</p>
   */
  private String pendingAnchor;

  /** The anchors this page knows how to land on, in document order. */
  private static final List<String> BAND_ANCHORS = List.of(
      PrimaryDatasetsComponent.BAND_ANCHOR, PrimaryDatasetsComponent.CONNECTED_BAND_ANCHOR);

  @Autowired
  public ProjectDatasetsMain(
      AssociatedDatasetService associatedDatasetService,
      ExperimentInformationService experimentInformationService,
      life.qbic.projectmanagement.application.ProjectInformationService projectInformationService,
      UserPermissions userPermissions,
      MessageSourceNotificationFactory notificationFactory,
      AuthenticationToUserIdTranslator authenticationToUserIdTranslator,
      ExternalCredentialService externalCredentialService,
      ProjectQueryService projectQueryService,
      @org.springframework.beans.factory.annotation.Value(
          "${server.download.api.measurement.url:}") String downloadEndpoint
      ) {
    this.associatedDatasetService = requireNonNull(associatedDatasetService,
        "associatedDatasetService must not be null");
    this.projectInformationService = requireNonNull(projectInformationService,
        "projectInformationService must not be null");
    this.experimentInformationService = requireNonNull(experimentInformationService,
        "experimentInformationService must not be null");
    this.userPermissions = requireNonNull(userPermissions,
        "userPermissions must not be null");
    this.notificationFactory = requireNonNull(notificationFactory,
        "notificationFactory must not be null");
    this.authenticationToUserIdTranslator = requireNonNull(authenticationToUserIdTranslator);
    this.externalCredentialService = requireNonNull(externalCredentialService,
        "externalCredentialService must not be null");

    addClassName("project");
    addClassName("datasets");
    primaryDatasetsComponent = new PrimaryDatasetsComponent(
        requireNonNull(projectQueryService, "projectQueryService must not be null"),
        downloadEndpoint, this.notificationFactory);
    connectedResourcesComponent = new ConnectedResourcesComponent(associatedDatasetService);
    // The primary half renders asynchronously, and a fragment can only be honoured against an
    // element that exists. Its load completion is therefore also a landing opportunity.
    primaryDatasetsComponent.addDatasetsRenderedListener(
        e -> landOn(PrimaryDatasetsComponent.BAND_ANCHOR));
    connectedResourcesComponent.addConnectDatasetsClickListener(e -> openConnectSidebar());
    connectedResourcesComponent.addRemoveDatasetClickListener(
        e -> onRemoveDataset(e.getDatasetId()));
    connectedResourcesComponent.addSyncDatasetClickListener(
        e -> onSyncDatasets(List.of(e.getDatasetId())));
    connectedResourcesComponent.addSyncAllDatasetsClickListener(e -> onSyncAllDatasets());
    add(pageBand(), primaryDatasetsComponent, connectedResourcesComponent);
  }

  /**
   * The page band. It names the page; each half titles itself below, so the page carries no
   * subtitle that would repeat them.
   */
  private static Section pageBand() {
    var header = new SectionHeader(new SectionTitle("Datasets", SectionTitle.Size.LARGE));
    Section band = new Section.SectionBuilder().withHeader(header).build();
    band.addClassName("project-datasets-page-head");
    return band;
  }

  @Override
  protected void onAttach(AttachEvent attachEvent) {
    super.onAttach(attachEvent);
    uiHandle.bind(attachEvent.getUI());
  }

  @Override
  protected void onDetach(DetachEvent detachEvent) {
    uiHandle.unbind();
    super.onDetach(detachEvent);
  }

  // ── Helpers ───────────────────────────────────────────────────────────

  private static final Logger log = LoggerFactory.logger(ProjectDatasetsMain.class);

  /** Resolves the current Spring Security user to a DM user ID. */
  private String resolveCurrentUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    return authenticationToUserIdTranslator.translateToUserId(auth).orElseThrow(() -> {
      log.error("Could not translate authentication to user ID");
      return new IllegalStateException("Could not translate authentication to user ID");
    });
  }

  @Override
  public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
    String projectId = beforeEnterEvent.getRouteParameters().get(PROJECT_ID_ROUTE_PARAMETER)
        .orElseThrow();
    var parsedProjectId =
        life.qbic.projectmanagement.domain.model.project.ProjectId.parse(projectId);
    if (!userPermissions.readProject(parsedProjectId)) {
      beforeEnterEvent.rerouteToError(NotFoundException.class);
      return;
    }
    this.context = new Context().with(parsedProjectId);
    // The human-facing project code names the exported file, so the dataset download does not fall
    // back to a generic name. Resolved here because the route has already resolved the project; the
    // component degrades to its own fallback if the code cannot be read.
    primaryDatasetsComponent.setProjectCode(projectInformationService.find(parsedProjectId)
        .map(project -> project.getProjectCode().value())
        .orElse(null));
    this.pendingAnchor = requestedAnchor(beforeEnterEvent.getLocation());
    setContext(parsedProjectId);
  }

  /**
   * The band the URL asked to land on, or {@code null}.
   *
   * <p>Read from the fragment Vaadin already parsed off the location. Only the anchors this page
   * defines are accepted: an arbitrary fragment is not turned into a selector.</p>
   */
  private static String requestedAnchor(Location location) {
    if (location == null) {
      return null;
    }
    String withFragment = location.getPathWithQueryParameters();
    int index = withFragment.indexOf('#');
    if (index < 0 || index == withFragment.length() - 1) {
      return null;
    }
    String anchor = withFragment.substring(index + 1);
    return BAND_ANCHORS.contains(anchor) ? anchor : null;
  }

  /**
   * Scrolls the requested band into view, once, and only if one was requested.
   *
   * <p>Called from the point that renders that band, and only for it: the primary half is loaded
   * asynchronously, so landing on the linked half must not wait for it (and vice versa). The request
   * is cleared on the way out, so a band that renders again -- after a sync, after a connection --
   * does not yank the viewport a second time.</p>
   *
   * <p>The scroll is deferred by a frame because the element was just added: at the moment the
   * callback runs the browser has not laid it out, and {@code scrollIntoView} on an unlaid-out
   * element is a no-op. {@code block: 'start'} puts the band's heading at the top of the viewport,
   * which is what "land in this section" means.</p>
   */
  private void landOn(String bandAnchor) {
    if (!bandAnchor.equals(pendingAnchor)) {
      return;
    }
    pendingAnchor = null;
    getUI().ifPresent(ui -> ui.getPage().executeJs(
        "const band = document.getElementById($0);"
            + "if (band) { requestAnimationFrame(() => band.scrollIntoView({block: 'start'})); }",
        bandAnchor));
  }

  private void setContext(
      life.qbic.projectmanagement.domain.model.project.ProjectId parsedProjectId) {
    primaryDatasetsComponent.setProjectId(parsedProjectId.value());
    connectedResourcesComponent.setWriteAllowed(userPermissions.editProject(parsedProjectId));
    connectedResourcesComponent.setContext(context);
    if (connectDatasetSidebar != null) {
      connectDatasetSidebar.setContext(context);
    }
    // The connected half is rendered synchronously by setContext above, so its band is complete
    // here; the primary half reports its own completion through its rendered event.
    landOn(PrimaryDatasetsComponent.CONNECTED_BAND_ANCHOR);
  }

  // ── Remove dataset flow ─────────────────────────────────────────────

  /**
   * Drives the Remove flow: opens a {@link AlertDialog} confirmation;
   * on confirm, calls {@code associatedDatasetService.removeDataset(…)},
   * shows a toast, and refreshes the card list.
   */
  private void onRemoveDataset(String datasetId) {
    // Resolve dataset view from current list so we can reference title
    // in the confirmation body and in subsequent toasts.
    List<ConnectedDatasetView> currentDatasets = List.of();
    if (context.projectId().isPresent()) {
      currentDatasets = associatedDatasetService.listConnectedDatasetViews(
          context.projectId().orElseThrow());
    }
    ConnectedDatasetView view = currentDatasets.stream()
        .filter(ds -> ds.id().equals(datasetId))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "Remove click event referenced unknown dataset %s".formatted(datasetId)));

    AlertDialog.danger(this,
        "Remove dataset connection?",
        "This will disconnect the dataset '" + view.title() + "' ("
            + view.pid() + ") from the project. The connection can be "
            + "re-established at any time.",
        "Disconnect dataset",
        "Keep connection",
        () -> performRemove(datasetId))
        .open();
  }

  /**
   * Kicks off the removal via
   * {@link AssociatedDatasetService#removeDatasetAsync(String, String)}
   * (which wraps the blocking service call on a bounded-elastic worker
   * thread). A pending toast is shown immediately; it is closed in an
   * {@code doFinally} terminal handler so it is always replaced by a
   * success or error toast regardless of failure mode.
   *
   * <p>The {@link AlertDialog#danger(Component, String, String, String, String, life.qbic.datamanager.views.general.dialog.DialogAction)}
   * factory already closes the dialog inside the confirm-action wrapper,
   * so the user's visible feedback is the pending toast, followed by the
   * result toast.</p>
   */
  private void performRemove(String datasetId) {
    var userId = resolveCurrentUserId();

    // Stays open until closed by the terminal doFinally handler below.
    Toast pendingToast = notificationFactory.pendingTaskToast(
        "dataset.removing.in-progress", EMPTY_PARAMETERS, getLocale());
    pendingToast.open();

    associatedDatasetService.removeDatasetAsync(datasetId, userId)
        // Always close the pending indicator on the UI thread.
        .doFinally(signal -> uiHandle.onUiAndPush(pendingToast::close))
        .subscribe(
            // Success path — the application-layer onErrorResume
            // converts every throwable into a REMOVAL_FAILED Result,
            // so a real error signal here is unexpected.
            result -> uiHandle.onUiAndPush(() -> {
              if (result.isError()) {
                handleRemoveError(result.getError());
              } else {
                notificationFactory.toast("dataset.removed.success",
                    EMPTY_PARAMETERS, getLocale()).open();
                connectedResourcesComponent.refresh();
              }
            }),
            // onError — defensive: the service layer converts any
            // throwable into a REMOVAL_FAILED Result, so this should
            // never fire. Kept as a safety net and a log entry.
            error -> uiHandle.onUiAndPush(() -> {
              log.error("Unexpected error on remove subscription for dataset %s: %s"
                  .formatted(datasetId, error.getMessage()), error);
              notificationFactory.toast("dataset.removed.failure",
                  EMPTY_PARAMETERS, getLocale()).open();
            })
        );
  }

  private static final Object[] EMPTY_PARAMETERS = new Object[]{ };

  /** Maps each {@link RemoveDatasetError} to its user-visible feedback.
   *  Non-critical errors ({@code DATASET_NOT_FOUND},
   *  {@code DATASET_ALREADY_REMOVED}) silently refresh the list so the
   *  card vanishes if the dataset was already gone. */
  private void handleRemoveError(RemoveDatasetError error) {
    switch (error) {
      case DATASET_ALREADY_REMOVED:
        // Already gone — nothing else to tell the user.
        connectedResourcesComponent.refresh();
        break;
      case DATASET_NOT_FOUND:
        // Already gone — silently refresh.
        connectedResourcesComponent.refresh();
        break;
      case REMOVAL_FAILED:
      default:
        notificationFactory.toast("dataset.removed.failure",
            EMPTY_PARAMETERS, getLocale()).open();
        break;
    }
  }

  private void openConnectSidebar() {
    if (connectDatasetSidebar == null) {
      connectDatasetSidebar = new ConnectDatasetSidebar(
          associatedDatasetService,
          experimentInformationService,
          notificationFactory,
          authenticationToUserIdTranslator,
          externalCredentialService);
      connectDatasetSidebar.setContext(context);
      connectDatasetSidebar.addDatasetsConnectedListener(
          e -> connectedResourcesComponent.refresh());
      add(connectDatasetSidebar);
    }
    connectDatasetSidebar.open();
  }

  // ── Sync flow ────────────────────────────────────────────────────

  /**
   * Drives a sync trigger for the given dataset connections: opens the
   * {@link SyncResultsSidebar}, starts the reactive sync, and refreshes
   * the card list when the trigger completes.
   */
  private void onSyncDatasets(List<String> datasetIds) {
    if (context.projectId().isEmpty() || datasetIds.isEmpty()) {
      return;
    }
    var projectId = context.projectId().orElseThrow();
    List<ConnectedDatasetView> views = associatedDatasetService.listConnectedDatasetViews(projectId)
        .stream()
        .filter(view -> datasetIds.contains(view.id()))
        .toList();
    if (views.isEmpty()) {
      return;
    }
    var userId = resolveCurrentUserId();
    ensureSyncSidebar().startSync(projectId, views, userId);
  }

  private void onSyncAllDatasets() {
    if (context.projectId().isEmpty()) {
      return;
    }
    var projectId = context.projectId().orElseThrow();
    List<ConnectedDatasetView> views = associatedDatasetService.listConnectedDatasetViews(projectId);
    if (views.isEmpty()) {
      return;
    }
    var userId = resolveCurrentUserId();
    ensureSyncSidebar().startSync(projectId, views, userId);
  }

  private SyncResultsSidebar ensureSyncSidebar() {
    if (syncResultsSidebar == null) {
      syncResultsSidebar = new SyncResultsSidebar(associatedDatasetService);
      syncResultsSidebar.addDatasetsSyncedListener(
          e -> connectedResourcesComponent.refresh());
      add(syncResultsSidebar);
    }
    return syncResultsSidebar;
  }
}
