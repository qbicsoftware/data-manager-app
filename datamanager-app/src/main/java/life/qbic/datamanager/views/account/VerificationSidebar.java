package life.qbic.datamanager.views.account;

import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.shared.Registration;
import java.io.Serial;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import life.qbic.datamanager.views.UiHandle;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import life.qbic.projectmanagement.application.associated_dataset.ExternalCredentialService;
import life.qbic.projectmanagement.application.associated_dataset.ExternalCredentialService.AddCredentialResult;
import life.qbic.projectmanagement.application.associated_dataset.ExternalCredentialService.CredentialStatusView;
import life.qbic.projectmanagement.application.associated_dataset.ExternalCredentialService.InvalidToken;
import life.qbic.projectmanagement.application.associated_dataset.ExternalCredentialService.Success;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Slide-in drawer that verifies all configured external provider connections.
 *
 * <p>On open, it lists all available providers. Configured ones show a loading
 * spinner ("Verifying…"); unconfigured ones immediately show a greyed-out
 * "Not connected" row — no validation is attempted for them.</p>
 *
 * <p>Configured instances are then validated in parallel against their remote
 * providers. Rows update as results arrive:</p>
 * <ul>
 *   <li><b>Valid</b>: Green checkmark + "Valid"</li>
 *   <li><b>Invalid</b>: Red exclamation + "Invalid" + inline "Reconnect" link</li>
 *   <li><b>Not connected</b>: Grey plug icon + "Not connected" (skipped from
 *       the validation run)</li>
 * </ul>
 *
 * <p>Clicking "Reconnect" inside an invalid row fires a
 * {@link ReconnectRequestEvent} handled by the parent view to open the
 * token-input dialog. The sidebar stays open throughout.</p>
 *
 * <p>Matches the pattern used by
 * {@link life.qbic.datamanager.views.projects.project.datasets.ConnectDatasetSidebar}.</p>
 *
 * @since 1.13.0
 */
public class VerificationSidebar extends Div {

  @Serial private static final long serialVersionUID = 1L;

  private final ExternalCredentialService credentialService;
  private final AuthenticationToUserIdTranslationService userIdTranslator;
  private final UiHandle uiHandle = new UiHandle();

  private final Div overlay;
  private final Div panel;
  private final Div contentContainer; // Holds the rows

  /** Maps instanceId → row Div so results can update rows in place. */
  private final Map<String, Div> rowMap = new ConcurrentHashMap<>();

  /** Maps instanceId → display name, captured at run start for stable rendering. */
  private final Map<String, String> displayNameMap = new ConcurrentHashMap<>();

  public VerificationSidebar(
      ExternalCredentialService credentialService,
      AuthenticationToUserIdTranslationService userIdTranslator) {
    this.credentialService = credentialService;
    this.userIdTranslator = userIdTranslator;

    addClassName("verification-sidebar");
    getStyle().set("display", "none");

    // ── Overlay (backdrop) ──
    overlay = new Div();
    overlay.addClassNames("vs-backdrop");
    overlay.addClickListener(e -> close());
    add(overlay);

    // ── Panel ──
    panel = new Div();
    panel.addClassNames("vs-panel");

    var body = new Div();
    body.addClassNames("vs-body");

    // Header
    var header = new Div();
    header.addClassNames("vs-header");

    var titleSpan = new Span("Verify connections");
    titleSpan.addClassNames("vs-title", "heading-3");

    var closeButton = new Button(VaadinIcon.CLOSE_SMALL.create());
    closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    closeButton.setTooltipText("Close");
    closeButton.addClickListener(e -> close());

    header.add(titleSpan, closeButton);
    body.add(header);

    // Content: Scrollable list of provider rows
    contentContainer = new Div();
    contentContainer.addClassNames("vs-content");
    body.add(contentContainer);

    panel.add(body);
    add(panel);
  }

  // ── Public API ──────────────────────────────────────────────────────

  /**
   * Opens the sidebar and starts a fresh verification run.
   * If the sidebar is already open, use {@link #refresh()} instead
   * so the user sees the "Reset + verify" behaviour rather than
   * two concurrent runs.
   */
  public void open() {
    uiHandle.bind(UI.getCurrent());
    getStyle().set("display", "block");
    // Force a reflow so the CSS transform animation can run
    panel.getElement().executeJs("");
    // Clear any previous state so repeated open() calls don't duplicate rows
    contentContainer.removeAll();
    rowMap.clear();
    displayNameMap.clear();
    addClassName("vs-open");
    runVerification();
  }

  /** Closes the sidebar immediately (no slide-out transition). */
  public void close() {
    removeClassName("vs-open");
    getStyle().set("display", "none");
    uiHandle.unbind();
    // Notify the parent view so it can re-fetch credential statuses.
    // This catches every exit path (close button, backdrop click, programmatic
    // close) in a single hook — the main view then re-renders with the
    // freshest state, including any INVALIDATED transitions that the
    // validation run may have persisted to the database.
    fireEvent(new SidebarClosedEvent(this));
  }

  /**
   * Clears current rows and starts a new verification run.
   * Use when the user clicks the toolbar button while the sidebar
   * is already open.
   */
  public void refresh() {
    contentContainer.removeAll();
    rowMap.clear();
    displayNameMap.clear();
    runVerification();
  }

  public Registration addReconnectRequestListener(
      ComponentEventListener<ReconnectRequestEvent> listener) {
    return addListener(ReconnectRequestEvent.class, listener);
  }

  /**
   * Registers a listener that fires once per verification run when every
   * configured provider has produced a result. The listener runs on the
   * UI thread, so it is safe to trigger a full-page re-render from it.
   */
  public Registration addValidationsCompletedListener(
      ComponentEventListener<ValidationsCompletedEvent> listener) {
    return addListener(ValidationsCompletedEvent.class, listener);
  }

  /**
   * Registers a listener that fires every time the sidebar is closed —
   * regardless of exit path (button, backdrop, programmatic).
   */
  public Registration addSidebarClosedListener(
      ComponentEventListener<SidebarClosedEvent> listener) {
    return addListener(SidebarClosedEvent.class, listener);
  }

  // ── Verification logic ────────────────────────────────────────────

  private void runVerification() {
    String userId = userIdTranslator.translateToUserId(
        SecurityContextHolder.getContext().getAuthentication()).orElseThrow();

    // Capture security context for propagation to async worker threads
    SecurityContext securityContext = SecurityContextHolder.getContext();

    // Fetch a fresh list of statuses up-front (one query, not N)
    var statuses = credentialService.listCredentialStatuses(userId);

    // 1. Render all rows: configured ones start with a spinner, others
    //    show "Not connected" immediately.
    for (var status : statuses) {
      displayNameMap.put(status.instanceId(), status.instanceDisplayName());
      Div row = status.configured()
          ? buildPendingRow(status.instanceDisplayName())
          : buildNotConnectedRow(status.instanceDisplayName());
      rowMap.put(status.instanceId(), row);
      contentContainer.add(row);
    }

    // 2. Fire async validation ONLY for providers that have a token.
    //    Unconfigured providers stay in the "Not connected" state — no
    //    validation is attempted, respecting ADR-0002 (no silent status
    //    changes without explicit user action).
    //
    //    Track how many configured providers are still in flight so we
    //    can fire a single "all-done" event once every async call has
    //    settled — the main view listens for this and refreshes its
    //    cards to reflect the new INVALIDATED / VALID statuses.
    final int configuredCount = (int) statuses.stream()
        .filter(CredentialStatusView::configured).count();
    if (configuredCount == 0) {
      // Nothing to validate — emit completion immediately so the main
      // view can still refresh (e.g. after a second run).
      fireEvent(new ValidationsCompletedEvent(this));
    } else {
      var remaining =
          new java.util.concurrent.atomic.AtomicInteger(configuredCount);
      for (var status : statuses) {
        if (status.configured()) {
          triggerValidation(status, securityContext, remaining);
        }
      }
    }
  }

  private void triggerValidation(
      CredentialStatusView status,
      SecurityContext securityContext,
      java.util.concurrent.atomic.AtomicInteger remainingInFlight) {

    CompletableFuture.supplyAsync(() -> {
      SecurityContextHolder.setContext(securityContext);
      try {
        String userId = userIdTranslator.translateToUserId(
            SecurityContextHolder.getContext().getAuthentication()).orElseThrow();
        return credentialService.validateCredential(userId, status.instanceId());
      } finally {
        SecurityContextHolder.clearContext();
      }
    }).whenComplete((result, throwable) -> {
      uiHandle.onUiAndPush(() -> {
        if (throwable != null) {
          // Transient failure — keep it informative; the user can retry.
          updateRowToStatus(status.instanceId(),
              "Error", null, false, true, false);
        } else if (result instanceof Success) {
          updateRowToStatus(status.instanceId(),
              "Valid", null, true, false, false);
        } else if (result instanceof InvalidToken inv) {
          // Token was there but is now rejected — ERROR state, NOT skipped.
          // The row must render as red "Invalid" with a Reconnect link so
          // the user can recover from either the sidebar or the main page
          // (where the card mirrors the same INVALIDATED state).
          updateRowToStatus(status.instanceId(),
              "Invalid", inv.reason(), false, false, false);
        } else {
          // Unknown / unhandled result form — defensive fallback
          updateRowToStatus(status.instanceId(),
              "Error", null, false, true, false);
        }

        // Last validation to finish — notify the main view to refresh
        // its cards so the invalidation is reflected there as well.
        if (remainingInFlight.decrementAndGet() == 0) {
          fireEvent(new ValidationsCompletedEvent(this));
        }
      });
    });
  }

  // ── Row construction ──────────────────────────────────────────────

  /**
   * Initial placeholder row for a configured provider: spinner + "Verifying...".
   */
  private Div buildPendingRow(String displayName) {
    var row = new Div();
    row.addClassNames("vs-row", "vs-row--pending");

    var identity = new Div();
    identity.addClassNames("vs-row__identity");

    var nameSpan = new Span(displayName);
    nameSpan.addClassNames("vs-row__name");

    var statusDiv = new Div();
    statusDiv.addClassNames("vs-row__status");

    var spinner = new Div();
    spinner.addClassName("vs-spinner");

    var label = new Span("Verifying…");
    label.addClassName("vs-status-label--pending");

    statusDiv.add(spinner, label);
    identity.add(nameSpan, statusDiv);
    row.add(identity);
    return row;
  }

  /**
   * Immediate "skipped" row for a provider without a token. No spinner, no
   * validation is attempted.
   */
  private Div buildNotConnectedRow(String displayName) {
    var row = new Div();
    row.addClassNames("vs-row", "vs-row--skipped");

    var identity = new Div();
    identity.addClassNames("vs-row__identity");

    var nameSpan = new Span(displayName);
    nameSpan.addClassNames("vs-row__name");

    var statusDiv = new Div();
    statusDiv.addClassNames("vs-row__status", "vs-status--skipped");

    var icon = VaadinIcon.PLUG.create();
    icon.getStyle().set("color", "var(--lumo-tertiary-text-color)");
    statusDiv.add(icon);
    statusDiv.add(new Span("Not connected"));

    identity.add(nameSpan, statusDiv);
    row.add(identity);
    return row;
  }

  /**
   * Updates an existing row (identified by instance id) to its final
   * state. The row is rebuilt in place so the user sees an in-place
   * transition rather than a flash.
   *
   * @param instanceId    stable instance key
   * @param message       short status text ("Valid", "Invalid", "Error")
   * @param invalidReason optional detail for {@code Invalid} rows
   * @param isValid       true → green row
   * @param isError       true → red row for transient failure
   * @param isSkipped     true → grey "skipped from verification" styling
   */
  private void updateRowToStatus(
      String instanceId,
      String message,
      String invalidReason,
      boolean isValid,
      boolean isError,
      boolean isSkipped) {

    Div row = rowMap.get(instanceId);
    if (row == null) {
      return;
    }

    String displayName = displayNameMap.getOrDefault(instanceId, instanceId);

    // Reset state classes that may have been set during the pending render
    row.removeClassName("vs-row--pending");
    row.removeClassName("vs-row--valid");
    row.removeClassName("vs-row--invalid");
    row.removeClassName("vs-row--skipped");

    if (isSkipped) {
      row.addClassName("vs-row--skipped");
    } else if (isError) {
      row.addClassName("vs-row--invalid");
    } else if (isValid) {
      row.addClassName("vs-row--valid");
    }

    // Clear and rebuild
    row.removeAll();

    var identity = new Div();
    identity.addClassNames("vs-row__identity");

    var nameSpan = new Span(displayName);
    nameSpan.addClassNames("vs-row__name");

    var statusDiv = new Div();
    statusDiv.addClassNames("vs-row__status");

    if (isSkipped) {
      var icon = VaadinIcon.PLUG.create();
      icon.getStyle().set("color", "var(--lumo-tertiary-text-color)");
      statusDiv.add(icon);
      statusDiv.add(new Span("Not connected"));
    } else if (isValid) {
      var icon = VaadinIcon.CHECK_CIRCLE.create();
      icon.getStyle().set("color", "var(--lumo-success-color)");
      statusDiv.add(icon);
      var label = new Span(message);
      label.addClassName("vs-status-label--valid");
      statusDiv.add(label);
    } else {
      // Invalid or Error
      VaadinIcon iconSymbol = isError
          ? VaadinIcon.EXCLAMATION_CIRCLE_O
          : VaadinIcon.EXCLAMATION_CIRCLE_O;
      var icon = iconSymbol.create();
      icon.getStyle().set("color", "var(--lumo-error-color)");
      statusDiv.add(icon);
      var label = new Span(message != null ? message : "Error");
      label.addClassName("vs-status-label--invalid");
      statusDiv.add(label);
      if (invalidReason != null && !invalidReason.isBlank()) {
        var reasonSpan = new Span("— " + invalidReason);
        reasonSpan.addClassName("vs-invalid-reason");
        identity.add(reasonSpan);
      }
    }

    identity.add(nameSpan, statusDiv);
    row.add(identity);

    // Actions column: Reconnect is only meaningful when the row shows
    // an invalidated token (explicit user recovery path per ADR-0002).
    if (!isValid && !isSkipped && !isError) {
      var actions = new Div();
      actions.addClassNames("vs-row__actions");
      var reconnect = new Button("Reconnect");
      reconnect.addClassName("vs-reconnect-link");
      reconnect.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
      reconnect.addClickListener(e ->
          fireEvent(new ReconnectRequestEvent(this, instanceId)));
      actions.add(reconnect);
      row.add(actions);
    }
  }

  // ── Events ────────────────────────────────────────────────────────

  /**
   * Fired when the user clicks the inline "Reconnect" link in an
   * invalidated row. The parent view opens the token input dialog
   * (overlay) without closing this sidebar.
   */
  public static class ReconnectRequestEvent
      extends ComponentEvent<VerificationSidebar> {

    @Serial private static final long serialVersionUID = 1L;

    private final String instanceId;

    public ReconnectRequestEvent(
        VerificationSidebar source, String instanceId) {
      super(source, false);
      this.instanceId = instanceId;
    }

    /** Provider instance identifier (e.g. {@code "zenodo"}). */
    public String getInstanceId() {
      return instanceId;
    }
  }

  /**
   * Fired once per {@link #runVerification()} invocation when every
   * configured provider's async validation has settled — success,
   * failure, or rejection. Listeners can use this moment to re-paint
   * dependent surfaces (e.g. the main provider cards, which must
   * reflect the new {@code INVALIDATED} status).
   */
  public static class ValidationsCompletedEvent
      extends ComponentEvent<VerificationSidebar> {

    @Serial private static final long serialVersionUID = 1L;

    public ValidationsCompletedEvent(VerificationSidebar source) {
      super(source, false);
    }
  }

  /**
   * Fired every time the sidebar is closed — regardless of the exit path
   * (close button, backdrop click, or programmatic {@link #close()} call).
   * The parent view uses this as the signal that the drawer is no longer
   * overlaying the page and the underlying cards should re-read their
   * status from the database so any in-run transitions (e.g. a token
   * proving invalid mid-flight) are reflected immediately.
   */
  public static class SidebarClosedEvent
      extends ComponentEvent<VerificationSidebar> {

    @Serial private static final long serialVersionUID = 1L;

    public SidebarClosedEvent(VerificationSidebar source) {
      super(source, false);
    }
  }

}
