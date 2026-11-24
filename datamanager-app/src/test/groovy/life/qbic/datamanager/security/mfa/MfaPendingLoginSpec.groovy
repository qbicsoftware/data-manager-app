package life.qbic.datamanager.security.mfa

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant

/**
 * <b>The five-minute window between password and factor (M4-1).</b>
 *
 * <p>Three properties: what is stored is not an authentication, it cannot be completed after it has
 * gone stale, and clearing it is idempotent because the challenge clears on both outcomes.</p>
 */
class MfaPendingLoginSpec extends Specification {

    private static final Instant NOW = Instant.parse("2026-11-18T09:00:00Z")

    HttpServletRequest request = Mock()
    HttpSession session = Mock()

    def setup() {
        request.getSession() >> session
        request.getSession(false) >> session
    }

    def "a pending login round-trips through the session"() {
        given:
        def pending = new MfaPendingLogin("u-1", "ada@example.org", NOW)
        session.getAttribute(MfaPendingLogin.SESSION_ATTRIBUTE) >> pending

        when:
        MfaPendingLogin.store(request, pending)

        then:
        1 * session.setAttribute(MfaPendingLogin.SESSION_ATTRIBUTE, pending)
        MfaPendingLogin.from(session).orElseThrow() == pending
    }

    def "nothing stored means no pending login"() {
        given:
        session.getAttribute(MfaPendingLogin.SESSION_ATTRIBUTE) >> null

        expect:
        MfaPendingLogin.from(session).isEmpty()
        MfaPendingLogin.from(null).isEmpty()
        MfaPendingLogin.usableFrom(session, NOW).isEmpty()
    }

    @Unroll
    def "a pending login is usable only before its deadline (#description)"() {
        given:
        def pending = new MfaPendingLogin("u-1", "ada@example.org", NOW)
        session.getAttribute(MfaPendingLogin.SESSION_ATTRIBUTE) >> pending

        expect:
        MfaPendingLogin.usableFrom(session, at).isPresent() == usable

        where:
        description                 | at                                       | usable
        "just after the password"   | NOW.plusSeconds(1)                       | true
        "three minutes later"       | NOW.plus(Duration.ofMinutes(3))          | true
        "one second before expiry"  | NOW.plus(MfaPendingLogin.LIFETIME).minusSeconds(1) | true
        "exactly at expiry"         | NOW.plus(MfaPendingLogin.LIFETIME)       | false
        "after expiry"              | NOW.plus(Duration.ofHours(1))            | false
    }

    def "the window is five minutes"() {
        expect: "long enough to open an app, short enough that a forgotten tab does not stay half-authenticated"
        MfaPendingLogin.LIFETIME == Duration.ofMinutes(5)
    }

    def "clearing is safe and idempotent"() {
        when: "the challenge completes, or the login is abandoned"
        MfaPendingLogin.clear(session)

        then:
        1 * session.removeAttribute(MfaPendingLogin.SESSION_ATTRIBUTE)

        when: "and there is no session at all"
        MfaPendingLogin.clear(null)

        then:
        noExceptionThrown()
    }

    def "an expired pending login stays readable, only unusable — so it can be cleared"() {
        given:
        def pending = new MfaPendingLogin("u-1", "ada@example.org", NOW.minus(Duration.ofHours(1)))
        session.getAttribute(MfaPendingLogin.SESSION_ATTRIBUTE) >> pending

        expect:
        MfaPendingLogin.from(session).isPresent()
        MfaPendingLogin.usableFrom(session, NOW).isEmpty()
    }

    def "the pending login carries no authority of its own"() {
        expect: "it is a plain record, not an Authentication — that is the whole design"
        !org.springframework.security.core.Authentication.isAssignableFrom(MfaPendingLogin.class)
    }
}
