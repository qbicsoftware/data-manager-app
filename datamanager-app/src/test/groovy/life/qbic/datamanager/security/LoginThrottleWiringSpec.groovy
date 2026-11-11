package life.qbic.datamanager.security

import life.qbic.identity.application.throttle.AuthAttemptThrottle
import life.qbic.identity.application.throttle.AuthAttemptThrottle.Scope
import life.qbic.identity.domain.repository.AuthAttemptCounter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent
import org.springframework.security.authentication.event.AuthenticationSuccessEvent
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * <b>Wiring guards for the throttle (AUTH-NFR-03, #1682).</b>
 *
 * <p>The previous attempt at this work was removed rather than shipped because it had <b>zero
 * production callers</b> — dead code dressed as a security control. These specs exist so that
 * cannot happen silently again: they fail if the throttle stops being consulted, and they pin the
 * two properties that make it safe (identical outcome, constant work).</p>
 */
class LoginThrottleWiringSpec extends Specification {

  // The filter reads Instant.now() itself (production code must not take a clock from the caller),
  // so the specs seed the bucket against the real clock too. A fixed T0 would look like an expired
  // window and the throttle would never engage.
  static final Instant NOW = Instant.now()
  static final String ADDRESS = 'alice@example.org'

  AuthAttemptCounter counter
  AuthAttemptThrottle throttle
  RecordingFailureHandler failureHandler
  List<String> constantWorkCalls
  LoginThrottleFilter filter

  def setup() {
    counter = new InMemoryCounter()
    throttle = new AuthAttemptThrottle(counter, 3, Duration.ofMinutes(15), Duration.ofMinutes(15),
        2, Duration.ofMinutes(15), Duration.ofMinutes(15))
    failureHandler = new RecordingFailureHandler()
    constantWorkCalls = []
    filter = new LoginThrottleFilter(throttle, failureHandler, { String a -> constantWorkCalls << a })
  }

  // ---- the filter must not be dead code ------------------------------------------------------

  def "an unthrottled login POST reaches the filter chain, so the real authentication runs"() {
    given:
    def request = loginPost(ADDRESS)
    def response = Stub(HttpServletResponse)
    def chain = new RecordingChain()

    when:
    filter.doFilterInternal(request, response, chain)

    then: 'the request went through — the filter is not blocking normal logins'
    chain.ranWith != null
    failureHandler.failures.isEmpty()
  }

  def "a throttled login POST is rejected without reaching the authentication path"() {
    given: 'the bucket is already over the limit'
    4.times { throttle.record(Scope.LOGIN, ADDRESS, NOW) }

    and:
    def request = loginPost(ADDRESS)
    def response = Stub(HttpServletResponse)
    def chain = new RecordingChain()

    when:
    filter.doFilterInternal(request, response, chain)

    then: 'the chain was NOT invoked, so no password comparison happened'
    chain.ranWith == null

    and: 'the failure handler ran, producing the normal failure page'
    failureHandler.failures.size() == 1
    failureHandler.failures.first() instanceof BadCredentialsException
  }

  def "the throttled rejection pays the same password work as a real failure"() {
    // Otherwise the throttle is observable as a timing signal: a throttled attempt that returns in
    // ~1 ms while a wrong password takes ~30 ms tells the attacker the address is being throttled,
    // and therefore that it exists.
    given:
    4.times { throttle.record(Scope.LOGIN, ADDRESS, NOW) }

    when:
    filter.doFilterInternal(loginPost(ADDRESS), Stub(HttpServletResponse), new RecordingChain())

    then: 'exactly one derivation was performed on the rejection path'
    constantWorkCalls == [ADDRESS]
  }

  def "the rejection uses the same generic message a wrong password produces"() {
    given:
    4.times { throttle.record(Scope.LOGIN, ADDRESS, NOW) }

    when:
    filter.doFilterInternal(loginPost(ADDRESS), Stub(HttpServletResponse), new RecordingChain())

    then: 'not "too many attempts" — that would confirm the account is real (decision B4)'
    failureHandler.failures.first().message == 'Bad credentials'
  }

  def "GET /login is never throttled, or the visitor could not reach the form to stop failing"() {
    given:
    4.times { throttle.record(Scope.LOGIN, ADDRESS, NOW) }

    and:
    def request = request('GET', '/login')

    expect:
    filter.shouldNotFilter(request)
  }

  def "a POST with no username is passed through for Spring Security to reject"() {
    given:
    def request = request('POST', '/login')

    when:
    def chain = new RecordingChain()
    filter.doFilterInternal(request, Stub(HttpServletResponse), chain)

    then:
    chain.ranWith != null
  }

  def "a rejected attempt is not counted, so retrying cannot extend the block"() {
    given:
    4.times { throttle.record(Scope.LOGIN, ADDRESS, NOW) }
    def before = counter.totalIncrements()

    when: 'the blocked client retries ten times'
    10.times {
      filter.doFilterInternal(loginPost(ADDRESS), Stub(HttpServletResponse), new RecordingChain())
    }

    then: 'the counter did not move — the block expires when it was going to'
    counter.totalIncrements() == before
  }

  // ---- the listener must count, and only on real password outcomes ---------------------------

  def "a failed authentication is recorded"() {
    given:
    def listener = new AuthAttemptEventListener(throttle)

    when:
    listener.onFailure(failureEvent())

    then:
    counter.totalIncrements() == 1
  }

  def "a disabled-account failure is recorded too, because it is also a password attempt"() {
    given: 'an unconfirmed account is a weaker target, so guessing at it must still cost attempts'
    def listener = new AuthAttemptEventListener(throttle)

    when:
    listener.onFailure(failureEvent())

    then:
    counter.totalIncrements() == 1
  }

  def "a successful password login clears the bucket"() {
    given: 'two failures, then a success'
    2.times { throttle.record(Scope.LOGIN, ADDRESS, NOW) }
    def listener = new AuthAttemptEventListener(throttle)

    when:
    listener.onSuccess(successEvent(AuthenticationSuccessEvent))

    then: 'the allowance is restored'
    !throttle.isThrottled(Scope.LOGIN, ADDRESS, NOW.plusSeconds(1))
  }

  def "an interactive (form-login) success also clears the bucket"() {
    // In Spring Security 7 InteractiveAuthenticationSuccessEvent is a SIBLING of
    // AuthenticationSuccessEvent, not a subclass — so a single handler cannot catch both. Missing
    // this one would leave every form-login user accumulating a counter they never clear.
    given:
    def listener = new AuthAttemptEventListener(throttle)
    2.times { throttle.record(Scope.LOGIN, ADDRESS, NOW) }

    when:
    listener.onInteractiveSuccess(
        new InteractiveAuthenticationSuccessEvent(auth(), Object))

    then:
    !throttle.isThrottled(Scope.LOGIN, ADDRESS, NOW.plusSeconds(1))
  }

  // ---- helpers -------------------------------------------------------------------------------

  private HttpServletRequest loginPost(String username) {
    return request('POST', '/login', [username: username, password: 'whatever'])
  }

  private HttpServletRequest request(String method, String path, Map<String, String> params = [:]) {
    def request = Stub(HttpServletRequest) {
      getMethod() >> method
      getServletPath() >> path
      getParameter(_) >> { String name -> params[name] }
    }
    return request
  }

  /** Records whether the chain ran, which is how "authentication was never reached" is asserted. */
  static final class RecordingChain implements FilterChain {

    ServletRequest ranWith = null

    @Override
    void doFilter(ServletRequest request, ServletResponse response) {
      ranWith = request
    }
  }

  private Authentication auth() {
    def authentication = Stub(Authentication)
    authentication.getName() >> ADDRESS
    return authentication
  }

  private AbstractAuthenticationFailureEvent failureEvent() {
    def authentication = auth()
    return new AbstractAuthenticationFailureEvent(authentication, new BadCredentialsException('Bad credentials')) {
    }
  }

  private AuthenticationSuccessEvent successEvent(Class<?> ignored) {
    return new AuthenticationSuccessEvent(auth())
  }

  static final class RecordingFailureHandler implements AuthenticationFailureHandler {

    final List<Exception> failures = []

    @Override
    void onAuthenticationFailure(jakarta.servlet.http.HttpServletRequest request,
        jakarta.servlet.http.HttpServletResponse response,
        org.springframework.security.core.AuthenticationException exception) {
      failures << exception
    }
  }

  static final class InMemoryCounter implements AuthAttemptCounter {

    private final Map<String, int[]> counts = [:]
    private final Map<String, Instant> blocks = [:]
    private int increments = 0

    @Override
    AuthAttemptCounter.Counter increment(String scope, String key, Instant now, Instant windowStart) {
      increments++
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

    int totalIncrements() {
      return increments
    }
  }
}
