package life.qbic.datamanager.views.login;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.Serial;
import java.time.Instant;
import java.util.Optional;
import life.qbic.datamanager.security.mfa.MfaPendingLogin;
import life.qbic.datamanager.views.landing.LandingPageLayout;
import life.qbic.identity.application.mfa.MfaReenrolmentService;

/**
 * <b>The landing page of the mailed second-factor enrolment link</b>
 *
 * <p>Its whole job is to turn a token into the one thing the enrolment page accepts: a
 * {@link MfaPendingLogin}. It then forwards there, so the page that collects the factor is the same one
 * the registration path uses — one enrolment page, two doors.</p>
 *
 * <h3>Every rejection looks the same</h3>
 * <p>Unknown, expired, already used, wrong flow, account already enrolled: the user sees one sentence
 * and a way back to the login form (AUTH-NFR-03 — this route is unauthenticated, so it must not disclose
 * account state). The differences are logged inside the service, never rendered.</p>
 *
 * <h3>What redeeming does not grant</h3>
 * <p>Not a session, and not a factor. It permits <em>starting</em> an enrolment for that account; the
 * factor exists only once a code from the new device verifies it. A compromised mailbox therefore still
 * cannot produce a second factor.</p>
 *
 * @since 1.0.0
 */
@PageTitle("Set up two-factor authentication")
@Route(value = "login/re-enrol", layout = LandingPageLayout.class)
@AnonymousAllowed
@UIScope
public class ReEnrolLinkView extends VerticalLayout implements BeforeEnterObserver {

  @Serial
  private static final long serialVersionUID = 1L;

  /** Must match the endpoint the link supplier builds ({@code routing.mfa-reenrol.endpoint}). */
  public static final String PATH = "/login/re-enrol";

  /** The query parameter the link carries; matches {@code routing.mfa-reenrol.token-parameter}. */
  public static final String TOKEN_PARAMETER = "token";

  private final transient MfaReenrolmentService reenrolmentService;

  public ReEnrolLinkView(MfaReenrolmentService reenrolmentService) {
    this.reenrolmentService = requireNonNull(reenrolmentService,
        "reenrolmentService must not be null");
    setAlignItems(FlexComponent.Alignment.CENTER);
    getStyle().set("padding-top", "var(--lumo-space-xl)");
    getStyle().set("padding-bottom", "var(--lumo-space-xl)");
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    HttpServletRequest request = currentHttpRequest();
    HttpSession session = request == null ? null : request.getSession(false);
    String token = request == null ? null : request.getParameter(TOKEN_PARAMETER);

    Optional<MfaReenrolmentService.ReenrolCandidate> candidate =
        token == null || token.isBlank()
            ? Optional.empty()
            : reenrolmentService.redeem(token, Instant.now());

    if (candidate.isEmpty()) {
      // No distinction between the failure reasons — see the class documentation.
      renderRejected(request == null || request.getContextPath() == null ? "" : request.getContextPath());
      return;
    }

    // From here on the session carries exactly what a verified password would have produced, and
    // nothing more: a pending login. The enrolment page requires one, and it is cleared as soon as the
    // factor is verified.
    MfaReenrolmentService.ReenrolCandidate account = candidate.get();
    if (session != null) {
      MfaPendingLogin.clear(session);
      session.setAttribute(MfaPendingLogin.SESSION_ATTRIBUTE,
          new MfaPendingLogin(account.userId(), account.emailAddress(), Instant.now()));
    }
    // Reroute, not forward: the token must leave the address bar once it has been redeemed (it is in
    // the browser history either way, but a consumed token in a bookmark only confuses), and the
    // enrolment page then owns the URL the user will retry from.
    event.rerouteTo(RequiredEnrolmentView.class);
  }

  private void renderRejected(String contextPath) {
    H2 heading = new H2("This link is no longer valid");
    Paragraph body = new Paragraph(
        "An enrolment link works once and expires after 15 minutes. Sign in again and ask for a new "
            + "link from the second-factor page.");
    // The context path belongs in the href: this deployment runs under /dev, and "/login" would leave it.
    Anchor back = new Anchor(contextPath + "/login", "Back to sign in");
    add(heading, body, back);
  }

  private static HttpServletRequest currentHttpRequest() {
    VaadinRequest request = VaadinService.getCurrentRequest();
    return request instanceof VaadinServletRequest servletRequest ? servletRequest : null;
  }
}
