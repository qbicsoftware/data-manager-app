package life.qbic.datamanager.security.mfa

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import life.qbic.identity.application.mfa.GraceEligibility
import life.qbic.identity.application.mfa.MfaLoginGuard
import life.qbic.identity.application.mfa.MfaPolicy
import life.qbic.identity.domain.model.mfa.MfaPolicyMode
import life.qbic.identity.domain.model.mfa.ProtectedSecret
import life.qbic.identity.domain.model.mfa.SecondFactor
import life.qbic.identity.domain.repository.MfaGraceRepository
import life.qbic.identity.domain.repository.SecondFactorRepository
import life.qbic.projectmanagement.application.authorization.QbicUserDetails
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.savedrequest.RequestCache
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * <b>Regression: abandoning a challenge must not authenticate the next account (MFA-25)</b>
 *
 * <p>The field report is a full identity substitution, and it is worth stating in the terms a reader
 * will recognise:</p>
 *
 * <ol>
 *   <li>Account A has a factor. The visitor types A's email and password and is sent to the challenge.</li>
 *   <li>At the challenge the visitor chooses "Use a different account", which is a plain link to
 *       {@code /login}.</li>
 *   <li>Account B has no factor. The visitor types B's email and password.</li>
 *   <li>The challenge appears again — with <em>A's</em> pending login still in the session — and a code
 *       from A's authenticator signs the visitor in as A.</li>
 * </ol>
 *
 * <p>Step 3's security property is the one that matters: a successful password authentication for B must
 * leave a session whose pending login names <em>B</em>, or none at all. It must never leave A's. The
 * cause is that {@link MfaLoginSuccessHandler} only <em>adds</em> its pending login: for an account that
 * needs no factor it takes the {@code default} branch, stores the session marker, and never looks at what
 * the previous half-finished login left behind.</p>
 *
 * <p>These are regression tests in the strict sense — they fail on the code as tested and pass when the
 * handler clears stale state. They are deliberately written against the handler's observable effect on
 * the session (the attributes it sets and removes), not against a particular implementation, so a fix
 * that clears the stale state somewhere else still satisfies them.</p>
 *
 * @see MfaLoginSuccessHandler
 */
class MfaAccountSwitchSpec extends Specification {

  static final String CONTEXT_PATH = "/app"
  static final String ACCOUNT_A = "u-a"
  static final String EMAIL_A = "alice@example.org"
  static final String ACCOUNT_B = "u-b"
  static final String EMAIL_B = "bob@example.org"

  static final Duration GRACE = Duration.ofHours(48)
  static final ProtectedSecret SECRET =
      ProtectedSecret.ofBase64(Base64.encoder.encodeToString(new byte[32]))

  FakeFactors factors = new FakeFactors()
  FakeGraces graces = new FakeGraces()
  RequestCache requestCache = Mock()
  AuthenticationSuccessHandler delegate = Mock()

  /** A real session, because the whole defect lives in what survives in it between two logins. */
  Map<String, Object> sessionAttributes = [:]

  HttpServletRequest request
  HttpServletResponse response
  HttpSession session

  def setup() {
    SecurityContextHolder.clearContext()
    sessionAttributes.clear()

    session = Mock()
    session.getAttribute(_) >> { String name -> sessionAttributes[name] }
    session.setAttribute(_, _) >> { String name, Object value -> sessionAttributes[name] = value }
    session.removeAttribute(_) >> { String name -> sessionAttributes.remove(name) }

    request = Mock()
    response = Mock()
    request.getContextPath() >> CONTEXT_PATH
    request.getSession() >> session
    request.getSession(false) >> session

    // Account A has an enrolled factor, account B has none: that is the pair from the report.
    factors.save(SecondFactor.enrolTotp(ACCOUNT_A, SECRET, "Alice's phone", Instant.now()))
  }

  def cleanup() {
    SecurityContextHolder.clearContext()
  }

  // ------------------------------------------------------------------ the reported sequence

  def "challenging account A leaves only A's pending login in the session"() {
    when: "A signs in with the correct password"
    handlerFor(MfaPolicyMode.OPTIONAL)
        .onAuthenticationSuccess(request, response, authentication(ACCOUNT_A, EMAIL_A))

    then: "the session holds A's pending login and nothing that authenticates"
    pendingLoginOf() == ACCOUNT_A
    sessionAttributes[HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY] == null
    SecurityContextHolder.context.authentication == null
  }

  def "after A is challenged, signing in as the factor-free account B does not inherit A"() {
    given: "A was challenged and the visitor went back to the login page"
    def handler = handlerFor(MfaPolicyMode.OPTIONAL)
    handler.onAuthenticationSuccess(request, response, authentication(ACCOUNT_A, EMAIL_A))
    assert pendingLoginOf() == ACCOUNT_A

    when: "B signs in with the correct password and needs no factor"
    handler.onAuthenticationSuccess(request, response, authentication(ACCOUNT_B, EMAIL_B))

    then: "B's login proceeds — B has no factor, so a challenge here would lock B out"
    1 * delegate.onAuthenticationSuccess(request, response, _)

    and: "no pending login naming A survives: this is the identity substitution"
    pendingLoginOf() == null

    and: "the session carries B's answer, not A's"
    sessionStateOf() == ACCOUNT_B
  }

  def "a pending login for A cannot be completed with B's identity afterwards"() {
    given: "A is mid-challenge"
    def handler = handlerFor(MfaPolicyMode.OPTIONAL)
    handler.onAuthenticationSuccess(request, response, authentication(ACCOUNT_A, EMAIL_A))

    and: "B's login replaced whatever the session held, or cleared it"
    handler.onAuthenticationSuccess(request, response, authentication(ACCOUNT_B, EMAIL_B))

    expect: "there is nothing left that would let a code from A's phone establish a session"
    MfaPendingLogin.from(session).isEmpty()
  }

  // ------------------------------------------------------------------ the same defect, other entries

  def "re-authenticating as the SAME account replaces the stale pending login rather than reusing it"() {
    given: "A was challenged, then typed the password again because the first code did not work"
    def handler = handlerFor(MfaPolicyMode.OPTIONAL)
    handler.onAuthenticationSuccess(request, response, authentication(ACCOUNT_A, EMAIL_A))
    def first = MfaPendingLogin.from(session).orElseThrow()

    when: "the password is presented a second time"
    handler.onAuthenticationSuccess(request, response, authentication(ACCOUNT_A, EMAIL_A))

    then: "the pending login is refreshed, so the five-minute lifetime starts again at this login"
    def second = MfaPendingLogin.from(session).orElseThrow()
    !second.createdAt().isBefore(first.createdAt())
    second.userId() == ACCOUNT_A
  }

  def "an account switch under REQUIRED with a stale grace window still ends as B"() {
    given: "A is enrolled and mid-challenge under REQUIRED"
    def handler = handlerFor(MfaPolicyMode.REQUIRED)
    handler.onAuthenticationSuccess(request, response, authentication(ACCOUNT_A, EMAIL_A))

    and: "B is owed a grace window, so the policy lets B in without a factor"
    graces.stored[ACCOUNT_B] = life.qbic.identity.domain.model.mfa.MfaGrace.start(
        ACCOUNT_B, Instant.now().minus(Duration.ofHours(1)), GRACE)

    when:
    handler.onAuthenticationSuccess(request, response, authentication(ACCOUNT_B, EMAIL_B))

    then:
    1 * delegate.onAuthenticationSuccess(request, response, _)
    pendingLoginOf() == null
    sessionStateOf() == ACCOUNT_B
  }

  def "an account switch under OFF is harmless because no pending login was ever created"() {
    given: "the policy is OFF, so even an enrolled account gets a session immediately"
    def handler = handlerFor(MfaPolicyMode.OFF)

    when: "A signs in, then B signs in without logging out"
    handler.onAuthenticationSuccess(request, response, authentication(ACCOUNT_A, EMAIL_A))
    handler.onAuthenticationSuccess(request, response, authentication(ACCOUNT_B, EMAIL_B))

    then: "the normal path runs both times and no pending login exists at any point"
    2 * delegate.onAuthenticationSuccess(request, response, _)
    pendingLoginOf() == null
  }

  // ------------------------------------------------------------------ helpers

  private MfaLoginSuccessHandler handlerFor(MfaPolicyMode mode) {
    def guard = new MfaLoginGuard(factors, graces, new MfaPolicy(mode.name(), GRACE, ""),
        { String ignored -> false } as GraceEligibility)
    return new MfaLoginSuccessHandler(guard, requestCache, delegate)
  }

  private Authentication authentication(String userId, String email) {
    def user = new life.qbic.projectmanagement.application.authorization.User(
        userId, "Test User", userId, email, "hash", true, null, null)
    def details = new QbicUserDetails(user, [])
    return UsernamePasswordAuthenticationToken.authenticated(details, null, [])
  }

  /** @return the user id a pending login names, or {@code null} */
  private String pendingLoginOf() {
    return MfaPendingLogin.from(session).map { it.userId() }.orElse(null)
  }

  /** @return the user id the session marker was issued for, or {@code null} */
  private String sessionStateOf() {
    def state = sessionAttributes[MfaSessionState.SESSION_ATTRIBUTE]
    return state instanceof MfaSessionState ? state.userId() : null
  }

  static class FakeFactors implements SecondFactorRepository {
    final List<SecondFactor> stored = []

    @Override
    SecondFactor save(SecondFactor factor) {
      stored << factor
      return factor
    }

    @Override
    Optional<SecondFactor> findById(long id) {
      Optional.empty()
    }

    @Override
    List<SecondFactor> findAllByUserId(String userId) {
      stored.findAll { it.userId() == userId }
    }

    @Override
    List<SecondFactor> findActiveByUserId(String userId) {
      stored.findAll { it.userId() == userId && it.isActive() }
    }

    @Override
    boolean hasActiveFactor(String userId) {
      !findActiveByUserId(userId).isEmpty()
    }
  }

  static class FakeGraces implements MfaGraceRepository {
    final Map<String, life.qbic.identity.domain.model.mfa.MfaGrace> stored = [:]

    @Override
    Optional<life.qbic.identity.domain.model.mfa.MfaGrace> findByUserId(String userId) {
      Optional.ofNullable(stored[userId])
    }

    @Override
    life.qbic.identity.domain.model.mfa.MfaGrace startIfAbsent(
        life.qbic.identity.domain.model.mfa.MfaGrace candidate) {
      if (!stored.containsKey(candidate.userId())) {
        stored[candidate.userId()] = candidate
      }
      return stored[candidate.userId()]
    }

    @Override
    List<life.qbic.identity.domain.model.mfa.MfaGrace> findActiveWithOutstandingReminder(
        Instant now) {
      stored.values().findAll { it.graceUntil().isAfter(now) }
    }

    @Override
    void markDueReminderSent(String userId, Instant now) {
    }

    @Override
    void markFinalReminderSent(String userId, Instant now) {
    }
  }
}
