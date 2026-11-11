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
import life.qbic.projectmanagement.application.authorization.QbicUserDetails
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
  static final String USER_ID = '3f3e2b1a-0000-4000-8000-000000000000'

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

  def "a successful password login clears the bucket that the FAILURES wrote to"() {
    // This is the accounting the success path exists for: fumble the password a few times, then get
    // it right, and the allowance must be whole again.
    //
    // It previously was not. The listener used authentication.getName() for both paths, but that
    // returns the principal's getUsername() — and QbicUserDetails.getUsername() returns the USER ID,
    // because ACL records are keyed on it. So on success the listener cleared hash("LOGIN\0<uuid>"),
    // a bucket nobody had written to, and the real email-keyed counter was left untouched.
    given: 'two failures keyed on the email, as the failure path does'
    2.times { throttle.record(Scope.LOGIN, ADDRESS, NOW) }
    def listener = new AuthAttemptEventListener(throttle)

    when: 'the same person logs in successfully'
    listener.onSuccess(successEvent())

    then: 'the EMAIL-keyed bucket is empty, so the allowance is whole again'
    counter.bucketCountFor(Scope.LOGIN, ADDRESS) == 0

    and: 'and nothing was written to a bucket keyed on the user id'
    counter.bucketCountFor(Scope.LOGIN, USER_ID) == 0
  }

  def "the success path reads the email from the principal, not getUsername()"() {
    // Pins the mechanism directly: a QbicUserDetails whose getUsername() is the user id and whose
    // getEmailAddress() is the address. If the listener ever goes back to getName(), this fails.
    given:
    def listener = new AuthAttemptEventListener(throttle)

    when:
    listener.onSuccess(successEvent())

    then: 'the email bucket was cleared, which is only possible by reading getEmailAddress()'
    counter.clearedKeys.contains(
        AuthAttemptThrottle.attemptKey(Scope.LOGIN, ADDRESS))

    and: 'the user-id bucket was never touched'
    !counter.clearedKeys.contains(
        AuthAttemptThrottle.attemptKey(Scope.LOGIN, USER_ID))
  }

  def "an interactive (form-login) success also clears the email-keyed bucket"() {
    // In Spring Security 7 InteractiveAuthenticationSuccessEvent is a SIBLING of
    // AuthenticationSuccessEvent, not a subclass — so a single handler cannot catch both. Missing
    // this one would leave every form-login user accumulating a counter they never clear.
    given:
    def listener = new AuthAttemptEventListener(throttle)
    2.times { throttle.record(Scope.LOGIN, ADDRESS, NOW) }

    when:
    listener.onInteractiveSuccess(
        new InteractiveAuthenticationSuccessEvent(authenticatedPrincipal(), Object))

    then:
    counter.bucketCountFor(Scope.LOGIN, ADDRESS) == 0
  }

  def "a failure is keyed on the submitted email, which is what the filter checks"() {
    // The pre-check in LoginThrottleFilter reads request.getParameter("username") — the raw address.
    // The failure listener must key on the same value, or an attacker's failures would never reach
    // the threshold the filter consults.
    given:
    def listener = new AuthAttemptEventListener(throttle)

    when:
    listener.onFailure(failureEvent())

    then:
    counter.bucketCountFor(Scope.LOGIN, ADDRESS) == 1
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

  /** A failure-shaped authentication: principal is the raw submitted string. */
  private Authentication auth() {
    def authentication = Stub(Authentication)
    authentication.getName() >> ADDRESS
    return authentication
  }

  /**
   * A success-shaped authentication, faithful to production.
   *
   * <p>{@code getName()} returns the USER ID, because it delegates to
   * {@link QbicUserDetails#getUsername()} and that returns the id (ACL records are keyed on it). The
   * email is only reachable through {@code getEmailAddress()}. A stub that returned the email from
   * {@code getName()} would hide the very bug these specs exist to catch.</p>
   */
  private Authentication authenticatedPrincipal() {
    def user = new life.qbic.projectmanagement.application.authorization.User(
        USER_ID, 'Alice Example', 'alice', ADDRESS, 'hash', true, null, null)
    def details = new QbicUserDetails(user, [])
    def authentication = Stub(Authentication)
    authentication.getPrincipal() >> details
    authentication.getName() >> details.getUsername()   // the user id
    return authentication
  }

  private AbstractAuthenticationFailureEvent failureEvent() {
    def authentication = auth()
    return new AbstractAuthenticationFailureEvent(authentication, new BadCredentialsException('Bad credentials')) {
    }
  }

  private AuthenticationSuccessEvent successEvent() {
    return new AuthenticationSuccessEvent(authenticatedPrincipal())
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
    AuthAttemptCounter.Counter increment(String scope, String key, Instant now, Duration window) {
      increments++
      String id = bucketId(scope, key)
      counts.computeIfAbsent(id) { new int[1] }[0]++
      return new AuthAttemptCounter.Counter(counts[id][0], blocks[id])
    }

    @Override
    void block(String scope, String key, Instant blockedUntil) {
      blocks[bucketId(scope, key)] = blockedUntil
    }

    @Override
    AuthAttemptCounter.Counter peek(String scope, String key) {
      String id = bucketId(scope, key)
      new AuthAttemptCounter.Counter(counts[id] == null ? 0 : counts[id][0], blocks[id])
    }

    final Set<String> clearedKeys = []

    @Override
    void clear(String scope, String key) {
      clearedKeys << key
      counts.remove(bucketId(scope, key))
      blocks.remove(bucketId(scope, key))
    }

    int totalIncrements() {
      return increments
    }

    /** How many attempts are counted against this identifier in this scope. */
    int bucketCountFor(Scope scope, String identifier) {
      // `.toString()` is not cosmetic: a GString is not equal to a String in Groovy, so an inline
      // "$a|$b" key silently misses entries inserted under a coerced String key.
      String id = bucketId(scope.name(), AuthAttemptThrottle.attemptKey(scope, identifier))
      return counts[id] == null ? 0 : counts[id][0]
    }

    private static String bucketId(String scope, String key) {
      return scope + '|' + key
    }
  }
}
