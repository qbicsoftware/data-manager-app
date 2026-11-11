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

  def "a successful resend confirms the email was sent"() {
    given:
    def view = newView()
    view.setParameter(event(), USER_ID)

    when:
    clickResend(view)

    then:
    def text = renderedText(view)
    text.toLowerCase().contains('sent')
    text.toLowerCase().contains('check your inbox')
  }

  def "a throttled resend tells the visitor to wait, instead of silently doing nothing"() {
    // This is the behaviour that was wrong first time round.
    //
    // The page is only reachable after the visitor proved they know the user id (UserRegistrationMain
    // navigates here with a real id straight after registering), so everyone here has ALREADY
    // demonstrated that the account exists. There is no enumeration oracle to protect, and staying
    // silent meant telling the visitor "an email was sent" while no email was sent — which reads as a
    // broken mail server and lands on the helpdesk.
    given:
    def view = newView()
    view.setParameter(event(), USER_ID)

    when: 'the first resend goes out'
    clickResend(view)

    and: 'the second one is throttled'
    clickResend(view)

    then: 'the visitor is told, in plain words, that they must wait'
    def text = renderedText(view).toLowerCase()
    text.contains('already sent')
    text.contains('can be requested in')

    and: 'the cooldown is stated as a human duration, not a raw ISO-8601 value'
    text ==~ /(?s).*\d+ minutes.*/
    !text.contains('pt15m')

    and: 'and it does not pretend the link is lost — the previous one is still valid'
    text.contains('still valid')
  }

  def "the throttle copy derives the duration from configuration, not a hard-coded 15"() {
    // The block duration is deployment configuration. Copy that hard-codes "15 minutes" would start
    // lying the moment an operator tunes AUTH_THROTTLE_RESEND_BLOCK_DURATION.
    expect:
    EmailConfirmationMain.cooldownText(java.time.Duration.ofMinutes(5)) == '5 minutes'
    EmailConfirmationMain.cooldownText(java.time.Duration.ofMinutes(15)) == '15 minutes'
    EmailConfirmationMain.cooldownText(java.time.Duration.ofMinutes(1)) == '1 minute'
    EmailConfirmationMain.cooldownText(java.time.Duration.ofHours(1)) == '1 hour'
    EmailConfirmationMain.cooldownText(java.time.Duration.ofHours(2)) == '2 hours'
    EmailConfirmationMain.cooldownText(java.time.Duration.ofSeconds(30)) == 'a moment'
  }

  def "a throttled resend does not send a second email"() {
    given:
    def view = newView()
    view.setParameter(event(), USER_ID)

    when:
    clickResend(view)
    clickResend(view)

    then: 'the resend limit is 1 in this spec'
    1 * emailService.send(_, _, _)
  }

  def "the page rendered without a user id does not throw when Resend is clicked"() {
    // The route uses @OptionalParameter, so /register/pending-email-confirmation is a valid URL that
    // renders this page with no userInfo. The click handler dereferenced it unconditionally and
    // produced a NullPointerException -> HTTP 500.
    given: 'no parameter was supplied, so setParameter never assigned a user'
    def view = newView()

    when:
    clickResend(view)

    then: 'no exception escapes'
    noExceptionThrown()

    and: 'the visitor is told what to do instead'
    def text = renderedText(view).toLowerCase()
    text.contains('cannot resend')
    text.contains('registration email')

    and: 'nothing was sent'
    0 * emailService.send(_, _, _)
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
