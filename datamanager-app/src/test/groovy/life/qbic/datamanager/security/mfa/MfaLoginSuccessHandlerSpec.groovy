package life.qbic.datamanager.security.mfa

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import life.qbic.identity.application.mfa.MfaLoginGuard
import life.qbic.identity.application.mfa.MfaPolicy
import life.qbic.identity.domain.model.mfa.MfaGrace
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
 * <b>The security property of M4-1: a password must not produce a session when a factor is required.</b>
 *
 * <p>These assertions are the difference between "we redirect to a challenge" and "there is no
 * authenticated session". The second one is what AUTH-R-01 says, and it is why the handler clears the
 * authentication from the session as well as from the thread — Spring saves the context <em>before</em>
 * the success handler runs.</p>
 *
 * <p>The guard is the real one with in-memory stores rather than a mock: the interesting behaviour is
 * the combination (policy + enrolment + grace → handler action), and it is cheap to arrange.</p>
 */
class MfaLoginSuccessHandlerSpec extends Specification {

    static final String CONTEXT_PATH = "/app"

    /**
     * The handler asks the guard with the real clock ({@code Instant.now()}), so the grace windows
     * arranged here must be relative to it too. A hard-coded instant silently rots: this spec used
     * {@code 2026-11-18T09:00:00Z} and its "running grace window" case started failing on
     * 2026-11-20T08:00Z, when that fixed window expired — a green suite that had become a lie about
     * yesterday rather than a statement about the code.
     */
    static final Instant NOW = Instant.now()
    static final Duration GRACE = Duration.ofHours(48)
    static final ProtectedSecret SECRET =
            ProtectedSecret.ofBase64(Base64.encoder.encodeToString(new byte[32]))

    FakeFactors factors = new FakeFactors()
    FakeGraces graces = new FakeGraces()
    RequestCache requestCache = Mock()
    AuthenticationSuccessHandler delegate = Mock()

    HttpServletRequest request
    HttpServletResponse response
    HttpSession session

    def setup() {
        SecurityContextHolder.clearContext()
        session = Mock()
        request = Mock()
        response = Mock()
        request.getContextPath() >> CONTEXT_PATH
        request.getSession() >> session
        request.getSession(false) >> session
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    // ---------------------------------------------------------------- challenge

    def "a required account is challenged and the authentication is withheld"() {
        given: "an enrolled account under OPTIONAL"
        factors.save(SecondFactor.enrolTotp("u-1", SECRET, "iPhone", NOW))

        when:
        handlerFor(MfaPolicyMode.OPTIONAL).onAuthenticationSuccess(request, response, authentication())

        then: "the identity is remembered for the challenge only…"
        1 * session.setAttribute(MfaPendingLogin.SESSION_ATTRIBUTE, { MfaPendingLogin p ->
            p.userId() == "u-1" && p.email() == "ada@example.org"
        })

        and: "…and no authentication survives, neither in the thread nor in the session"
        SecurityContextHolder.context.authentication == null
        1 * session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)

        and: "the user is sent to the challenge, not into the application"
        1 * response.sendRedirect(CONTEXT_PATH + "/login/second-factor")

        and: "the normal success path must not run"
        0 * delegate.onAuthenticationSuccess(_, _, _)
    }

    def "the pending login is remembered with the email, not the id that getUsername() returns"() {
        given: "QbicUserDetails.getUsername() returns the user id; the challenge needs the login name"
        factors.save(SecondFactor.enrolTotp("u-1", SECRET, "iPhone", NOW))

        when:
        handlerFor(MfaPolicyMode.OPTIONAL).onAuthenticationSuccess(request, response, authentication())

        then:
        1 * session.setAttribute(MfaPendingLogin.SESSION_ATTRIBUTE, { MfaPendingLogin p ->
            p.email() == "ada@example.org" && p.email() != "u-1"
        })
        true
    }

    // ---------------------------------------------------------------- no factor needed

    def "when the policy is OFF the login proceeds normally"() {
        given:
        factors.save(SecondFactor.enrolTotp("u-1", SECRET, "iPhone", NOW))

        when:
        handlerFor(MfaPolicyMode.OFF).onAuthenticationSuccess(request, response, authentication())

        then: "the delegate owns the redirect and the request cache, exactly as without MFA"
        1 * delegate.onAuthenticationSuccess(request, response, _)
        0 * session.setAttribute(MfaPendingLogin.SESSION_ATTRIBUTE, _)
        0 * session.removeAttribute(_)
    }

    def "an unenrolled account under OPTIONAL proceeds"() {
        when:
        handlerFor(MfaPolicyMode.OPTIONAL).onAuthenticationSuccess(request, response, authentication())

        then:
        1 * delegate.onAuthenticationSuccess(request, response, _)
    }

    def "a running grace window proceeds with the password"() {
        given:
        graces.stored["u-1"] = MfaGrace.start("u-1", NOW.minus(Duration.ofHours(1)), GRACE)

        when:
        handlerFor(MfaPolicyMode.REQUIRED).onAuthenticationSuccess(request, response, authentication())

        then: "the grace window is exactly the point: no factor yet, but a deadline exists"
        1 * delegate.onAuthenticationSuccess(request, response, _)
        0 * session.setAttribute(MfaPendingLogin.SESSION_ATTRIBUTE, _)
    }

    def "a first login under REQUIRED grants a window, persists it, and lets the user in"() {
        when:
        handlerFor(MfaPolicyMode.REQUIRED).onAuthenticationSuccess(request, response, authentication())

        then:
        1 * delegate.onAuthenticationSuccess(request, response, _)

        and: "the window is anchored at the login and lasts the configured 48 hours"
        def stored = graces.stored["u-1"]
        stored != null
        stored.graceUntil() == stored.startedAt().plus(GRACE)
        stored.graceUntil().isAfter(Instant.now())
    }

    // ---------------------------------------------------------------- denials

    def "an expired grace window denies the login and says why"() {
        given:
        graces.stored["u-1"] = MfaGrace.start("u-1", NOW.minus(Duration.ofDays(10)), GRACE)

        when:
        handlerFor(MfaPolicyMode.REQUIRED).onAuthenticationSuccess(request, response, authentication())

        then: "the password was correct, so naming the cause is not an enumeration oracle"
        1 * response.sendRedirect(CONTEXT_PATH + "/login/second-factor?graceExpired")
        0 * delegate.onAuthenticationSuccess(_, _, _)
        0 * session.setAttribute(MfaPendingLogin.SESSION_ATTRIBUTE, _)
        SecurityContextHolder.context.authentication == null
    }

    def "an unexpected principal fails closed rather than treating the login as factor-free"() {
        when:
        handlerFor(MfaPolicyMode.REQUIRED).onAuthenticationSuccess(request, response,
                new UsernamePasswordAuthenticationToken("someone", "secret"))

        then:
        thrown(IllegalStateException)
        0 * delegate.onAuthenticationSuccess(_, _, _)
        0 * response.sendRedirect(_)
    }

    // ---------------------------------------------------------------- helpers

    private MfaLoginSuccessHandler handlerFor(MfaPolicyMode mode) {
        def guard = new MfaLoginGuard(factors, graces, new MfaPolicy(mode.name(), GRACE))
        return new MfaLoginSuccessHandler(guard, requestCache, delegate)
    }

    private Authentication authentication() {
        def user = new life.qbic.projectmanagement.application.authorization.User(
                "u-1", "Ada Example", "ada", "ada@example.org", "hash", true, null, null)
        def details = new QbicUserDetails(user, [])
        return UsernamePasswordAuthenticationToken.authenticated(details, null, [])
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
        final Map<String, MfaGrace> stored = [:]

        @Override
        Optional<MfaGrace> findByUserId(String userId) {
            Optional.ofNullable(stored[userId])
        }

        @Override
        MfaGrace startIfAbsent(MfaGrace candidate) {
            if (!stored.containsKey(candidate.userId())) {
                stored[candidate.userId()] = candidate
            }
            return stored[candidate.userId()]
        }
    }
}
