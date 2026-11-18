package life.qbic.datamanager.security.mfa

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import life.qbic.projectmanagement.application.authorization.QbicUserDetails
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import spock.lang.Specification

import java.time.Instant

/**
 * <b>The safety net behind the success handler (M4-1).</b>
 *
 * <p>If an authenticated session ever coexists with a pending second factor, it must lose the
 * authentication on the next request. This is the assertion that makes the property hold even if a
 * future login path forgets the handler.</p>
 */
class MfaChallengeEnforcementFilterSpec extends Specification {

    static final String CONTEXT_PATH = "/app"

    MfaChallengeEnforcementFilter filter = new MfaChallengeEnforcementFilter()
    HttpServletRequest request = Mock()
    HttpServletResponse response = Mock()
    HttpSession session = Mock()
    FilterChain chain = Mock()

    def setup() {
        request.getContextPath() >> CONTEXT_PATH
        request.getSession(false) >> session
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }

    // ---------------------------------------------------------------- the property

    def "an authenticated session with a pending factor loses its authentication"() {
        given:
        session.getAttribute(MfaPendingLogin.SESSION_ATTRIBUTE) >> pending()
        request.getRequestURI() >> (CONTEXT_PATH + "/projects")
        authenticateAs("u-1")

        when:
        filter.doFilter(request, response, chain)

        then:
        SecurityContextHolder.context.authentication == null
        1 * session.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)
        1 * response.sendRedirect(CONTEXT_PATH + "/login/second-factor")
        0 * chain.doFilter(_, _)
    }

    // ---------------------------------------------------------------- the no-ops

    def "without a pending factor the request passes through untouched"() {
        given:
        session.getAttribute(MfaPendingLogin.SESSION_ATTRIBUTE) >> null
        authenticateAs("u-1")

        when:
        filter.doFilter(request, response, chain)

        then:
        1 * chain.doFilter(request, response)
        0 * session.removeAttribute(_)
        SecurityContextHolder.context.authentication != null
    }

    def "an unauthenticated request during a challenge is not redirected in a loop"() {
        given:
        session.getAttribute(MfaPendingLogin.SESSION_ATTRIBUTE) >> pending()

        when:
        filter.doFilter(request, response, chain)

        then:
        1 * chain.doFilter(request, response)
        0 * response.sendRedirect(_)
    }

    def "an anonymous authentication does not count as authenticated"() {
        given:
        session.getAttribute(MfaPendingLogin.SESSION_ATTRIBUTE) >> pending()
        SecurityContextHolder.context.authentication = new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"))

        when:
        filter.doFilter(request, response, chain)

        then:
        1 * chain.doFilter(request, response)
    }

    def "the challenge and login paths are exempt, so the user can actually get there"() {
        given:
        session.getAttribute(MfaPendingLogin.SESSION_ATTRIBUTE) >> pending()
        request.getRequestURI() >> uri
        authenticateAs("u-1")

        when:
        filter.doFilter(request, response, chain)

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
        filter.doFilter(request, response, chain)

        then:
        1 * chain.doFilter(request, response)
    }

    // ---------------------------------------------------------------- helpers

    private static MfaPendingLogin pending() {
        new MfaPendingLogin("u-1", "ada@example.org", Instant.now())
    }

    private void authenticateAs(String userId) {
        def user = new life.qbic.projectmanagement.application.authorization.User(
                userId, "Ada Example", "ada", "ada@example.org", "hash", true, null, null)
        SecurityContextHolder.context.authentication = UsernamePasswordAuthenticationToken.authenticated(
                new QbicUserDetails(user, []), null, [])
    }
}
