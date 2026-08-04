package life.qbic.datamanager.views.account;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import life.qbic.application.commons.time.DateTimeFormat;
import life.qbic.datamanager.views.UiHandle;
import life.qbic.datamanager.views.UserMainLayout;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.general.Tag;
import life.qbic.datamanager.views.general.Tag.TagColor;
import life.qbic.datamanager.views.general.dialog.AlertDialog;
import life.qbic.datamanager.views.general.dialog.AppDialog;
import life.qbic.datamanager.views.general.dialog.DialogBody;
import life.qbic.datamanager.views.general.dialog.DialogFooter;
import life.qbic.datamanager.views.general.dialog.DialogHeader;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import life.qbic.projectmanagement.application.associated_dataset.ExternalCredentialService;
import life.qbic.projectmanagement.domain.model.associated_dataset.CredentialStatus;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * External Providers configuration page.
 *
 * <p>Allows a user to view and manage their personal access tokens
 * for external data source instances (e.g. InvenioRDM instances such
 * as Zenodo and FDAT), so they can connect access-restricted datasets
 * to their projects.</p>
 *
 * <p>Route: {@code /external-providers}, laid out with
 * {@link UserMainLayout} alongside the user's profile and personal
 * access token pages.</p>
 *
 * @since 1.12.0
 */
@Route(value = "external-providers", layout = UserMainLayout.class)
@SpringComponent
@UIScope
@PermitAll
public class ExternalProvidersMain extends Main
    implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 6739821508412736524L;
  private static final Logger log = LoggerFactory.logger(
      ExternalProvidersMain.class);



  private static final String SECONDARY_COLOR =
      "var(--lumo-secondary-text-color)";

  private final transient ExternalCredentialService credentialService;
  private final transient AuthenticationToUserIdTranslationService userIdTranslator;

  private final UiHandle uiHandle = new UiHandle();

  private final Div content = new Div();
  private final VerificationSidebar verificationSidebar;

  public ExternalProvidersMain(
      ExternalCredentialService credentialService,
      AuthenticationToUserIdTranslationService userIdTranslator) {
    this.credentialService = requireNonNull(credentialService,
        "credentialService must not be null");
    this.userIdTranslator = requireNonNull(userIdTranslator,
        "userIdTranslator must not be null");

    verificationSidebar = new VerificationSidebar(
        credentialService, userIdTranslator);
    verificationSidebar.addReconnectRequestListener(e ->
        openAddTokenDialog(e.getInstanceId()));
    // When the sidebar finishes a verification run, refresh the main
    // page so any tokens that proved to be invalid now render as red
    // INVALIDATED cards (with a Reconnect + Disconnect action).
    verificationSidebar.addValidationsCompletedListener(
        e -> renderContent());
    // When the sidebar closes (button, backdrop, programmatic) do a
    // fresh status fetch. This catches the tail-end case where a
    // validation ran while the drawer was open, the main view refreshed
    // mid-flight, and the user then closed the drawer.
    verificationSidebar.addSidebarClosedListener(
        e -> renderContent());

    addClassName("external-providers");
    content.addClassNames("external-providers__content");
    add(content, verificationSidebar);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    renderContent();
  }

  // ─ Rendering ──────────────────────────────────────────────────

  private void renderContent() {
    content.removeAll();

    // ── Heading & benefit text (AC-6) ──
    var heading = new H2("External Providers");
    heading.addClassNames("font-semibold", "text-size-l", "m-0");

    var benefit = new Paragraph(
        "Connect your personal access tokens to enable access to "
            + "access-restricted datasets on external instances. Once "
            + "connected, you can link restricted datasets from these "
            + "instances to your Data Manager projects.");
    benefit.addClassNames("text-contrast-70pct", "text-size-s",
        "mt-xs", "mb-s");

    // ─ Security reassurance ─
    var securityNote = new Span(
        "Tokens are encrypted at rest and never shared with third "
            + "parties. You can disconnect at any time.");
    securityNote.addClassNames("security-note", "mb-m");

    content.add(heading, benefit, securityNote);

    // ── Toolbar (Verify connections) ──
    var toolbar = new Div();
    toolbar.addClassNames("external-providers__toolbar");
    var verifyButton = new Button("Verify connections", VaadinIcon.REFRESH.create());
    verifyButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    verifyButton.addClickListener(e -> {
      // If sidebar is open, refresh; otherwise open it
      if (isSidebarOpen()) {
        verificationSidebar.refresh();
      } else {
        verificationSidebar.open();
      }
    });
    toolbar.add(verifyButton);
    content.add(toolbar);

    // ── Instance list (AC-1, AC-2) ──
    // TODO: Consider incremental DOM updates instead of full re-render
    List<ExternalCredentialService.CredentialStatusView> statuses =
        credentialService.listCredentialStatuses(userId());

    if (statuses.isEmpty()) {
      content.add(new Paragraph(
          "No external data source instances are currently configured. "
              + "Contact your administrator to add instances."));
      return;
    }

    for (var status : statuses) {
      content.add(renderInstanceCard(status));
    }
  }

  // ── Card rendering ─────────────────────────────────────────────

  private Div renderInstanceCard(
      ExternalCredentialService.CredentialStatusView status) {
    String stateClass;
    if (!status.configured()) {
      stateClass = "instance-card--not-connected";
    } else if (isInvalidated(status)) {
      stateClass = "instance-card--invalidated";
    } else {
      stateClass = "instance-card--connected";
    }

    var card = new Div();
    card.addClassNames("instance-card", stateClass);

    // ── Provider identity ──
    String name = status.instanceDisplayName();
    String baseUrl = status.instanceBaseUrl();

    // Header: name + URL (left), action button (right)
    var header = new Div();
    header.addClassNames("instance-card__header");

    var identity = new Div();
    identity.addClassNames("instance-card__identity");

    var providerName = new Span(name);
    providerName.addClassNames("font-bold", "text-size-l");

    if (baseUrl != null && !baseUrl.isBlank()) {
      String displayUrl = stripTrailingSlash(baseUrl);
      var urlAnchor = new Anchor(baseUrl + "/", displayUrl);
      urlAnchor.addClassNames("instance-card__url", "text-size-s");
      urlAnchor.setTarget(AnchorTarget.BLANK);
      identity.add(providerName, urlAnchor);
    } else {
      identity.add(providerName);
    }

    header.add(identity);

    // Action buttons (top-right)
    if (isInvalidated(status)) {
      // Side-by-side Reconnect + Disconnect for invalid state
      var buttonGroup = new Div();
      buttonGroup.addClassNames("instance-card__action-group");
      buttonGroup.add(buildReconnectButton(status));
      buttonGroup.add(buildDisconnectButton(status));
      header.add(buttonGroup);
    } else if (status.configured()) {
      var actionButton = buildDisconnectButton(status);
      actionButton.addClassNames("instance-card__action");
      header.add(actionButton);
    } else {
      var actionButton = buildConnectButton(status);
      actionButton.addClassNames("instance-card__action");
      header.add(actionButton);
    }
    card.add(header);

    // Body: status label + date + description
    var body = new Div();
    body.addClassNames("instance-card__body");

    var statusLine = new Div();
    statusLine.addClassNames("instance-card__status-line");

    if (status.configured()) {
      if (isInvalidated(status)) {
        var tag = new Tag("Token invalid");
        tag.setTagColor(TagColor.ERROR);
        statusLine.add(tag);
      } else {
        var tag = new Tag("Connected");
        tag.setTagColor(TagColor.SUCCESS);
        statusLine.add(tag);
      }
      if (status.configuredAt() != null) {
        var dateSep = new Span("·");
        dateSep.getStyle().set("color", "var(--lumo-contrast-30pct)");
        dateSep.addClassNames("extra-small-body-text");

        String dateText = "since "
            + DateTimeFormat.asJavaFormatter(DateTimeFormat.SIMPLE_DATE_SHORT, ZoneId.systemDefault())
            .format(status.configuredAt());
        var dateSpan = new Span(dateText);
        dateSpan.addClassNames("small-body-text");
        dateSpan.getStyle().set("color", SECONDARY_COLOR);

        statusLine.add(dateSep, dateSpan);
      }
    } else {
      var tag = new Tag("Not connected");
      tag.setTagColor(TagColor.CONTRAST);
      statusLine.add(tag);
    }

    body.add(statusLine);

    var description = new Paragraph(describeProvider(status, name));
    description.addClassNames("instance-card__description");
    body.add(description);
    card.add(body);

    return card;
  }

  // ── Descriptive text per state ─────────────────────────────────

  private String describeProvider(
      ExternalCredentialService.CredentialStatusView status,
      String name) {
    if (!status.configured()) {
      return "Connect your account to link " + name
          + " datasets to your projects.";
    }
    if (isInvalidated(status)) {
      return "Your token was rejected by " + name
          + " — please reconnect to restore access.";
    }
    return "Your personal access token allows Data Manager to read "
        + "your access-restricted datasets on " + name + ".";
  }

  // ── Action buttons ─────────────────────────────────────────────

  private Button buildConnectButton(
      ExternalCredentialService.CredentialStatusView status) {
    var button = new Button("Connect", VaadinIcon.PLUS.create());
    button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    button.addClickListener(
        e -> openAddTokenDialog(status));
    return button;
  }

  /** Reconnect button — visually identical to Connect. */
  private Button buildReconnectButton(
      ExternalCredentialService.CredentialStatusView status) {
    var button = new Button("Reconnect", VaadinIcon.RECYCLE.create());
    button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    button.addClickListener(
        e -> openAddTokenDialog(status));
    return button;
  }

  private Button buildDisconnectButton(
      ExternalCredentialService.CredentialStatusView status) {
    var button = new Button("Disconnect", VaadinIcon.TRASH.create());
    button.addThemeVariants(
        ButtonVariant.LUMO_TERTIARY_INLINE,
        ButtonVariant.LUMO_ERROR);
    button.addClickListener(
        e -> confirmDisconnectToken(status));
    return button;
  }

  // ── Disconnect confirmation ────────────────────────────────────

  private void confirmDisconnectToken(
      ExternalCredentialService.CredentialStatusView status) {
    AlertDialog.alert(this)
        .danger()
        .title("Disconnect from " + status.instanceDisplayName() + "?")
        .message("This will remove your personal access token for "
            + status.instanceDisplayName()
            + ". You will need to reconnect to link datasets "
            + "from this instance again.")
        .confirmButton("Disconnect",
            () -> performDisconnect(status))
        .cancelButton("Cancel", () -> { /* dismiss only */ })
        .build()
        .open();
  }

  private void performDisconnect(
      ExternalCredentialService.CredentialStatusView status) {
    boolean removed = credentialService.removeCredential(
        userId(), status.instanceId());
    if (removed) {
      showToast("Disconnected from " + status.instanceDisplayName(),
          NotificationVariant.LUMO_WARNING);
    } else {
      showToast("No token found to remove for "
              + status.instanceDisplayName(),
          NotificationVariant.LUMO_WARNING);
    }
    renderContent();
    // If the verification sidebar is open, refresh it to reflect the
    // new NOT_CONFIGURED state after disconnect
    if (isSidebarOpen()) {
      verificationSidebar.refresh();
    }
  }

  // ── Connect → Add Token dialog (AC-3, AC-4, AC-5) ─────────────

  /**
   * Opens the Add Token dialog for the given instance ID.
   * Used by both the card "Connect"/"Reconnect" buttons and the
   * sidebar's Reconnect request event.
   */
  private void openAddTokenDialog(String instanceId) {
    List<ExternalCredentialService.CredentialStatusView> all =
        credentialService.listCredentialStatuses(userId());
    var target = all.stream()
        .filter(s -> s.instanceId().equals(instanceId))
        .findFirst();
    if (target.isEmpty()) {
      showToast("Unknown instance: " + instanceId,
          NotificationVariant.LUMO_WARNING);
      return;
    }
    openAddTokenDialog(target.get());
  }

  private void openAddTokenDialog(
      ExternalCredentialService.CredentialStatusView status) {
    String tokenUrl = buildTokenCreationUrl(
        status.instanceBaseUrl());

    var dialog = AppDialog.medium();

    String providerName = extractProviderName(status.instanceDisplayName());

    var tokenInput = new TokenInput(providerName, tokenUrl);

    var loadingOverlay = buildLoadingOverlay(providerName);

    var bodyWrapper = new Div(tokenInput, loadingOverlay);
    bodyWrapper.getElement().getStyle().set("position", "relative");

    DialogHeader.with(dialog, "Connect to " + providerName);
    DialogBody.with(dialog, bodyWrapper, tokenInput);
    DialogFooter.with(dialog, "Cancel", "Validate & Save");

    dialog.registerCancelAction(dialog::close);

    var validationRunning = new AtomicBoolean(false);

    dialog.registerConfirmAction(() ->
        validateAndSaveToken(status, providerName, tokenInput,
            dialog, loadingOverlay, validationRunning));

    dialog.open();
  }

  // ── Helpers ───────────────────────────────────────────────────

  private String extractProviderName(String displayName) {
    if (displayName == null) {
      return "this provider";
    }
    int open = displayName.indexOf('(');
    if (open > 0) {
      return displayName.substring(0, open).trim();
    }
    return displayName;
  }

  static String buildTokenCreationUrl(String baseUrl) {
    if (baseUrl == null || baseUrl.isBlank()) {
      return null;
    }
    String normalized = baseUrl.endsWith("/")
        ? baseUrl : baseUrl + "/";
    return normalized + "account/settings/applications/";
  }

  private String userId() {
    return userIdTranslator.translateToUserId(
        SecurityContextHolder.getContext().getAuthentication())
        .orElseThrow();
  }

  private void showToast(
      String message, NotificationVariant variant) {
    var notification = new Notification();
    notification.addThemeVariants(variant);
    notification.setPosition(
        Notification.Position.BOTTOM_START);
    notification.setDuration(4000);
    notification.add(new Span(message));
    notification.open();
  }

  private boolean isSidebarOpen() {
    return verificationSidebar.isOpen();
  }

  private static boolean isInvalidated(
      ExternalCredentialService.CredentialStatusView status) {
    return status.status() == CredentialStatus.INVALIDATED;
  }

  private static String stripTrailingSlash(String url) {
    if (url != null && url.endsWith("/")) {
      return url.substring(0, url.length() - 1);
    }
    return url;
  }

  private Div buildLoadingOverlay(String providerName) {
    var spinner = new Div();
    spinner.addClassName("spinner");

    var loadingLabel = new Span(
        "Validating your token with " + providerName + "…");
    loadingLabel.addClassName("token-loading-label");

    var loadingHint = new Span("This may take a few seconds.");
    loadingHint.addClassName("token-loading-hint");

    var overlay = new Div();
    overlay.addClassNames("token-loading-overlay");
    overlay.add(spinner, loadingLabel, loadingHint);
    overlay.getElement().getStyle().set("display", "none");
    return overlay;
  }

  private void validateAndSaveToken(
      ExternalCredentialService.CredentialStatusView status,
      String providerName,
      TokenInput tokenInput,
      AppDialog dialog,
      Div loadingOverlay,
      AtomicBoolean validationRunning) {
    if (!validationRunning.compareAndSet(false, true)) {
      return;
    }
    tokenInput.setEnabled(false);
    loadingOverlay.getElement().getStyle().set("display", "flex");

    final char[] token = tokenInput.getToken();
    final var userIdCurrent = userId();
    uiHandle.bind(UI.getCurrent());
    final var securityContext = SecurityContextHolder.getContext();

    CompletableFuture.supplyAsync(() -> {
      SecurityContextHolder.setContext(securityContext);
      try {
        return credentialService.addCredential(
            userIdCurrent, status.instanceId(), token);
      } finally {
        SecurityContextHolder.clearContext();
      }
    }).whenComplete((result, throwable) -> {
      uiHandle.onUiAndPush(() -> {
        loadingOverlay.getElement().getStyle().set("display", "none");
        tokenInput.setEnabled(true);
        validationRunning.set(false);
        handleValidationResult(result, throwable, status, providerName,
            tokenInput, dialog);
      });
    });
  }

  private void handleValidationResult(
      Object result, Throwable throwable,
      ExternalCredentialService.CredentialStatusView status,
      String providerName,
      TokenInput tokenInput, AppDialog dialog) {
    if (throwable != null) {
      tokenInput.setError("Could not validate the token. Please try again.");
      showToast("Token validation failed due to an unexpected error.",
          NotificationVariant.LUMO_ERROR);
      log.error("Credential validation threw unexpectedly for instance '"
          + status.instanceId() + "': " + throwable.getMessage(), throwable);
      return;
    }

    if (result instanceof ExternalCredentialService.Success) {
      tokenInput.clearField();
      dialog.close();
      showToast("Token validated successfully. " + providerName
          + " is now connected.", NotificationVariant.LUMO_SUCCESS);
      renderContent();
      if (isSidebarOpen()) {
        verificationSidebar.refresh();
      }
    } else if (result instanceof ExternalCredentialService.InvalidToken(String reason)) {
      tokenInput.setError("Token was rejected by " + providerName
          + ". Please check and try again.");
      showToast("Token validation failed: " + reason
          + ". Please check your token and try again.",
          NotificationVariant.LUMO_ERROR);
    } else if (result instanceof ExternalCredentialService.ServiceError(String reason)) {
      tokenInput.setError(
          "Could not validate the token now. Please try again later.");
      showToast("Token validation failed due to a transient error: "
          + reason + ". Please try again later.",
          NotificationVariant.LUMO_ERROR);
      log.warn("Credential validation service error for instance '"
          + status.instanceId() + "': " + reason);
    } else if (result instanceof ExternalCredentialService.UnknownInstance) {
      tokenInput.setError("Unknown instance.");
      log.error("Add-token attempted for unknown instance '"
          + status.instanceId() + "'");
    }
  }

}
