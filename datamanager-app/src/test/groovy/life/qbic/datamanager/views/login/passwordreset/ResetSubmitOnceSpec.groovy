package life.qbic.datamanager.views.login.passwordreset

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.Router
import com.vaadin.flow.server.RouteRegistry
import com.vaadin.flow.server.VaadinService
import life.qbic.identity.api.UserInformationService
import life.qbic.identity.application.throttle.AuthAttemptThrottle
import life.qbic.identity.application.user.IdentityService
import life.qbic.identity.domain.repository.AuthAttemptCounter
import life.qbic.identity.domain.repository.UserDataStorage
import life.qbic.identity.domain.repository.UserRepository
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * <b>One submit must produce exactly one throttled attempt, no matter how often the view was built.</b>
 *
 * <p>This is the regression guard for a defect that doubled the throttle counter and reached a running
 * server, where it was reported as "counter jumps to 3 instead of 2".</p>
 *
 * <p>{@code ResetPasswordComponent} is a {@code @UIScope} bean, so one instance is reused across every
 * {@code ResetPasswordMain} constructed in the same UI. The view constructor registered its handler
 * with {@code addListener(...)}, which <em>stacks</em>: after the view had been constructed twice, a
 * single submit fired the handler twice and the counter advanced by two.</p>
 *
 * <p>The consequence was not only a wrong number: with {@code resend.max-attempts=3} the limit was
 * reached after roughly half the intended number of requests, so the user was throttled earlier than
 * the configuration promised.</p>
 */
class ResetSubmitOnceSpec extends Specification {

  static final String ADDRESS = 'alice@example.org'

  IdentityService identityService
  ResetPasswordComponent component
  ResetEmailSentComponent sentComponent
  CountingCounter counter
  AuthAttemptThrottle throttle

  def setup() {
    def registry = Stub(RouteRegistry)
    registry.getTargetUrl(life.qbic.datamanager.views.login.LoginLayout, _ as RouteParameters) >> Optional.of('login')
    registry.getTargetUrl(life.qbic.datamanager.views.register.UserRegistrationMain,
        _ as RouteParameters) >> Optional.of('registration')
    def service = Stub(VaadinService)
    service.getRouter() >> new Router(registry)
    service.getContext() >> null
    VaadinService.setCurrent(service)
    // ResetPasswordMain registers a login-button handler that navigates through UI.getCurrent().
    UI.setCurrent(Mock(UI))

    def storage = Stub(UserDataStorage)
    storage.findUsersByEmailAddress(_) >> []
    identityService = new IdentityService(new UserRepository(storage))

    // ONE component instance, shared across view constructions — exactly what @UIScope does.
    component = new ResetPasswordComponent(Stub(UserInformationService))
    sentComponent = new ResetEmailSentComponent()
    counter = new CountingCounter()
    throttle = new AuthAttemptThrottle(counter,
        10, Duration.ofMinutes(15), Duration.ofMinutes(15),
        3, Duration.ofMinutes(15), Duration.ofMinutes(15))
  }

  def cleanup() {
    VaadinService.setCurrent(null)
    UI.setCurrent(null)
  }

  def "one submit advances the counter by exactly one"() {
    given:
    new ResetPasswordMain(identityService, component, sentComponent, throttle)

    when:
    submit(ADDRESS)

    then:
    counter.increments == 1
  }

  def "a submit after the view was constructed again still advances by exactly one"() {
    // The bug: the second construction stacked a second listener, so this submit counted twice and the
    // persisted count went 1 -> 3.
    given: 'the view has been built once and submitted once'
    new ResetPasswordMain(identityService, component, sentComponent, throttle)
    submit(ADDRESS)
    def afterFirst = counter.increments

    when: 'the visitor navigates away and back, so a NEW view is built over the SAME component'
    new ResetPasswordMain(identityService, component, sentComponent, throttle)
    submit(ADDRESS)

    then: 'exactly one more increment, not two'
    counter.increments == afterFirst + 1

    and: 'so the persisted count is 2, not 3'
    counter.countFor(AuthAttemptThrottle.Scope.PASSWORD_RESET_RESEND, ADDRESS) == 2
  }

  def "N view constructions, N submits, N increments"() {
    given:
    (1..5).each {
      new ResetPasswordMain(identityService, component, sentComponent, throttle)
      submit(ADDRESS)
    }

    expect: 'the counter tracks the number of submits, not their triangular sum'
    counter.countFor(AuthAttemptThrottle.Scope.PASSWORD_RESET_RESEND, ADDRESS) == 5
  }

  def "the login-button handler is also not stacked by repeated view construction"() {
    // Same @UIScope trap in ResetEmailSentComponent. The contract is "last registration wins", so a
    // stacked registration would run the handler N times for N page loads instead of once.
    given: 'the view has been constructed three times over the SAME @UIScope component'
    new ResetPasswordMain(identityService, component, sentComponent, throttle)
    new ResetPasswordMain(identityService, component, sentComponent, throttle)
    new ResetPasswordMain(identityService, component, sentComponent, throttle)

    and: 'we take over the handler to observe how often it fires'
    int navigations = 0
    sentComponent.addLoginButtonListener({ navigations++ })

    when: 'the button is clicked once'
    clickLogin(sentComponent)

    then: 'exactly one handler runs'
    navigations == 1
  }

  // ---- helpers -------------------------------------------------------------------------------

  private com.vaadin.flow.component.button.Button findButton(component, String text) {
    if (component instanceof com.vaadin.flow.component.button.Button && component.text == text) {
      return component
    }
    for (child in component.children) {
      def found = findButton(child, text)
      if (found != null) {
        return found
      }
    }
    return null
  }

  private void submit(String email) {
    component.fireEvent(new ResetPasswordComponent.ResetPasswordEvent(component, true, email))
  }

  private void clickLogin(ResetEmailSentComponent component) {
    def button = findButton(component, 'Login')
    button.fireEvent(new com.vaadin.flow.component.ClickEvent(button))
  }

  static final class CountingCounter implements AuthAttemptCounter {

    private final Map<String, int[]> counts = [:]
    int increments = 0

    @Override
    AuthAttemptCounter.Counter increment(String scope, String key, Instant now, Duration window) {
      increments++
      String id = scope + '|' + key
      counts.computeIfAbsent(id) { new int[1] }[0]++
      new AuthAttemptCounter.Counter(counts[id][0], null)
    }

    @Override
    void block(String scope, String key, Instant blockedUntil) { }

    @Override
    AuthAttemptCounter.Counter peek(String scope, String key) {
      def c = counts[scope + '|' + key]
      new AuthAttemptCounter.Counter(c == null ? 0 : c[0], null)
    }

    @Override
    void clear(String scope, String key) { counts.remove(scope + '|' + key) }

    int countFor(AuthAttemptThrottle.Scope scope, String identifier) {
      def key = AuthAttemptThrottle.attemptKey(scope, identifier)
      def c = counts[scope.name() + '|' + key]
      return c == null ? 0 : c[0]
    }
  }
}
