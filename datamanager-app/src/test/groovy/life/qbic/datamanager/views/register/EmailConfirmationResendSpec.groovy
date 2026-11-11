package life.qbic.datamanager.views.register

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.Router
import com.vaadin.flow.server.RouteRegistry
import com.vaadin.flow.server.VaadinService
import life.qbic.identity.api.UserInfo
import life.qbic.identity.api.UserInformationService
import life.qbic.identity.api.VerificationLinkService
import life.qbic.identity.application.communication.EmailService
import life.qbic.identity.application.throttle.AuthAttemptThrottle
import life.qbic.identity.application.throttle.AuthAttemptThrottle.Scope
import life.qbic.identity.application.user.policy.EmailConfirmationLinkSupplier
import life.qbic.identity.domain.repository.AuthAttemptCounter
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * <b>The resend button must be throttled server-side, and must not say so (AUTH-NFR-03, #1682).</b>
 *
 * <p>It used to be guarded by a 30-second {@code java.util.Timer} over a field in this UI-scoped
 * view: reloading the page reset the counter to zero, so it was not a control at all. This spec pins
 * the two things that replaced it — a server-side bucket that survives a reload, and one message for
 * both outcomes.</p>
 *
 * <p>Why one message: a distinct "too many requests" panel would confirm to an attacker that the
 * address is real and that the throttle is keyed on it, which is the enumeration oracle the whole
 * requirement exists to close (decision B4).</p>
 */
class EmailConfirmationResendSpec extends Specification {

  static final String USER_ID = '3f3e2b1a-0000-4000-8000-000000000000'
  static final String ADDRESS = 'alice@example.org'

  UserInformationService userInformationService
  EmailService emailService
  EmailConfirmationLinkSupplier linkSupplier
  VerificationLinkService verificationLinkService
  InMemoryCounter counter
  AuthAttemptThrottle throttle

  Router router
  UI currentUi

  def setup() {
    // The view builds a RouterLink to the login page, whose href resolves through the router at
    // construction time (same scaffolding as LoginLayoutSpec / ResetPasswordMainSpec).
    def registry = Stub(RouteRegistry)
    registry.getTargetUrl(life.qbic.datamanager.views.login.LoginLayout,
        _ as RouteParameters) >> Optional.of('login')
    router = new Router(registry)
    def service = Stub(VaadinService)
    service.getRouter() >> router
    service.getContext() >> null
    VaadinService.setCurrent(service)

    currentUi = Mock(UI)
    UI.setCurrent(currentUi)

    userInformationService = Stub(UserInformationService) {
      findById(USER_ID) >> Optional.of(
          new UserInfo(USER_ID, 'Alice Example', ADDRESS, 'alice', false, null, null))
    }
    emailService = Mock(EmailService)
    // A real instance rather than a mock: it is a concrete class with no no-arg constructor, and
    // its only job is to format the URL, which the real implementation does correctly.
    linkSupplier = new EmailConfirmationLinkSupplier('https', 'example.org', 443, '',
        '/registration/confirm', 'token')
    verificationLinkService = Stub(VerificationLinkService) {
      issue(_, _, _) >> new VerificationLinkService.Issued('raw-token', Instant.now().plusSeconds(3600))
    }
    counter = new InMemoryCounter()
    throttle = new AuthAttemptThrottle(counter, 10, Duration.ofMinutes(15), Duration.ofMinutes(15),
        1, Duration.ofMinutes(15), Duration.ofMinutes(15))   // resend limit of 1
  }

  def cleanup() {
    VaadinService.setCurrent(null)
    UI.setCurrent(null)
  }

  def "a resend sends exactly one email"() {
    given:
    def view = newView()

    when:
    view.setParameter(event(), USER_ID)
    clickResend(view)

    then:
    1 * emailService.send(_, _, _)
  }

  def "a second resend within the window does not send another email"() {
    given:
    def view = newView()
    view.setParameter(event(), USER_ID)

    when:
    clickResend(view)
    clickResend(view)

    then: 'only the first one went out — the limit is 1'
    1 * emailService.send(_, _, _)
  }

  def "the throttled resend shows the same panel as a successful one"() {
    // The single most important assertion in this spec. If a throttled resend said "too many
    // requests", the button would become an account-existence oracle.
    given:
    def view = newView()
    view.setParameter(event(), USER_ID)

    when: 'one resend succeeds'
    clickResend(view)
    def afterFirst = renderedText(view)

    and: 'the next one is throttled'
    clickResend(view)
    def afterSecond = renderedText(view)

    then: 'the visitor cannot tell the difference — identical rendering, character for character'
    afterFirst == afterSecond
    afterFirst.toLowerCase().contains('sent')

    and: 'and neither rendering carries throttle language'
    !afterSecond.toLowerCase().contains('too many')
    !afterSecond.toLowerCase().contains('throttl')
    !afterSecond.toLowerCase().contains('seconds until')
    !afterSecond.toLowerCase().contains('try again later')
  }

  def "the throttle survives a page reload, because it is in the store and not in the view"() {
    given: 'one resend from a first visit'
    def firstVisit = newView()
    firstVisit.setParameter(event(), USER_ID)
    clickResend(firstVisit)

    and: 'the visitor reloads, which creates a brand-new view instance'
    def afterReload = newView()
    afterReload.setParameter(event(), USER_ID)

    when:
    clickResend(afterReload)

    then: 'the reloaded view sends nothing: the bucket is full, and it lives in the store, not the view'
    0 * emailService.send(_, _, _)
  }

  // ---- helpers -------------------------------------------------------------------------------

  private EmailConfirmationMain newView() {
    return new EmailConfirmationMain(userInformationService, emailService, linkSupplier,
        verificationLinkService, throttle)
  }

  private com.vaadin.flow.router.BeforeEvent event() {
    // A real BeforeEnterEvent, not a mock: this classpath has no Objenesis and the Vaadin event
    // classes have no usable no-arg constructor. Same approach as ConfirmEmailMainSpec.
    return new com.vaadin.flow.router.BeforeEnterEvent(router,
        com.vaadin.flow.router.NavigationTrigger.PROGRAMMATIC,
        new com.vaadin.flow.router.Location('register/pending-email-confirmation',
            com.vaadin.flow.router.QueryParameters.empty()),
        EmailConfirmationMain, currentUi, List.of())
  }

  /** Clicks the "Resend verification link" span by firing its registered click listeners. */
  private static void clickResend(EmailConfirmationMain view) {
    def link = findSpan(view, 'Resend verification link')
    // No browser here, so the click is dispatched as the component event Vaadin would produce.
    // addClickListener registers on the component's event bus; a real ClickEvent fired through the
    // bus is what a browser click ultimately does.
    link.fireEvent(new com.vaadin.flow.component.ClickEvent(link))
  }

  private static com.vaadin.flow.component.html.Span findSpan(component, String text) {
    if (component instanceof com.vaadin.flow.component.html.Span
        && component.text == text) {
      return component
    }
    for (child in component.children) {
      def found = findSpan(child, text)
      if (found != null) {
        return found
      }
    }
    return null
  }

  private static String renderedText(component) {
    def sink = []
    collect(component, sink)
    return sink.join(' ').trim()
  }

  private static void collect(component, List<String> sink) {
    if (component instanceof com.vaadin.flow.component.HasText) {
      try {
        def text = component.text
        if (text) {
          sink << text
        }
      } catch (Exception ignored) {
      }
    }
    component.children.each { collect(it, sink) }
  }

  static final class InMemoryCounter implements AuthAttemptCounter {

    private final Map<String, int[]> counts = [:]
    private final Map<String, Instant> blocks = [:]

    @Override
    AuthAttemptCounter.Counter increment(String scope, String key, Instant now, Instant windowStart) {
      String id = "$scope|$key"
      counts.computeIfAbsent(id) { new int[1] }[0]++
      return new AuthAttemptCounter.Counter(counts[id][0], blocks[id])
    }

    @Override
    void block(String scope, String key, Instant blockedUntil) {
      blocks["$scope|$key"] = blockedUntil
    }

    @Override
    AuthAttemptCounter.Counter peek(String scope, String key) {
      String id = "$scope|$key"
      new AuthAttemptCounter.Counter(counts[id] == null ? 0 : counts[id][0], blocks[id])
    }

    @Override
    void clear(String scope, String key) {
      counts.remove("$scope|$key")
      blocks.remove("$scope|$key")
    }
  }
}
