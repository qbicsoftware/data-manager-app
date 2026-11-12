package life.qbic.datamanager.views.register

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.Location
import com.vaadin.flow.router.NavigationTrigger
import com.vaadin.flow.router.QueryParameters
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.Router
import com.vaadin.flow.server.RouteRegistry
import com.vaadin.flow.server.VaadinService
import com.vaadin.flow.server.VaadinSession
import life.qbic.identity.api.UserInfo
import life.qbic.identity.api.UserInformationService
import life.qbic.identity.application.communication.EmailService
import life.qbic.identity.application.throttle.AuthAttemptThrottle
import life.qbic.identity.application.user.policy.EmailConfirmationLinkSupplier
import life.qbic.identity.api.VerificationLinkService
import life.qbic.identity.domain.repository.AuthAttemptCounter
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant

/**
 * <b>When the registration session is gone, the page must say so — not offer a button that fails.</b>
 *
 * <p>The pending-confirmation page enables "Resend verification link" from a session attribute, because the
 * account id must not travel in the URL. The session is not guaranteed to survive: it expires
 * ({@code server.servlet.session.timeout}, 30 minutes by default), it is lost on a node restart, and behind a
 * load balancer a request can be served by an instance that never saw the registration.</p>
 *
 * <p>In all of those cases the resend action is impossible. Presenting it anyway produced an error on click;
 * hiding it silently would read as a broken page. The requirement is information plus a way forward.</p>
 *
 * <p><b>The way forward must be support, not re-registration or a password reset.</b> Both are dead ends for
 * an unconfirmed account: a repeated registration is answered uniformly and deliberately sends no email
 * (AUTH-NFR-03), and a reset request for an unconfirmed address dispatches nothing either. Telling the
 * visitor to do either would send them in a circle.</p>
 */
class ResendUnavailableSpec extends Specification {

  UserInformationService userInformationService
  EmailService emailService
  AuthAttemptThrottle throttle
  Router router
  UI currentUi

  def setup() {
    def registry = Stub(RouteRegistry)
    registry.getTargetUrl(_, _ as RouteParameters) >> Optional.of('login')
    router = new Router(registry)
    def service = Stub(VaadinService)
    service.getRouter() >> router
    service.getContext() >> null
    VaadinService.setCurrent(service)
    currentUi = Stub(UI)

    userInformationService = Stub(UserInformationService)
    emailService = Mock(EmailService)
    throttle = new AuthAttemptThrottle(new AllowingCounter(),
        10, Duration.ofMinutes(15), Duration.ofMinutes(15),
        3, Duration.ofMinutes(15), Duration.ofMinutes(15))
  }

  def cleanup() {
    VaadinSession.setCurrent(null)
    VaadinService.setCurrent(null)
  }

  @Unroll
  def "without a pending registration the page explains it instead of offering resend (#reason)"() {
    given: 'a page reached without any registration context'
    def view = newView()

    when:
    enter(view, sessionWithPendingId)

    then: 'the resend link is gone, so it cannot fail on click'
    !text(view).contains('Resend verification link')

    and: 'the visitor is told why, and where to go'
    def lower = text(view).toLowerCase()
    lower.contains('session has expired')
    lower.contains('support@qbic.zendesk.com')

    where:
    reason                                   | sessionWithPendingId
    'session expired or opened directly'     | null
  }

  def "the page does not send the visitor to a dead end"() {
    given:
    def view = newView()

    when:
    enter(view, null)

    then: 'neither re-registration nor a password reset is suggested, because both silently do nothing'
    def lower = text(view).toLowerCase()
    !lower.contains('register again')
    !lower.contains('forgot your password')
  }

  def "with a valid pending registration the resend link is still offered"() {
    given: 'a session that remembers the account the registration just created'
    userInformationService.findById('user-1') >> Optional.of(
        new UserInfo('user-1', 'Alice Example', 'alice@example.org', 'alice', false, null, null))
    def view = newView()

    when:
    enter(view, 'user-1')

    then: 'the normal flow is untouched'
    text(view).contains('Resend verification link')
    !text(view).toLowerCase().contains('session has expired')
  }

  def "a stale pending id is treated like an absent one"() {
    given: 'the session holds an id that no longer resolves to an account'
    userInformationService.findById('gone') >> Optional.empty()
    def view = newView()

    when:
    enter(view, 'gone')

    then: 'no resend link, and the same explanation'
    !text(view).contains('Resend verification link')
    text(view).toLowerCase().contains('session has expired')
  }

  // ---- helpers -------------------------------------------------------------------------------

  private EmailConfirmationMain newView() {
    return new EmailConfirmationMain(userInformationService, emailService,
        new EmailConfirmationLinkSupplier('https', 'example.org', 443, '',
            '/registration/confirm', 'token'),
        Stub(VerificationLinkService), throttle)
  }

  private void enter(EmailConfirmationMain view, String pendingId) {
    def session = Stub(VaadinSession) {
      getAttribute(EmailConfirmationMain.PENDING_REGISTRATION_USER_ID) >> pendingId
    }
    VaadinSession.setCurrent(session)
    try {
      view.beforeEnter(new BeforeEnterEvent(router, NavigationTrigger.PROGRAMMATIC,
          new Location('register/pending-email-confirmation', QueryParameters.empty()),
          EmailConfirmationMain, currentUi, List.of()))
    } finally {
      VaadinSession.setCurrent(null)
    }
  }

  private static String text(Component component) {
    def parts = []
    if (component instanceof Span) {
      parts << component.text
    }
    if (component.hasProperty('text')) {
      try { parts << component.text } catch (Exception ignored) { }
    }
    component.children.each { parts << text(it) }
    return parts.findAll { it }.join(' ')
  }

  static class AllowingCounter implements AuthAttemptCounter {
    @Override
    Counter increment(String scope, String key, Instant now, Duration window) {
      return new Counter(1, null)
    }

    @Override
    Counter peek(String scope, String key) { return new Counter(0, null) }

    @Override
    void block(String scope, String key, Instant blockedUntil) { }

    @Override
    void clear(String scope, String key) { }
  }
}
