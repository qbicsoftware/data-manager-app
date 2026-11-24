package life.qbic.datamanager.security.mfa;

import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.spring.security.VaadinDefaultRequestCache;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import life.qbic.logging.api.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * <b>Turns "the second factor was presented" into the session the password did not get</b>
 *
 * <p>Two pages need this and they must not diverge: the challenge ({@code /login/second-factor}) and
 * mandatory enrolment ({@code /login/enrol}). Both arrive the same way — a password verified, the
 * authentication withdrawn, a {@link MfaPendingLogin} left in the session — and both end the same way.
 * The rules below are therefore stated once.</p>
 *
 * <h3>What "the same way" means</h3>
 * <ul>
 *   <li><b>The principal is reloaded</b> through {@link UserDetailsService} rather than reconstructed,
 *       so authorities are exactly what a password login would have produced. The factor step grants
 *       nothing extra; it completes the same login (AUTH-R-01).</li>
 *   <li><b>The marker is stored</b> ({@link MfaSessionState}), because
 *       {@link MfaChallengeEnforcementFilter} would otherwise re-decide on the next request and start a
 *       challenge loop.</li>
 *   <li><b>The pending login is cleared</b>, so a completed step cannot be replayed from the session.</li>
 *   <li><b>The saved request wins over a hard-coded target</b>, and its URL already carries the context
 *       path. Without one, the fallback must be the context root, not the host root: this deployment
 *       runs under {@code /dev}, and {@code /} would leave it — a working login landing on a 404.</li>
 * </ul>
 *
 * <p><b>Why the session is not rotated again here:</b> Spring rotates the id inside the password
 * filter, before either page is reachable, so the session that carries the pending login is already a
 * fresh one that nobody could have fixed in advance. Rotating underneath a live Vaadin UI would be the
 * bigger risk.</p>
 *
 * @since 1.0.0
 */
@Component
public class MfaSessionCompletion {

  private static final Logger log = logger(MfaSessionCompletion.class);

  private final UserDetailsService userDetailsService;
  private final VaadinDefaultRequestCache requestCache;

  public MfaSessionCompletion(UserDetailsService userDetailsService,
      VaadinDefaultRequestCache requestCache) {
    this.userDetailsService = requireNonNull(userDetailsService,
        "userDetailsService must not be null");
    this.requestCache = requireNonNull(requestCache, "requestCache must not be null");
  }

  /**
   * Establishes the authentication for a login whose factor step just succeeded, then sends the
   * browser on.
   *
   * @param pending the pending login being completed; must not be null
   */
  public void complete(MfaPendingLogin pending) {
    requireNonNull(pending, "pending must not be null");

    UserDetails user = userDetailsService.loadUserByUsername(pending.email());
    var authentication = UsernamePasswordAuthenticationToken.authenticated(
        user, null, user.getAuthorities());

    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);

    HttpServletRequest request = (HttpServletRequest) VaadinService.getCurrentRequest();
    HttpServletResponse response = (HttpServletResponse) VaadinService.getCurrentResponse();
    new HttpSessionSecurityContextRepository().saveContext(context, request, response);

    HttpSession session = request.getSession(false);
    MfaSessionState.store(session, pending.userId(), Instant.now());
    MfaPendingLogin.clear(session);
    log.info("Second factor completed for user %s; session established".formatted(pending.userId()));

    var saved = requestCache.getRequest(request, response);
    String contextPath = request.getContextPath() == null ? "" : request.getContextPath();
    String target = saved == null ? contextPath + "/" : saved.getRedirectUrl();
    UI.getCurrent().getPage().setLocation(target);
  }

  /**
   * Where a completed login lands when nothing was saved before it started.
   *
   * <p>Exposed for tests: the fallback target is a deployment fact (the context path), not a
   * detail of this method.</p>
   *
   * @param request the current request
   * @return the context root, e.g. {@code /dev/}
   */
  static String defaultTarget(HttpServletRequest request) {
    String contextPath = request.getContextPath();
    return (contextPath == null ? "" : contextPath) + "/";
  }
}
