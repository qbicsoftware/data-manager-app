package life.qbic.datamanager.security;

import static java.util.Objects.requireNonNull;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import life.qbic.datamanager.views.AppRoutes;
import life.qbic.identity.api.OidcLinkOutcomeMessage;
import life.qbic.identity.api.OidcLinkService;
import life.qbic.identity.api.OidcLinkService.Outcome;
import life.qbic.logging.api.Logger;
import life.qbic.logging.service.LoggerFactory;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationCodeGrantFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * <b>Completes an ORCID link after the provider redirect (AUTH-R-07).</b>
 *
 * <h3>Why a filter and not a view or a success handler</h3>
 * <p>A view can be navigated to directly, so a view that decides "did the provider assertion succeed?"
 * can be made to claim success. A success handler, in the {@code oauth2Login} arrangement this replaces,
 * ran <em>after</em> Spring Security had already written an authenticated provider principal into the
 * session — which is precisely the state a second-factor requirement cannot tolerate, and the reason the
 * whole story exists.</p>
 *
 * <p>This filter runs after {@link OAuth2AuthorizationCodeGrantFilter} has exchanged the code, so the
 * {@link OAuth2AuthorizedClient} is already stored for the current principal. It then reads the ORCID iD
 * and discards the token. No {@code Authentication} is ever created, changed or restored here — the
 * session continues as the user who started the link.</p>
 *
 * <h3>The two things it refuses</h3>
 * <ol>
 *   <li><b>No pending link.</b> Without the marker the request is not a link returning; it is someone
 *       hitting the callback URL. Nothing happens.</li>
 *   <li><b>No password-authenticated principal.</b> Linking requires an account whose owner proved
 *       themselves with a password. A provider principal — which can no longer exist, but is checked
 *       anyway — or an anonymous request is refused.</li>
 * </ol>
 *
 * @since 1.20.4
 */
public class OidcLinkCompletionFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.logger(OidcLinkCompletionFilter.class);

  /** Session key holding the pending-link marker; see {@link OidcLinkController}. */
  static final String PENDING_LINK = OidcLinkController.PENDING_LINK;

  /** Session key holding the sanitized path to return to. */
  static final String RETURN_TO = OidcLinkController.RETURN_TO;

  /** The OAuth2 registration id configured in {@code application.properties}. */
  static final String REGISTRATION_ID = "orcid";

  /** Bound so a hanging provider cannot hold the request thread open indefinitely. */
  private static final Duration USERINFO_TIMEOUT = Duration.ofSeconds(10);

  private final OidcLinkService oidcLinkService;
  private final OAuth2AuthorizedClientService authorizedClientService;
  private final String issuer;
  private final HttpClient httpClient;
  private final ObjectMapper json;

  /**
   * @param oidcLinkService          the port that writes the attribute
   * @param authorizedClientService  where the grant filter left the provider token
   * @param issuer                   the configured issuer of the ORCID registration. Taken from
   *                                 configuration, never from the assertion: a provider that could
   *                                 name its own issuer could rename itself into an accepted value.
   */
  public OidcLinkCompletionFilter(OidcLinkService oidcLinkService,
      OAuth2AuthorizedClientService authorizedClientService, String issuer) {
    this.oidcLinkService = requireNonNull(oidcLinkService, "oidcLinkService must not be null");
    this.authorizedClientService = requireNonNull(authorizedClientService,
        "authorizedClientService must not be null");
    this.issuer = requireNonNull(issuer, "issuer must not be null");
    this.httpClient = HttpClient.newBuilder().connectTimeout(USERINFO_TIMEOUT).build();
    this.json = new ObjectMapper();
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    // Only the provider's callback completes a link. Without this check the filter would act on the
    // FIRST request that happens to arrive while a marker is set — a favicon fetch, a push poll — and
    // consume the marker before the callback ever came back.
    if (!isOrcidCallback(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    var session = request.getSession(false);
    Object pending = session == null ? null : session.getAttribute(PENDING_LINK);
    if (!(pending instanceof String userId) || userId.isBlank()) {
      // A callback with no pending link is someone hitting the URL directly, or a stale tab. Nothing
      // to complete, and nothing to say about it.
      filterChain.doFilter(request, response);
      return;
    }

    // Read the return path BEFORE clearing, or it is always the default (the marker and the path are
    // removed together, and reading after removal silently loses the user's location).
    String returnTo = Optional.ofNullable((String) session.getAttribute(RETURN_TO))
        .filter(path -> !path.isBlank())
        .orElse("/settings/profile");

    // A pending link exists, so this request belongs to the link flow and must end in a redirect.
    clearMarker(session);

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (!(authentication != null && authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof QbicUserDetails)) {
      // The link was started by an authenticated user; by the time the provider sends them back, the
      // session no longer says so. Refuse rather than guess — and say nothing about accounts.
      log.warn("ORCID link return without a password-authenticated principal; refusing");
      redirect(request, response, returnTo, session, OidcLinkOutcomeMessage.SESSION_ENDED);
      return;
    }

    Optional<String> oidcId = readOrcidId(authentication);
    if (oidcId.isEmpty()) {
      log.error("ORCID link return without a readable ORCID iD in the provider response");
      redirect(request, response, returnTo, session, OidcLinkOutcomeMessage.NO_IDENTIFIER);
      return;
    }

    Outcome outcome = oidcLinkService.link(userId, oidcId.get(), issuer);
    OidcLinkOutcomeMessage message = switch (outcome) {
      case Outcome.Linked ignored -> OidcLinkOutcomeMessage.LINKED;
      case Outcome.AlreadyLinked ignored -> OidcLinkOutcomeMessage.ALREADY_LINKED;
      case Outcome.HeldByAnotherAccount ignored -> OidcLinkOutcomeMessage.HELD_BY_ANOTHER_ACCOUNT;
      case Outcome.InvalidIdentifier ignored -> OidcLinkOutcomeMessage.INVALID_IDENTIFIER;
      case Outcome.UnknownAccount ignored -> OidcLinkOutcomeMessage.UNKNOWN_ACCOUNT;
      // Unlink outcomes cannot be produced by link(); listed exhaustively so a new outcome is a
      // compile error here rather than a silently unhandled case.
      case Outcome.Unlinked ignored -> OidcLinkOutcomeMessage.UNKNOWN_ACCOUNT;
      case Outcome.NotLinked ignored -> OidcLinkOutcomeMessage.UNKNOWN_ACCOUNT;
    };
    redirect(request, response, returnTo, session, message);
  }

  /**
   * @return whether this request is the ORCID authorization-code callback
   */
  private static boolean isOrcidCallback(HttpServletRequest request) {
    // Matched on the servlet path so a context path, proxy prefix or trailing parameter cannot change
    // the answer. The registration id is ours, not user input.
    String path = request.getServletPath();
    return path != null && path.endsWith("/login/oauth2/code/" + REGISTRATION_ID);
  }

  /**
   * Reads the ORCID iD from the provider's response and discards the token that made it available.
   *
   * <p>Reads {@code sub}: ORCID returns the bare iD there ({@code 0000-0002-1825-0097}), not a URI. The
   * response also carries {@code name}/{@code given_name}/{@code family_name}, which are ignored — the
   * account's own name is authoritative and must not be overwritten by a provider.</p>
   */
  private Optional<String> readOrcidId(Authentication authentication) {
    String principalName = authentication.getName();
    OAuth2AuthorizedClient client =
        authorizedClientService.loadAuthorizedClient(REGISTRATION_ID, principalName);
    if (client == null) {
      // The code exchange did not produce a client for this principal. Nothing to read.
      return Optional.empty();
    }
    try {
      // The client holds the access token, not the user's attributes: oauth2Client() performs the code
      // exchange but does not call the userinfo endpoint, because nothing needs a principal. So we call
      // it ourselves, once, with the token we just obtained over TLS using our own client secret.
      return fetchSubject(client);
    } finally {
      // The link stores the iD and nothing else. No access token, no refresh token, no id token — the
      // application must not become a holder of provider credentials just because it read one attribute.
      authorizedClientService.removeAuthorizedClient(REGISTRATION_ID, principalName);
    }
  }

  /**
   * Calls the provider's userinfo endpoint and returns {@code sub}.
   *
   * <p>ORCID returns the bare iD in {@code sub} ({@code 0000-0002-1825-0097}), not a URI. The response
   * also carries {@code name}/{@code given_name}/{@code family_name}, which are deliberately ignored:
   * the account's own name is authoritative and must not be overwritten by a provider.</p>
   *
   * <p>Any failure is reported as "no identifier", never as an exception that would escape the filter
   * and leave the browser on a blank page mid-link.</p>
   */
  private Optional<String> fetchSubject(OAuth2AuthorizedClient client) {
    URI userInfoUri = client.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri()
        == null ? null
        : URI.create(client.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri());
    if (userInfoUri == null) {
      log.error("The ORCID client registration has no userinfo endpoint configured");
      return Optional.empty();
    }
    try {
      HttpRequest request = HttpRequest.newBuilder(userInfoUri)
          .header("Authorization", "Bearer " + client.getAccessToken().getTokenValue())
          .header("Accept", "application/json")
          .timeout(USERINFO_TIMEOUT)
          .GET()
          .build();
      HttpResponse<String> response = httpClient.send(request,
          HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() != 200) {
        log.error("ORCID userinfo returned status " + response.statusCode());
        return Optional.empty();
      }
      JsonNode node = json.readTree(response.body());
      JsonNode subject = node == null ? null : node.get("sub");
      return subject != null && !subject.asText().isBlank()
          ? Optional.of(subject.asText())
          : Optional.empty();
    } catch (IOException e) {
      log.error("Could not read the ORCID userinfo response: " + e.getClass().getSimpleName());
      return Optional.empty();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Interrupted while reading the ORCID userinfo response");
      return Optional.empty();
    }
  }

  private static void clearMarker(jakarta.servlet.http.HttpSession session) {
    session.removeAttribute(PENDING_LINK);
    session.removeAttribute(RETURN_TO);
  }

  /**
   * Sends the browser back to the settings page, leaving the outcome in the session for it to read.
   *
   * <h3>Why not in the URL</h3>
   * <p>An earlier version redirected to {@code ?error=1&reason=<sentence>}. Two problems, one of them
   * serious:</p>
   * <ul>
   *   <li>The view rendered the level flag as the message, so every failure surfaced as "failed: 1."</li>
   *   <li>Anyone could craft {@code /settings/profile?error=1&reason=<their own text>} and have the
   *       application render it in a trusted notification on its own domain — a ready-made pretext for a
   *       phishing step.</li>
   * </ul>
   * <p>The outcome is decided here, in the same request that redirects, so nothing has to survive a
   * round trip through the address bar. A one-shot session attribute keeps the wording in the message
   * bundle and out of anything a caller can influence.</p>
   *
   * <p>The cost, stated rather than hidden: the notification does not survive a page reload. That is the
   * right behaviour — a stale "your link failed" after a refresh is noise — and the settings page shows
   * the true state regardless, because it renders from the account, not from this message.</p>
   */
  private static void redirect(HttpServletRequest request, HttpServletResponse response,
      String returnTo, jakarta.servlet.http.HttpSession session, OidcLinkOutcomeMessage outcome)
      throws IOException {
    if (session != null) {
      session.setAttribute(OidcLinkController.OUTCOME, outcome);
    }
    response.sendRedirect(request.getContextPath() + returnTo);
  }

}
