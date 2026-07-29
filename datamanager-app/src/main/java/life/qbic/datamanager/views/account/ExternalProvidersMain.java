package life.qbic.datamanager.views.account;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import java.io.Serial;
import java.util.List;
import life.qbic.datamanager.views.UserMainLayout;
import life.qbic.datamanager.views.general.Main;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import life.qbic.projectmanagement.application.associated_dataset.ExternalCredentialService;
import life.qbic.projectmanagement.application.associated_dataset.ExternalCredentialService;
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

  private final transient ExternalCredentialService credentialService;
  private final transient AuthenticationToUserIdTranslationService userIdTranslator;

  private final Div content = new Div();

  public ExternalProvidersMain(
      ExternalCredentialService credentialService,
      AuthenticationToUserIdTranslationService userIdTranslator) {
    this.credentialService = requireNonNull(credentialService,
        "credentialService must not be null");
    this.userIdTranslator = requireNonNull(userIdTranslator,
        "userIdTranslator must not be null");
    addClassName("external-providers");
    content.addClassNames("external-providers__content");
    add(content);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    renderContent();
  }

  // ── Rendering ──────────────────────────────────────────────────

  private void renderContent() {
    content.removeAll();

    var userId = userIdTranslator.translateToUserId(
            SecurityContextHolder.getContext().getAuthentication())
        .orElseThrow();

    // ── Heading & benefit text (AC-6) ──
    var heading = new H2("External Providers");
    heading.addClassNames("font-semibold", "text-size-l", "m-0");

    var benefit = new Paragraph(
        "Connect your personal access tokens to enable access to "
            + "access-restricted datasets on external instances. Once "
            + "connected, you can link restricted datasets from these "
            + "instances to your Data Manager projects.");
    benefit.addClassNames("text-contrast-70pct", "text-size-s",
        "mt-xs", "mb-m");

    content.add(heading, benefit);

    // ── Instance list (AC-1, AC-2) ──
    List<ExternalCredentialService.CredentialStatusView> statuses =
        credentialService.listCredentialStatuses(userId);

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

  private Div renderInstanceCard(ExternalCredentialService.CredentialStatusView status) {
    var card = new Div();
    card.addClassNames("flex", "items-center", "gap-m", "p-m",
        "border", "rounded-s");

    // Status icon
    var icon = status.configured()
        ? VaadinIcon.CHECK_CIRCLE.create()
        : VaadinIcon.CLOSE_CIRCLE.create();
    icon.getStyle().set("font-size", "var(--lumo-font-size-xl)");
    icon.getStyle().set("color",
        status.configured() ? "var(--lumo-success-color)"
            : "var(--lumo-error-color)");

    // Instance info
    var info = new Div();
    info.addClassNames("flex", "flex-col", "gap-2xs", "flex-grow");

    var name = new Span(status.instanceDisplayName());
    name.addClassNames("font-bold", "text-size-m");

    var detail = new Span(describeStatus(status));
    detail.addClassNames("text-contrast-60pct", "text-size-xs");

    info.add(name, detail);

    // Action button
    var actionButton = status.configured()
        ? buildRemoveButton(status)
        : buildAddTokenButton(status);

    card.add(icon, info, actionButton);
    return card;
  }

  // ── Action buttons ─────────────────────────────────────────────

  private Button buildAddTokenButton(ExternalCredentialService.CredentialStatusView status) {
    var button = new Button("Add Token", VaadinIcon.PLUS.create());
    button.addThemeVariants(ButtonVariant.LUMO_PRIMARY,
        ButtonVariant.LUMO_SMALL);
    button.addClickListener(e -> openAddTokenDialog(status));
    return button;
  }

  private Button buildRemoveButton(ExternalCredentialService.CredentialStatusView status) {
    var button = new Button("Remove", VaadinIcon.TRASH.create());
    button.addThemeVariants(ButtonVariant.LUMO_TERTIARY,
        ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
    button.addClickListener(e -> {
      var userId = userIdTranslator.translateToUserId(
              SecurityContextHolder.getContext().getAuthentication())
          .orElseThrow();
      boolean removed = credentialService.removeCredential(
          userId, status.instanceId());
      if (removed) {
        showToast("Token removed for " + status.instanceDisplayName(),
            NotificationVariant.LUMO_SUCCESS);
      } else {
        showToast("No token found to remove for "
                + status.instanceDisplayName(),
            NotificationVariant.LUMO_WARNING);
      }
      renderContent();
    });
    return button;
  }

  // ── Add Token dialog (AC-3, AC-4, AC-5) ───────────────────────

  private void openAddTokenDialog(ExternalCredentialService.CredentialStatusView status) {
    var dialog = new Dialog();
    dialog.setHeaderTitle("Add Token — " + status.instanceDisplayName());
    dialog.setCloseOnOutsideClick(false);
    dialog.setModal(true);

    var form = new Div();
    form.addClassNames("flex", "flex-col", "gap-s");

    var description = new Paragraph(
        "Paste your personal access token from your "
            + status.instanceDisplayName() + " account.");
    description.addClassNames("text-contrast-70pct", "text-size-s");

    var tokenField = new PasswordField("Personal Access Token");
    tokenField.setPlaceholder("Paste token here");
    tokenField.setRequired(true);
    tokenField.setMinLength(1);
    tokenField.setWidthFull();

    // Help link to token settings (standard InvenioRDM pattern)
    var helpLine = new Div();
    helpLine.addClassNames("text-size-xs", "text-contrast-60pct");
    helpLine.add(new Span(
        "Your token is stored encrypted and used only to access "
            + "your own restricted datasets. You can create one at: "));
    String tokenUrl = buildTokenCreationUrl(
        status.instanceDisplayName());
    helpLine.add(new Anchor(tokenUrl,
        status.instanceDisplayName() + " token settings",
        AnchorTarget.BLANK));

    form.add(description, tokenField, helpLine);
    dialog.add(form);

    dialog.getFooter().add(
        new Button("Cancel", e -> dialog.close()),
        new Button("Validate & Save", e ->
            performValidationAndSave(status, dialog, tokenField)));

    dialog.addOpenedChangeListener(e -> {
      if (!e.isOpened()) {
        tokenField.clear();
        tokenField.setInvalid(false);
        tokenField.setErrorMessage("");
      }
    });

    dialog.open();
  }

  private void performValidationAndSave(ExternalCredentialService.CredentialStatusView status,
      Dialog dialog, PasswordField tokenField) {
    String rawToken = tokenField.getValue();
    if (rawToken == null || rawToken.isBlank()) {
      tokenField.setErrorMessage("Token must not be empty");
      tokenField.setInvalid(true);
      return;
    }

    var userId = userIdTranslator.translateToUserId(
            SecurityContextHolder.getContext().getAuthentication())
        .orElseThrow();

    ExternalCredentialService.AddCredentialResult result = credentialService.addCredential(
        userId, status.instanceId(), rawToken.toCharArray());
    // Token has been zeroed by the service before returning

    if (result instanceof ExternalCredentialService.Success) {
      showToast("Token validated successfully. "
              + status.instanceDisplayName() + " is now connected.",
          NotificationVariant.LUMO_SUCCESS);
      dialog.close();
      renderContent();
    } else if (result instanceof ExternalCredentialService.InvalidToken inv) {
      tokenField.setErrorMessage("Token was rejected");
      tokenField.setInvalid(true);
      showToast("Token validation failed: " + inv.reason()
              + ". Please check your token and try again.",
          NotificationVariant.LUMO_ERROR);
    } else if (result instanceof ExternalCredentialService.ServiceError err) {
      tokenField.setErrorMessage("Validation could not be completed");
      tokenField.setInvalid(true);
      showToast("Token validation failed due to a transient error: "
              + err.reason() + ". Please try again later.",
          NotificationVariant.LUMO_ERROR);
      log.warn("Credential validation service error for instance '"
          + status.instanceId() + "': " + err.reason());
    } else if (result instanceof ExternalCredentialService.UnknownInstance) {
      tokenField.setErrorMessage("Unknown instance");
      tokenField.setInvalid(true);
    }
  }

  // ── Helpers ────────────────────────────────────────────────────

  private static String describeStatus(ExternalCredentialService.CredentialStatusView status) {
    if (!status.configured()) {
      return "No token configured";
    }
    return "Token configured · Status: " + status.status();
  }

  /**
   * Best-effort construction of the token creation URL.
   * InvenioRDM instances follow the standard path
   * {@code {baseUrl}/account/settings/applications/}.
   * Falls back to "#" when the display name cannot be parsed.
   */
  private static String buildTokenCreationUrl(String displayName) {
    if (displayName == null) return "#";
    int openParen = displayName.lastIndexOf('(');
    int closeParen = displayName.lastIndexOf(')');
    if (openParen >= 0 && closeParen > openParen + 1) {
      String baseUrl = displayName.substring(
          openParen + 1, closeParen).trim();
      if (!baseUrl.contains(" ")) {
        return baseUrl.endsWith("/")
            ? baseUrl + "account/settings/applications/"
            : baseUrl + "/account/settings/applications/";
      }
    }
    return "#";
  }

  private void showToast(String message, NotificationVariant variant) {
    var notification = new Notification();
    notification.addThemeVariants(variant);
    notification.setPosition(
        com.vaadin.flow.component.notification.Notification.Position.BOTTOM_START);
    notification.setDuration(4000);
    notification.add(new Span(message));
    notification.open();
  }

}
