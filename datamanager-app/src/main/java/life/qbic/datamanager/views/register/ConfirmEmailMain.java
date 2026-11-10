package life.qbic.datamanager.views.register;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.datamanager.views.general.Main;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.datamanager.views.login.LoginLayout;
import life.qbic.identity.api.VerificationLinkService;
import life.qbic.identity.api.VerificationLinkService.Issued;
import life.qbic.identity.application.user.IdentityService;
import life.qbic.identity.application.user.policy.EmailConfirmationLinkSupplier;
import life.qbic.logging.api.Logger;

/**
 * <b>Confirm an email address.</b>
 *
 * <p>Owns the confirmation link (AUTH-R-05). Three properties matter here and each maps to a
 * requirement:</p>
 *
 * <ol>
 *   <li><b>Retrieval changes nothing.</b> A mail scanner, link prefetcher or corporate proxy that
 *       GETs this URL must not confirm anything — the link would be spent before the user saw it.
 *       The state change happens only on the explicit button press.</li>
 *   <li><b>One message for every failure.</b> Unknown, expired, already-used and wrong-purpose
 *       tokens all render identically (AUTH-NFR-03). Telling the visitor which it was would disclose
 *       account state to an unauthenticated caller.</li>
 *   <li><b>The token never survives in the address bar.</b> After a successful confirmation the
 *       token is replaced in the URL, so a shared, bookmarked or history-recalled link is drained.</li>
 * </ol>
 *
 * @since 1.0.0
 */
@Route(value = AppRoutes.CONFIRM_EMAIL, layout = LandingPageLayout.class)
@PageTitle("Confirm email address")
@AnonymousAllowed
public class ConfirmEmailMain extends Main implements BeforeEnterObserver {

  private static final Logger log = logger(ConfirmEmailMain.class);

  private final transient VerificationLinkService verificationLinkService;
  private final transient IdentityService identityService;
  private final transient EmailConfirmationLinkSupplier linkSupplier;

  private final transient String tokenParameter;

  private final Div content = new Div();

  private String rawToken;
  private boolean confirmationPerformed;

  public ConfirmEmailMain(VerificationLinkService verificationLinkService,
      IdentityService identityService,
      EmailConfirmationLinkSupplier linkSupplier,
      @org.springframework.beans.factory.annotation.Value(
          "${routing.email-confirmation.token-parameter:token}") String tokenParameter) {
    this.verificationLinkService = requireNonNull(verificationLinkService);
    this.identityService = requireNonNull(identityService);
    this.linkSupplier = requireNonNull(linkSupplier);
    this.tokenParameter = requireNonNull(tokenParameter);

    // .main is the theme's page-level grid (display:grid; height:100%). Every sibling landing view
    // gets it by extending Main; ConfirmEmailMain does too, and the component below carries the
    // same "email-confirm" spacing rules EmailConfirmationMain uses.
    addClassName("email-confirm");
    content.addClassName("email-confirm-component");
    add(content);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Map<String, List<String>> params = event.getLocation().getQueryParameters().getParameters();
    rawToken = params.containsKey(tokenParameter) ? params.get(tokenParameter).getFirst() : null;
    // Deliberately render only. No state changes on retrieval — see class javadoc.
    renderPrompt();
  }

  /** Shows the explicit "confirm" action. Nothing has happened yet. */
  private void renderPrompt() {
    content.removeAll();
    if (rawToken == null || rawToken.isBlank()) {
      renderFailure();
      return;
    }
    H2 title = new H2("Confirm your email address");
    Paragraph text = new Paragraph(
        "Click the button below to confirm your email address and finish your registration.");
    Button confirm = new Button("Confirm email address", it -> confirm());
    confirm.addClassName("confirm-email-button");
    content.add(title, text, confirm, backToLogin());
  }

  /**
   * Performs the state change, on the user's explicit action only.
   */
  private void confirm() {
    if (confirmationPerformed || rawToken == null) {
      return;
    }
    confirmationPerformed = true;

    Optional<String> userId = verificationLinkService.consume(rawToken,
        VerificationLinkService.Purpose.EMAIL_CONFIRMATION, Instant.now());
    if (userId.isEmpty()) {
      // One branch for every rejection reason, on purpose (AUTH-NFR-03).
      log.info("Email confirmation rejected; token unusable, unknown, expired or already used.");
      renderFailure();
      return;
    }

    try {
      identityService.confirmUserEmail(userId.get());
    } catch (RuntimeException e) {
      log.warn("Token consumed but email confirmation failed for user %s: %s"
          .formatted(userId.get(), e.toString()));
      renderFailure();
      return;
    }

    // Replace the token in the address bar so a copied or history-recalled URL is worthless.
    getUI().ifPresent(ui -> ui.getPage().getHistory().replaceState(null, AppRoutes.CONFIRM_EMAIL));
    renderSuccess();
  }

  private void renderSuccess() {
    content.removeAll();
    content.add(new H2("Email address confirmed"),
        new Paragraph("You can now log in with your credentials."), backToLogin());
  }

  private void renderFailure() {
    content.removeAll();
    content.add(new H2("This link is no longer valid"),
        new Paragraph("The link has expired or has already been used. Request a new one below."),
        backToLogin());
  }

  private Div backToLogin() {
    Div section = new Div();
    section.add(new RouterLink("Go to login", LoginLayout.class));
    section.addClassName("back-to-login");
    return section;
  }
}
