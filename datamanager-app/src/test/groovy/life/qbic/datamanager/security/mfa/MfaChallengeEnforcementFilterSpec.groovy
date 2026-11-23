package life.qbic.datamanager.security.mfa

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import life.qbic.identity.application.mfa.GraceEligibility
import life.qbic.identity.application.mfa.MfaLoginGuard
import life.qbic.identity.application.mfa.MfaPolicy
import life.qbic.identity.domain.model.mfa.MfaGrace
import life.qbic.identity.domain.model.mfa.MfaPolicyMode
import life.qbic.identity.domain.model.mfa.ProtectedSecret
import life.qbic.identity.domain.model.mfa.SecondFactor
import life.qbic.identity.domain.repository.MfaGraceRepository
import life.qbic.identity.domain.repository.SecondFactorRepository
import life.qbic.projectmanagement.application.authorization.QbicUserDetails
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * <b>Enforcement that does not depend on the login configuration (M4-1).</b>
 *
 * <p>This filter is what makes the requirement hold even when a framework configurer replaces the
 * success handler — which is exactly what happened on the first deployment: policy {@code OPTIONAL},
 * account enrolled, nothing asked.</p>
 */
class MfaChallengeEnforcementFilterSpec extends Specification {

    static final String CONTEXT_PATH = "/app"
    static final Duration GRACE = Duration.ofHours(48)
    static final ProtectedSecret SECRET =
            ProtectedSecret.ofBase64(Base64.encoder.encodeToString(new byte[32]))

    FakeFactors factors = new FakeFactors()
    FakeGraces graces = new FakeGraces()

    HttpServletRequest request = Mock()
    HttpServletResponse response = Mock()
    HttpSession session = Mock()
    FilterChain chain = Mock()

    def setup() {
        request.getContextPath() >> CONTEXT_PATH
        request.getSession(false) >> session
        request.getSession() >> session
        // Session attributes are left unstubbed on purpose: a Mock returns null, and a catch-all stub
        // in setup() takes precedence over the per-test one, which silently turns every challenge into
        // a pass-through.
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    private MfaChallengeEnforcementFilter filterFor(MfaPolicyMode mode, boolean owesGrace = true) {
        new MfaChallengeEnforcementFilter(
                new MfaLoginGuard(factors, graces, new MfaPolicy(mode.name(), GRACE, ""),
                        { String ignored -> owesGrace } as GraceEligibility))
    }

    // ---------------------------------------------------------------- the challenge

    def "an enrolled account under OPTIONAL is challenged at the request boundary"() {
        given: "the success handler did not run — the scenario that broke on the first deployment"
        factors.save(SecondFactor.enrolTotp("u-1", SECRET, "iPhone", Instant.now()))
        authenticateAs("u-1")

        when:
        filterFor(MfaPolicyMode.OPTIONAL).doFilter(request, response, chain)

        then: "the session is withheld and the identity is remembered for the challenge"
        1 * session.setAttribute(MfaPendingLogin.SESSION_ATTRIBUTE, { MfaPendingLogin p ->
            p.userId() == "u-1" && p.email() == "ada@example.org"
        })
        1 * session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)
        1 * response.sendRedirect(CONTEXT_PATH + "/login/second-factor")
        0 * chain.doFilter(_, _)
        SecurityContextHolder.context.authentication == null
    }

    def "an authenticated session that coexists with a pending factor is withdrawn"() {
        given:
        session.getAttribute(MfaPendingLogin.SESSION_ATTRIBUTE) >>
                new MfaPendingLogin("u-1", "ada@example.org", Instant.now())
        authenticateAs("u-1")

        when:
        filterFor(MfaPolicyMode.OFF).doFilter(request, response, chain)

        then:
        1 * session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)
        1 * response.sendRedirect(CONTEXT_PATH + "/login/second-factor")
        0 * chain.doFilter(_, _)
    }

    def "an expired grace window denies and says why"() {
        given:
        graces.stored["u-1"] = MfaGrace.start("u-1", Instant.now().minus(Duration.ofDays(10)), GRACE)
        authenticateAs("u-1")

        when:
        filterFor(MfaPolicyMode.REQUIRED).doFilter(request, response, chain)

        then:
        1 * response.sendRedirect(CONTEXT_PATH + "/login/second-factor?graceExpired")
        0 * chain.doFilter(_, _)
    }

    // ---------------------------------------------------------------- enrolment due (registration path)

    def "an account that is not owed a window is sent to enrolment, with no session"() {
        given: "a new account under REQUIRED: no factor, no deadline to count down"
        authenticateAs("u-1")

        when:
        filterFor(MfaPolicyMode.REQUIRED, false).doFilter(request, response, chain)

        then: "the same mechanical pair as the challenge, aimed at the enrolment page"
        1 * session.setAttribute(MfaPendingLogin.SESSION_ATTRIBUTE, { MfaPendingLogin p ->
            p.userId() == "u-1" && p.email() == "ada@example.org"
        })
        1 * session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)
        1 * response.sendRedirect(CONTEXT_PATH + "/login/enrol")
        0 * chain.doFilter(_, _)
        SecurityContextHolder.context.authentication == null
    }

    def "the enrolment page itself is not re-gated, or the redirect would loop"() {
        given: "the request arrives at the page the filter just redirected to"
        request.getRequestURI() >> (CONTEXT_PATH + "/login/enrol")
        authenticateAs("u-1")

        when:
        filterFor(MfaPolicyMode.REQUIRED, false).doFilter(request, response, chain)

        then:
        1 * chain.doFilter(request, response)
        0 * response.sendRedirect(_)
    }

    def "an enrolment-due account gets no grace row written"() {
        given:
        authenticateAs("u-1")

        when:
        filterFor(MfaPolicyMode.REQUIRED, false).doFilter(request, response, chain)

        then: "nothing persisted, so nothing can expire and no lockout can follow"
        graces.stored.isEmpty()
        0 * chain.doFilter(_, _)
    }

    // ---------------------------------------------------------------- the pass-throughs

    def "a session that already passed the step is not asked again"() {
        given:
        factors.save(SecondFactor.enrolTotp("u-1", SECRET, "iPhone", Instant.now()))
        session.getAttribute(MfaSessionState.SESSION_ATTRIBUTE) >>
                new MfaSessionState("u-1", Instant.now())
        authenticateAs("u-1")

        when:
        filterFor(MfaPolicyMode.REQUIRED).doFilter(request, response, chain)

        then: "one session lookup, no database work on every request"
        1 * chain.doFilter(request, response)
        0 * response.sendRedirect(_)
    }

    def "a session marked for another account does not satisfy this one"() {
        given:
        factors.save(SecondFactor.enrolTotp("u-1", SECRET, "iPhone", Instant.now()))
        session.getAttribute(MfaSessionState.SESSION_ATTRIBUTE) >>
                new MfaSessionState("someone-else", Instant.now())
        authenticateAs("u-1")

        when:
        filterFor(MfaPolicyMode.REQUIRED).doFilter(request, response, chain)

        then:
        1 * response.sendRedirect(CONTEXT_PATH + "/login/second-factor")
        0 * chain.doFilter(_, _)
    }

    def "an unenrolled account under OPTIONAL proceeds and is marked"() {
        authenticateAs("u-1")

        when:
        filterFor(MfaPolicyMode.OPTIONAL).doFilter(request, response, chain)

        then:
        1 * session.setAttribute(MfaSessionState.SESSION_ATTRIBUTE, _)
        1 * chain.doFilter(request, response)
        0 * response.sendRedirect(_)
    }

    def "REQUIRED grants and persists a window, then marks the session"() {
        authenticateAs("u-1")

        when:
        filterFor(MfaPolicyMode.REQUIRED).doFilter(request, response, chain)

        then:
        1 * chain.doFilter(request, response)
        graces.stored["u-1"] != null
        graces.stored["u-1"].graceUntil() == graces.stored["u-1"].startedAt().plus(GRACE)
    }

    def "an unauthenticated request is never redirected in a loop"() {
        when:
        filterFor(MfaPolicyMode.REQUIRED).doFilter(request, response, chain)

        then:
        1 * chain.doFilter(request, response)
        0 * response.sendRedirect(_)
    }

    def "an anonymous authentication does not count as authenticated"() {
        given:
        SecurityContextHolder.context.authentication = new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"))

        when:
        filterFor(MfaPolicyMode.REQUIRED).doFilter(request, response, chain)

        then:
        1 * chain.doFilter(request, response)
    }

    def "the challenge and login paths are exempt, so the user can actually get there"() {
        given:
        factors.save(SecondFactor.enrolTotp("u-1", SECRET, "iPhone", Instant.now()))
        request.getRequestURI() >> uri
        authenticateAs("u-1")

        when:
        filterFor(MfaPolicyMode.REQUIRED).doFilter(request, response, chain)

        then:
        1 * chain.doFilter(request, response)
        0 * response.sendRedirect(_)

        where:
        uri << [CONTEXT_PATH + "/login/second-factor", CONTEXT_PATH + "/login"]
    }

    def "a request without a session passes through"() {
        given:
        request.getSession(false) >> null

        when:
        filterFor(MfaPolicyMode.REQUIRED).doFilter(request, response, chain)

        then:
        1 * chain.doFilter(request, response)
    }

    // ---------------------------------------------------------------- helpers

    private void authenticateAs(String userId) {
        def user = new life.qbic.projectmanagement.application.authorization.User(
                userId, "Ada Example", "ada", "ada@example.org", "hash", true, null, null)
        SecurityContextHolder.context.authentication =
                UsernamePasswordAuthenticationToken.authenticated(new QbicUserDetails(user, []), null, [])
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
