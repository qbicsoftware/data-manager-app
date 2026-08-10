package life.qbic.datamanager.views.projects.project.datasets;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
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
import life.qbic.identity.api.AuthenticationToUserIdTranslator;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.associated_dataset.AssociatedDatasetService;
import life.qbic.projectmanagement.application.associated_dataset.ConnectedDatasetView;
import life.qbic.projectmanagement.application.associated_dataset.ExternalCredentialService;
import life.qbic.projectmanagement.application.associated_dataset.RemoveDatasetError;
import life.qbic.projectmanagement.application.experiment.ExperimentInformationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * <b>Associated Datasets view</b>
 *
 * <p>The main view for the "Connect associated datasets with Data Manager
 * projects" feature (FEAT-DATSET-01). Provides:
 * <ul>
 *   <li>an "All datasets" section showing the datasets currently
 *       connected to the selected project, or an empty-state guide when
 *       no datasets are connected yet;</li>
 *   <li>a sliding connect-datasets sidebar that searches InvenioRDM
 *       instances, lets the user pick results to connect, and optionally
 *       associates them with a project experiment.</li>
 * </ul>
 *
 *
 * @since 1.12.0
 */
@SpringComponent
@UIScope
@Route(value = "projects/:projectId?/datasets", layout = ProjectMainLayout.class)
@PermitAll
public class ConnectedDatasetsMain extends Main implements BeforeEnterObserver {

  public static final String PROJECT_ID_ROUTE_PARAMETER = "projectId";

  private final transient AssociatedDatasetService associatedDatasetService;
  private final transient ExperimentInformationService experimentInformationService;
  private final transient UserPermissions userPermissions;
  private final transient MessageSourceNotificationFactory notificationFactory;
  private final transient ExternalCredentialService externalCredentialService;

  private final ConnectedResourcesComponent connectedResourcesComponent;
  private final AuthenticationToUserIdTranslator authenticationToUserIdTranslator;
  private ConnectDatasetSidebar connectDatasetSidebar;

  private Context context = new Context();
  private final UiHandle uiHandle = new UiHandle();

  @Autowired
  public ConnectedDatasetsMain(
      AssociatedDatasetService associatedDatasetService,
      ExperimentInformationService experimentInformationService,
      UserPermissions userPermissions,
      MessageSourceNotificationFactory notificationFactory,
      AuthenticationToUserIdTranslator authenticationToUserIdTranslator,
      ExternalCredentialService externalCredentialService
      ) {
    this.associatedDatasetService = requireNonNull(associatedDatasetService,
        "associatedDatasetService must not be null");
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
    connectedResourcesComponent = new ConnectedResourcesComponent(associatedDatasetService);
    connectedResourcesComponent.addConnectDatasetsClickListener(e -> openConnectSidebar());
    connectedResourcesComponent.addRemoveDatasetClickListener(
        e -> onRemoveDataset(e.getDatasetId()));
    add(connectedResourcesComponent);
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

  private static final Logger log = LoggerFactory.logger(ConnectedDatasetsMain.class);

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
    setContext(parsedProjectId);
  }

  private void setContext(
      life.qbic.projectmanagement.domain.model.project.ProjectId parsedProjectId) {
    connectedResourcesComponent.setWriteAllowed(userPermissions.editProject(parsedProjectId));
    connectedResourcesComponent.setContext(context);
    if (connectDatasetSidebar != null) {
      connectDatasetSidebar.setContext(context);
    }
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
}
