package life.qbic.datamanager.views.login

import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>Grouping a recovery code while it is typed.</b>
 *
 * <p>The field shows {@code XXXX-XXXX-XXXX}; the encoder hashes the same characters without the hyphens.
 * This formatter runs on every keystroke, so it has to tolerate a value that is still too short or
 * temporarily malformed — the strict rules stay at submission, where a bad value can be refused.</p>
 */
class RecoveryCodeInputFormattingSpec extends Specification {

    @Unroll
    def "typing groups every four characters: '#typed' -> '#expected'"() {
        expect:
        SecondFactorView.formatRecoveryCodeInput(typed) == expected

        where:
        typed            | expected
        ""               | ""
        "A"              | "A"
        "ABCD"           | "ABCD"
        "ABCDE"          | "ABCD-E"
        "ABCDEFGH"       | "ABCD-EFGH"
        "ABCDEFGHI"      | "ABCD-EFGH-I"
        "ABCDEFGHIJKL"   | "ABCD-EFGH-IJKL"
    }

    def "lower case is upper-cased as it is typed"() {
        expect: "codes are printed in upper case; typing in lower case must not look wrong"
        SecondFactorView.formatRecoveryCodeInput("abcdefghijkl") == "ABCD-EFGH-IJKL"
    }

    def "hyphens pasted from the printed sheet are re-created, not doubled"() {
        expect:
        SecondFactorView.formatRecoveryCodeInput("abcd-efgh-ijkl") == "ABCD-EFGH-IJKL"
        SecondFactorView.formatRecoveryCodeInput("ABCD-EFGH-IJKL") == "ABCD-EFGH-IJKL"
        SecondFactorView.formatRecoveryCodeInput("  abcd efgh ijkl  ") == "ABCD-EFGH-IJKL"
    }

    def "characters outside the alphabet are dropped instead of blocking the keystroke"() {
        expect: "0, 1, 8 and 9 and punctuation never appear in a code"
        SecondFactorView.formatRecoveryCodeInput("AB0CD1EF8GH9") == "ABCD-EFGH"
        SecondFactorView.formatRecoveryCodeInput("ABCD!EFGH?IJKL") == "ABCD-EFGH-IJKL"
    }

    def "input longer than a code is truncated, so the field cannot hold a pasted paragraph"() {
        expect:
        SecondFactorView.formatRecoveryCodeInput("ABCDEFGHIJKL" + "MNOPQRST") == "ABCD-EFGH-IJKL"
    }

    def "a null value formats to empty rather than throwing"() {
        expect:
        SecondFactorView.formatRecoveryCodeInput(null) == ""
    }

    def "the grouped form is what the service will accept"() {
        given:
        def typed = SecondFactorView.formatRecoveryCodeInput("abcdefghijkl")

        expect: "the encoder normalises the hyphens away again, so grouped input round-trips"
        life.qbic.identity.domain.model.mfa.RecoveryCodes.normalize(typed) == "ABCDEFGHIJKL"
    }
}
