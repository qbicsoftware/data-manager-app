package life.qbic.datamanager.views.projects.project.datasets;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import life.qbic.datamanager.views.UiHandle;
import life.qbic.datamanager.views.account.ExternalProvidersMain;
import life.qbic.projectmanagement.application.associated_dataset.AssociatedDatasetService;
import life.qbic.projectmanagement.application.associated_dataset.ConnectedDatasetView;
import life.qbic.projectmanagement.application.associated_dataset.SyncDatasetError;
import life.qbic.projectmanagement.application.associated_dataset.SyncDatasetResponse;
import life.qbic.projectmanagement.domain.model.associated_dataset.AssociatedDatasetId;
import life.qbic.projectmanagement.domain.model.project.ProjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;

/**
 * <b>Sync Results Sidebar</b>
 *
 * <p>A right-side sliding panel that shows the outcome of a sync trigger
 * (single dataset or Sync All) with one row per dataset. Rows flip live
 * from "Syncing…" to their final state as the per-dataset responses
 * arrive:</p>
 * <ul>
 *   <li><b>Updated</b> — shows the new version (green).</li>
 *   <li><b>Up to date</b> — no changes on the source.</li>
 *   <li><b>Cannot be synced</b> — inline guidance (insufficient rights,
 *       provider not configured, access link refresh requires the record
 *       owner, record no longer exists, …).</li>
 * </ul>
 *
 * <p>Per ADR-0005 (A1) per-record access is only known after the HTTP
 * call — rows therefore resolve live; only the "provider not configured"
 * case for metadata-restricted records is short-circuited locally.</p>
 *
 * <p>Fires {@link DatasetsSyncedEvent} when the trigger completes so the
 * parent view can refresh the connected-datasets grid.</p>
 *
 * @since 1.13.0
 */
public class SyncResultsSidebar extends Div {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final Logger log = LoggerFactory.getLogger(SyncResultsSidebar.class);

  private static final String SUCCESS_COLOR = "var(--lumo-success-text-color)";
  private static final String SECONDARY_COLOR = "var(--lumo-secondary-text-color)";
  private static final String ERROR_COLOR = "var(--lumo-error-text-color)";

  private final AssociatedDatasetService associatedDatasetService;
  private final UiHandle uiHandle = new UiHandle();
  private final Div overlay;
  private final Div panel;
  private final Span summaryLabel;
  private final Div rowsContainer;
  private final Div credentialsHint;

  private final Map<String, RowState> rows = new HashMap<>();
  private final List<String> rowOrder = new ArrayList<>();

  private Registration detachRegistration;
  private Disposable activeSubscription;

  public SyncResultsSidebar(AssociatedDatasetService associatedDatasetService) {
    this.associatedDatasetService = requireNonNull(associatedDatasetService,
        "associatedDatasetService must not be null");

    addClassName("sync-results-sidebar");

    // ── Overlay (semi-transparent backdrop) ──────────────────────────
    overlay = new Div();
    overlay.addClassName("srs-backdrop");
    overlay.getStyle().set("display", "none");
    overlay.addClickListener(e -> close());
    add(overlay);

    // ── Panel ────────────────────────────────────────────────────────
    panel = new Div();
    panel.addClassName("srs-panel");
    panel.getStyle().set("display", "none");

    var body = new Div();
    body.addClassNames("srs-body", "flex-vertical");
    panel.add(body);

    // Header: title + close button
    var header = new Div();
    header.addClassNames("srs-header", "flex-horizontal", "items-center");
    var title = new Span("Synchronisation results");
    title.addClassName("heading-4");
    var closeButton = new Button(VaadinIcon.CLOSE.create());
    closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
    closeButton.getElement().setAttribute("title", "Close");
    closeButton.getElement().setAttribute("aria-label", "Close");
    closeButton.addClickListener(e -> close());
    var titleSpacer = new Div();
    titleSpacer.getStyle().set("flex-grow", "1");
    header.add(title, titleSpacer, closeButton);
    body.add(header);

    // Summary line
    summaryLabel = new Span();
    summaryLabel.addClassNames("normal-body-text", "color-secondary");
    var summaryContainer = new Div();
    summaryContainer.addClassNames("srs-summary");
    summaryContainer.add(summaryLabel);
    body.add(summaryContainer);

    // Rows
    rowsContainer = new Div();
    rowsContainer.addClassNames("srs-rows", "flex-vertical");
    body.add(rowsContainer);

    // Footer: hint + Done
    credentialsHint = new Div();
    credentialsHint.addClassNames("srs-credentials-hint", "extra-small-body-text");
    credentialsHint.getStyle().set("display", "none");

    var hintText = new Span(
        "Some datasets could not be updated because of missing or insufficient "
            + "provider connections. ");
    var manageLink = new Anchor(
        RouteConfiguration.forSessionScope()
            .getUrl(ExternalProvidersMain.class),
        "Manage provider connections");
    manageLink.setTarget(AnchorTarget.BLANK);
    manageLink.addClassName("extra-small-body-text");
    credentialsHint.add(hintText, manageLink);

    var footer = new Div();
    footer.addClassNames("srs-footer", "flex-horizontal", "items-center", "gap-03");
    var doneButton = new Button("Done", VaadinIcon.CHECK.create());
    doneButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    doneButton.addClickListener(e -> close());
    var footerSpacer = new Div();
    footerSpacer.getStyle().set("flex-grow", "1");
    footer.add(credentialsHint, footerSpacer, doneButton);
    body.add(footer);

    add(panel);
  }

  // ── Public API ──────────────────────────────────────────────────────

  /**
   * Starts a sync for the given datasets and opens the sidecar. One row
   * per dataset is rendered immediately with a "Syncing…" status and
   * resolved live as responses arrive.
   *
   * @param projectId the project the datasets belong to
   * @param datasets  the datasets to sync (defines row order and titles)
   * @param userId    the invoking user (credentials are never borrowed)
   */
  public void startSync(ProjectId projectId, List<ConnectedDatasetView> datasets, String userId) {
    requireNonNull(projectId, "projectId must not be null");
    requireNonNull(datasets, "datasets must not be null");
    requireNonNull(userId, "userId must not be null");

    if (activeSubscription != null) {
      activeSubscription.dispose();
      activeSubscription = null;
    }

    UI ui = UI.getCurrent();
    uiHandle.bind(ui);
    if (detachRegistration == null) {
      detachRegistration = ui.addDetachListener(e -> disposeOnDetach());
    }

    renderRows(datasets);
    // Reset the summary counters — the sidebar instance is reused across
    // triggers, so totals from a previous sync must not leak into the next.
    updated = 0;
    upToDate = 0;
    failed = 0;
    summaryLabel.getStyle().set("color", SECONDARY_COLOR);
    summaryLabel.setText("Syncing %d dataset%s…".formatted(
        datasets.size(), datasets.size() == 1 ? "" : "s"));
    show();

    List<AssociatedDatasetId> ids = datasets.stream()
        .map(ConnectedDatasetView::id)
        .map(AssociatedDatasetId::parse)
        .toList();

    credentialsHint.getStyle().set("display", "none");
    activeSubscription = associatedDatasetService
        .syncDatasets(projectId, ids, userId)
        .subscribe(
            response -> uiHandle.onUiAndPush(() -> resolveRow(response)),
            error -> {
              log.error("Unexpected error on sync subscription: %s".formatted(error.getMessage()),
                  error);
              uiHandle.onUiAndPush(() -> resolveStreamError());
            },
            () -> uiHandle.onUiAndPush(() -> {
              resolveStreamComplete();
              fireEvent(new DatasetsSyncedEvent(this));
            })
        );
  }

  /**
   * Closes the sidebar, releases the UI binding, and disposes any
   * in-flight subscription.
   */
  public void close() {
    if (activeSubscription != null) {
      activeSubscription.dispose();
      activeSubscription = null;
    }
    if (detachRegistration != null) {
      detachRegistration.remove();
      detachRegistration = null;
    }
    uiHandle.unbind();
    overlay.getStyle().set("display", "none");
    panel.getStyle().set("display", "none");
  }

  private void show() {
    overlay.getStyle().set("display", "block");
    panel.getStyle().set("display", "block");
  }

  /**
   * Registers a listener invoked when a sync trigger completes (regardless
   * of how many records were updated). The parent view refreshes the
   * connected-datasets grid in response.
   */
  public com.vaadin.flow.shared.Registration addDatasetsSyncedListener(
      com.vaadin.flow.component.ComponentEventListener<DatasetsSyncedEvent> listener) {
    return addListener(DatasetsSyncedEvent.class, listener);
  }

  private void disposeOnDetach() {
    if (activeSubscription != null) {
      activeSubscription.dispose();
      activeSubscription = null;
    }
    detachRegistration = null;
  }

  // ── Row rendering ───────────────────────────────────────────────────

  private static final class RowState {
    final String rowId;
    final String resourceProvider;
    final Div row;
    final Div spinner;
    final Span statusText;

    RowState(String rowId, String resourceProvider, Div row, Div spinner, Span statusText) {
      this.rowId = rowId;
      this.resourceProvider = resourceProvider;
      this.row = row;
      this.spinner = spinner;
      this.statusText = statusText;
    }
  }

  private void renderRows(List<ConnectedDatasetView> datasets) {
    rows.clear();
    rowOrder.clear();
    rowsContainer.removeAll();
    for (ConnectedDatasetView view : datasets) {
      var row = new Div();
      row.addClassNames("srs-row");

      var titleSpan = new Span(view.title());
      titleSpan.addClassName("normal-body-text");
      titleSpan.getStyle().set("font-weight", "600");
      titleSpan.getElement().setAttribute("title", view.title());

      var statusRow = new Div();
      statusRow.addClassNames("flex-horizontal", "items-center", "gap-02");

      var spinner = new Div();
      spinner.addClassName("srs-spinner");

      var statusText = new Span("Syncing…");
      statusText.addClassName("extra-small-body-text");
      statusText.getStyle().set("color", SECONDARY_COLOR);

      statusRow.add(spinner, statusText);
      row.add(titleSpan, statusRow);
      rowsContainer.add(row);

      rows.put(view.id(), new RowState(view.id(), view.resourceProvider(), row, spinner, statusText));
      rowOrder.add(view.id());
    }
  }

  /**
   * Resolves one per-dataset response into its row.
   */
  private void resolveRow(SyncDatasetResponse response) {
    RowState state = rows.get(response.datasetId().value());
    if (state == null) {
      log.debug("Sync response for unknown dataset {} — sidebar closed?", state);
      return;
    }
    state.spinner.getStyle().set("display", "none");
    switch (response.status()) {
      case UPDATED -> {
        state.statusText.getStyle().set("color", SUCCESS_COLOR);
        StringBuilder text = new StringBuilder("Updated");
        if (response.newVersion() != null && !response.newVersion().isBlank()) {
          text.append(" — now ").append(normalizeVersion(response.newVersion()));
        }
        if (response.accessStatusChanged()) {
          text.append(" (access status changed)");
        }
        state.statusText.setText(text.toString());
        updateSummary(updated + 1, upToDate, failed);
      }
      case UP_TO_DATE -> {
        state.statusText.getStyle().set("color", SECONDARY_COLOR);
        state.statusText.setText("Up to date");
        updateSummary(updated, upToDate + 1, failed);
      }
      case FAILED -> {
        state.statusText.getStyle().set("color", ERROR_COLOR);
        state.statusText.setText(failureText(response.error(), state.resourceProvider));
        if (response.error() == SyncDatasetError.CREDENTIAL_REQUIRED
            || response.error() == SyncDatasetError.CREDENTIAL_INSUFFICIENT
            || response.error() == SyncDatasetError.ACCESS_LINK_REFRESH_FAILED) {
          credentialsHint.getStyle().set("display", "block");
        }
        updateSummary(updated, upToDate, failed + 1);
      }
      default -> { }
    }
  }

  private String failureText(SyncDatasetError error, String resourceProvider) {
    return switch (error) {
      case CREDENTIAL_REQUIRED ->
          "Cannot be synced — configure a connection for the " + resourceProvider
              + " provider first.";
      case CREDENTIAL_INSUFFICIENT ->
          "Cannot be synced — your token does not grant access to this restricted record.";
      case ACCESS_LINK_REFRESH_FAILED ->
          "Cannot be synced — only the record owner can refresh the access link for the new version.";
      case RECORD_NOT_FOUND ->
          "Record no longer exists on the source. Connection kept.";
      case ALREADY_CONNECTED ->
          "Not synced — a connection to this version already exists in the project.";
      case DATASET_NOT_FOUND ->
          "Dataset connection no longer available.";
      case SYNC_FAILED ->
          "Sync failed. Please try again in a moment.";
    };
  }

  private static String normalizeVersion(String version) {
    return "v" + version.replaceFirst("^v", "");
  }

  // ── Summary ─────────────────────────────────────────────────────────

  private int updated;
  private int upToDate;
  private int failed;

  private void updateSummary(int updated, int upToDate, int failed) {
    this.updated = updated;
    this.upToDate = upToDate;
    this.failed = failed;
    summaryLabel.setText("%d updated · %d up to date · %d not synced".formatted(
        updated, upToDate, failed));
  }

  private void resolveStreamComplete() {
    summaryLabel.setText("%d updated · %d up to date · %d not synced".formatted(
        updated, upToDate, failed));
    summaryLabel.getStyle().set("color",
        failed == 0 && updated > 0 ? SUCCESS_COLOR : SECONDARY_COLOR);
  }

  private void resolveStreamError() {
    int unresolved = rows.size() - updated - upToDate - failed;
    if (unresolved > 0) {
      failed += unresolved;
    }
    summaryLabel.setText("%d updated · %d up to date · %d not synced".formatted(
        updated, upToDate, failed));
    summaryLabel.getStyle().set("color", ERROR_COLOR);
  }
}