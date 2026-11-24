package life.qbic.identity.domain.model.mfa

import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>The keyed hash that lets a recovery code be looked up without its account (AUTH-R-03).</b>
 *
 * <p>Two properties have to hold at once, and they pull in opposite directions: the hash must be
 * <em>deterministic</em> (the login path has no account id, so the code is the lookup key), and it must
 * not be <em>recomputable</em> from a stolen database (that is what the pepper buys).</p>
 */
class RecoveryCodeEncoderSpec extends Specification {

    private static final String PEPPER = "a".repeat(RecoveryCodeEncoder.MIN_PEPPER_LENGTH)
    private static final String CODE = "A2B3C4D5E6F7"

    private static RecoveryCodeEncoder encoder(String pepper = PEPPER) {
        new RecoveryCodeEncoder(pepper)
    }

    def "hashing is deterministic, because the hash is the lookup key"() {
        expect:
        encoder().hash(CODE) == encoder().hash(CODE)
    }

    def "the same code hashes identically however the user typed it"() {
        expect: "otherwise a correct sheet produces a code that 'does not work'"
        encoder().hash("a2b3-c4d5-e6f7") == encoder().hash(CODE)
        encoder().hash("  A2B3 C4D5 E6F7 ") == encoder().hash(CODE)
    }

    def "the digest is a lower-case 64-character hex string"() {
        expect: "the column is varchar(255) and the value is compared byte-for-byte"
        encoder().hash(CODE) ==~ /[0-9a-f]{64}/
    }

    def "different codes hash differently, so one hash cannot stand in for another"() {
        expect:
        encoder().hash(CODE) != encoder().hash("A2B3C4D5E6F6")
    }

    def "the hash does not contain the code"() {
        expect:
        !encoder().hash(CODE).toUpperCase().contains(CODE)
    }

    def "a different pepper produces a different hash"() {
        given: "the pepper was rotated, or another deployment's database was restored"
        def other = "b".repeat(RecoveryCodeEncoder.MIN_PEPPER_LENGTH)

        expect: "which is why the pepper must be backed up with the configuration"
        encoder().hash(CODE) != encoder(other).hash(CODE)
    }

    def "matches re-verifies in memory, for the paths that do not look up by hash"() {
        given:
        def storedHash = encoder().hash(CODE)

        expect:
        encoder().matches(CODE, storedHash)
        encoder().matches("a2b3-c4d5-e6f7", storedHash)

        and: "a wrong or malformed code never matches"
        !encoder().matches("A2B3C4D5E6F6", storedHash)
        !encoder().matches("nonsense", storedHash)
    }

    @Unroll
    def "a pepper shorter than #length characters is refused"() {
        when: "a short pepper is guessable from a single leaked hash"
        encoder("x" * length)

        then:
        thrown(IllegalArgumentException)

        where:
        length << [0, 1, 16, RecoveryCodeEncoder.MIN_PEPPER_LENGTH - 1]
    }

    def "a missing pepper is refused"() {
        when:
        new RecoveryCodeEncoder(null)

        then:
        thrown(NullPointerException)
    }

    def "the encoder never prints the pepper"() {
        expect:
        encoder().toString() == "RecoveryCodeEncoder[pepper=redacted]"
    }
}
