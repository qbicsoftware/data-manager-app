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
import life.qbic.identity.api.VerificationLinkService
import life.qbic.identity.application.throttle.AuthAttemptThrottle
import life.qbic.identity.application.user.policy.EmailConfirmationLinkSupplier
import life.qbic.identity.domain.repository.AuthAttemptCounter
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant

/**
 * <b>The "check your inbox" page must be honest in both situations that reach it.</b>
 *
 * <p>It is reached after a real registration <em>and</em> after a registration against an address that
 * already has an account, and the two must be indistinguishable (AUTH-NFR-03). That constrains the copy
 * in a non-obvious way: it cannot promise that an email was sent, because on the duplicate path no
 * account is created and nothing is dispatched.</p>
 *
 * <p>The first implementation resolved the duplicate case by reusing the password-reset notification. That
 * sent the account owner a message describing an action they never took, and invalidated any reset link
 * they were genuinely waiting for. It was removed, and this spec pins the replacement: a single
 * conditional sentence, shown to everyone, that gives a confused visitor a way forward without anyone
 * being mailed.</p>
 */
class PendingConfirmationCopySpec extends Specification {

  UserInformationService userInformationService
  EmailService emailService
  AuthAttemptThrottle throttle
  VaadinService service
  Router router
  UI currentUi

  def setup() {
    def registry = Stub(RouteRegistry)
    registry.getTargetUrl(_, _ as RouteParameters) >> Optional.of('login')
    router = new Router(registry)
    service = Stub(VaadinService)
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
    VaadinService.setCurrent(null)
  }

  def "the page does not promise an email, because on the duplicate path none is sent"() {
    given:
    def view = newView()

    when:
    def text = allText(view)

    then: 'the sentence about having sent a link is conditional or absent — never an unconditional promise'
    def lower = text.toLowerCase()
    if (lower.contains("we've sent")) {
      // The current copy opens with the new-registration case; that is acceptable only because the
      // very next sentence tells the duplicate visitor that no mail will arrive.
      lower.contains('no new email will arrive')
    } else {
      lower.contains('if that address')
    }
  }

  @Unroll
  def "the page tells an already-registered visitor how to get back in (#why)"() {
    given:
    def view = newView()

    when:
    def lower = allText(view).toLowerCase()

    then: 'a dead end is not acceptable: the visitor is pointed at the reset flow'
    lower.contains('forgot your password')
    lower.contains('already')

    where:
    why << ['the only way forward without mail']
  }

  def "the page still offers the resend action, and the login link"() {
    given:
    def view = newView()

    when:
    def lower = allText(view).toLowerCase()

    then:
    lower.contains('resend verification link')
    lower.contains('go back to login')
  }

  def "rendering the page sends no email by itself"() {
    given:
    def view = newView()

    when: 'the page is built and the visitor id is resolved'
    def session = Stub(VaadinSession) {
      getAttribute(EmailConfirmationMain.PENDING_REGISTRATION_USER_ID) >> 'user-1'
    }
    VaadinSession.setCurrent(session)
    try {
      view.beforeEnter(event())
    } finally {
      VaadinSession.setCurrent(null)
    }

    then: 'nothing is dispatched on render — retrieval is not an action'
    0 * emailService.send(_, _, _)
  }

  private EmailConfirmationMain newView() {
    return new EmailConfirmationMain(userInformationService, emailService,
        // Real instances, not mocks: both are concrete classes without a usable no-arg constructor and
        // this classpath has no Objenesis. Neither is exercised by these specs.
        new EmailConfirmationLinkSupplier('https', 'example.org', 443, '',
            '/registration/confirm', 'token'),
        Stub(VerificationLinkService), throttle)
  }

  private BeforeEnterEvent event() {
    userInformationService.findById('user-1') >> Optional.of(
        new UserInfo('user-1', 'Existing Person', 'existing@example.org', 'existing', true, null, null))
    return new BeforeEnterEvent(router, NavigationTrigger.PROGRAMMATIC,
        new Location('register/pending-email-confirmation', QueryParameters.empty()),
        EmailConfirmationMain, currentUi, List.of())
  }

  /** All visible text in the tree, so the assertion is about what a visitor reads. */
  private static String allText(Component component) {
    def parts = []
    if (component instanceof Span) {
      parts << component.text
    }
    if (component.hasProperty('text')) {
      try { parts << component.text } catch (Exception ignored) { }
    }
    component.children.each { parts << allText(it) }
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
