package life.qbic.identity.domain.model.mfa

import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant

/**
 * <b>The aggregate that keeps a malformed factor out of the database (AUTH-R-02, AUTH-NFR-01).</b>
 *
 * <p>Nothing here is a UI concern: these are the invariants that decide whether a row is a factor at
 * all, whether it may be presented, and whether a removed credential can come back to life.</p>
 */
class SecondFactorSpec extends Specification {

    private static final Instant ENROLLED = Instant.parse("2026-11-17T10:00:00Z")
    // 12-byte nonce + 16-byte tag + one ciphertext byte
    private static final ProtectedSecret SECRET =
            ProtectedSecret.ofBase64(Base64.encoder.encodeToString(new byte[29]))
    private static final String CREDENTIAL = '{"credentialId":"abc","publicKey":"...","signCount":0}'

    private static SecondFactor totp() {
        SecondFactor.enrolTotp("u-1", SECRET, "iPhone (1Password)", ENROLLED)
    }

    private static SecondFactor webauthn() {
        SecondFactor.enrolWebAuthn("u-1", CREDENTIAL, "YubiKey 5", ENROLLED)
    }

    // ---------------------------------------------------------------- enrolment shape

    def "a TOTP factor carries the protected secret and no WebAuthn credential"() {
        when:
        def factor = totp()

        then:
        factor.type() == SecondFactorType.TOTP
        factor.secret().isPresent()
        factor.secret().get() == SECRET
        factor.webauthnCredential().isEmpty()
        factor.isActive()
        factor.createdAt() == ENROLLED
        factor.lastUsedAt() == null
        factor.disabledAt() == null
    }

    def "a WebAuthn factor carries the public credential and no secret"() {
        when:
        def factor = webauthn()

        then: "there is no server-side secret to leak (AUTH-NFR-01)"
        factor.type() == SecondFactorType.WEBAUTHN
        factor.webauthnCredential().get() == CREDENTIAL
        factor.secret().isEmpty()
    }

    @Unroll
    def "a blank WebAuthn credential is refused (#reason)"() {
        when:
        SecondFactor.enrolWebAuthn("u-1", credential, "YubiKey 5", ENROLLED)

        then: "a factor without credential id and public key cannot be verified, so it is not a factor"
        thrown(IllegalArgumentException)

        where:
        credential | reason
        "   "      | "whitespace"
        ""         | "empty"
    }

    def "the user id is required"() {
        when:
        SecondFactor.enrolTotp("  ", SECRET, "iPhone", ENROLLED)

        then:
        thrown(IllegalArgumentException)
    }

    // ---------------------------------------------------------------- display name

    def "the display name is trimmed and must be non-blank"() {
        expect:
        SecondFactor.enrolTotp("u-1", SECRET, "  iPhone  ", ENROLLED).displayName() == "iPhone"
        SecondFactor.enrolTotp("u-1", SECRET, name, ENROLLED).displayName() == expected

        where:
        name              | expected
        "a"               | "a"
        " a b "           | "a b"
        "x" * 255         | "x" * 255
        "ünïcödé label"   | "ünïcödé label"
    }

    @Unroll
    def "a display name that is blank or too long is refused (#reason)"() {
        when:
        SecondFactor.enrolTotp("u-1", SECRET, name, ENROLLED)

        then:
        thrown(expected)

        where:
        expected                 | name      | reason
        IllegalArgumentException | ""        | "empty"
        IllegalArgumentException | "    "    | "whitespace"
        NullPointerException     | null      | "null"
        IllegalArgumentException | "x" * 256 | "longer than the column"
    }

    def "renaming relabels the factor without touching its credential"() {
        given:
        def factor = totp()

        when:
        factor.rename("  iPad  ")

        then: "only the label moved — the credential is the same factor, not a new enrolment"
        factor.displayName() == "iPad"
        factor.secret().get() == SECRET
        factor.type() == SecondFactorType.TOTP
        factor.createdAt() == ENROLLED
    }

    // ---------------------------------------------------------------- lifecycle

    def "a removed factor stays removed, and records when it was actually removed"() {
        given:
        def factor = totp()

        when:
        def firstRemoval = factor.disable(ENROLLED)
        def secondRemoval = factor.disable(ENROLLED.plusSeconds(60))

        then: "the audit trail must show the user's action, not when the code ran last"
        firstRemoval
        !secondRemoval
        !factor.isActive()
        factor.disabledAt() == ENROLLED
    }

    def "a disabled factor can never authenticate"() {
        given:
        def factor = totp()
        factor.disable(ENROLLED)

        when:
        factor.markUsed(ENROLLED.plusSeconds(1))

        then: "failing loudly is what stops a stale row in a session from being presented again"
        thrown(IllegalStateException)
        factor.lastUsedAt() == null
    }

    def "using an active factor records the instant"() {
        given:
        def factor = webauthn()

        when:
        factor.markUsed(ENROLLED.plusSeconds(5))

        then:
        factor.lastUsedAt() == ENROLLED.plusSeconds(5)
        factor.isActive()
    }
}
