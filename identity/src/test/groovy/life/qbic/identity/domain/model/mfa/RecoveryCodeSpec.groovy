package life.qbic.identity.domain.model.mfa

import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant

/**
 * <b>The last self-service way in (AUTH-R-03, ADR 0011 B1).</b>
 *
 * <p>A recovery code is a credential, so the only behaviour that matters is that it works exactly
 * once and that regeneration retires the old set rather than leaving two live sets behind. There is
 * deliberately no expiry: a code that ages out recreates the lockout it exists to prevent.</p>
 */
class RecoveryCodeSpec extends Specification {

    private static final Instant GENERATED = Instant.parse("2026-11-17T10:00:00Z")
    private static final String HASH = "pbkdf2:100000:deadbeef:cafebabe"

    private static RecoveryCode aCode() {
        RecoveryCode.issue("u-1", HASH, GENERATED)
    }

    def "a freshly generated code is usable and records when it was generated"() {
        when:
        def code = aCode()

        then:
        code.isUsable()
        code.userId() == "u-1"
        code.codeHash() == HASH
        code.createdAt() == GENERATED
        code.usedAt() == null
        code.invalidatedAt() == null
    }

    def "a code works once"() {
        given:
        def code = aCode()

        when:
        code.consume(GENERATED.plusSeconds(60))

        then:
        !code.isUsable()
        code.usedAt() == GENERATED.plusSeconds(60)
    }

    def "a spent code cannot be spent again"() {
        given:
        def code = aCode()
        code.consume(GENERATED.plusSeconds(60))

        when:
        code.consume(GENERATED.plusSeconds(120))

        then:
        thrown(IllegalStateException)
        code.usedAt() == GENERATED.plusSeconds(60)
    }

    def "regeneration retires an unused code"() {
        given:
        def code = aCode()

        when:
        def retired = code.invalidate(GENERATED.plus(Duration.ofDays(30)))

        then:
        retired
        !code.isUsable()
        code.invalidatedAt() == GENERATED.plus(Duration.ofDays(30))
        code.usedAt() == null
    }

    def "regeneration leaves a spent code marked as used, not as invalidated"() {
        given: "the audit trail must say a code was used, not merely retired"
        def code = aCode()
        code.consume(GENERATED.plusSeconds(60))

        when:
        def retired = code.invalidate(GENERATED.plus(Duration.ofDays(30)))

        then:
        !retired
        code.usedAt() == GENERATED.plusSeconds(60)
        code.invalidatedAt() == null
    }

    def "a retired code cannot be spent"() {
        given:
        def code = aCode()
        code.invalidate(GENERATED.plus(Duration.ofDays(30)))

        when:
        code.consume(GENERATED.plus(Duration.ofDays(31)))

        then:
        thrown(IllegalStateException)
        code.usedAt() == null
    }

    @Unroll
    def "a hash that is blank, missing or too long is refused (#reason)"() {
        when:
        RecoveryCode.issue("u-1", hash, GENERATED)

        then:
        thrown(expected)

        where:
        expected                 | hash      | reason
        IllegalArgumentException | ""        | "empty"
        IllegalArgumentException | "   "     | "whitespace"
        NullPointerException     | null      | "null"
        IllegalArgumentException | "x" * 256 | "does not fit the column"
    }

    def "the hash is stored trimmed"() {
        expect:
        RecoveryCode.issue("u-1", "  $HASH  ", GENERATED).codeHash() == HASH
    }

    def "the user id is required"() {
        when:
        RecoveryCode.issue(" ", HASH, GENERATED)

        then:
        thrown(IllegalArgumentException)
    }
}
