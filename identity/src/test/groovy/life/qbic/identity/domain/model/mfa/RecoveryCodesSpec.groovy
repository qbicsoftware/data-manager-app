package life.qbic.identity.domain.model.mfa

import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>The format of the last self-service way in (AUTH-R-03).</b>
 *
 * <p>These assertions are load-bearing for two different reasons: the entropy determines whether a
 * printed sheet is a backdoor, and the normalisation determines whether a user with a correct code can
 * actually use it.</p>
 */
class RecoveryCodesSpec extends Specification {

    def "a set has ten codes of twelve characters from the Base32 alphabet"() {
        when:
        def codes = RecoveryCodes.generate()

        then: "60 bits per code: 12 characters × 5 bits"
        codes.size() == RecoveryCodes.SET_SIZE
        codes.every { it.length() == RecoveryCodes.CODE_LENGTH }
        codes.every { it ==~ /[A-Z2-7]{12}/ }
    }

    def "the codes in a set are distinct"() {
        expect: "a duplicate looks like a bug to the user and costs one line to rule out"
        new ArrayList<String>(RecoveryCodes.generate()).unique().size() == RecoveryCodes.SET_SIZE
    }

    def "two sets are different"() {
        expect:
        RecoveryCodes.generate() != RecoveryCodes.generate()
    }

    def "no code contains a digit that collides with a letter"() {
        expect: "the alphabet allows 2-7 only, so 0/O and 1/I/L confusion cannot arise"
        RecoveryCodes.generate().every { !(it ==~ /.*[0189].*/) }
    }

    def "formatting groups the code for reading, and only for reading"() {
        given:
        def code = "A2B3C4D5E6F7"

        expect:
        RecoveryCodes.format(code) == "A2B3-C4D5-E6F7"

        and: "a formatted code normalises back to the same value"
        RecoveryCodes.normalize(RecoveryCodes.format(code)) == code
    }

    @Unroll
    def "user input is normalised: '#input' -> 'A2B3C4D5E6F7'"() {
        expect:
        RecoveryCodes.normalize(input) == "A2B3C4D5E6F7"

        where:
        input << ["A2B3C4D5E6F7", "a2b3c4d5e6f7", "A2B3-C4D5-E6F7",
                  "a2b3 c4d5 e6f7", "  A2B3-C4D5-E6F7  "]
    }

    @Unroll
    def "input that cannot be a code is refused: #reason"(String input, String reason) {
        when:
        RecoveryCodes.normalize(input)

        then:
        thrown(IllegalArgumentException)

        where:
        input                | reason
        ""                   | "empty"
        "A2B3C4D5E6F"        | "one character short"
        "A2B3C4D5E6F78"      | "one character long"
        "A2B3C4D5E6F0"       | "0 is not in the alphabet"
        "A2B3C4D5E6F1"       | "1 is not in the alphabet"
        "A2B3C4D5E6F8"       | "8 is not in the alphabet"
        "A2B3C4D5E6F!"       | "punctuation"
    }

    def "a null input is refused"() {
        when:
        RecoveryCodes.normalize(null)

        then:
        thrown(NullPointerException)
    }
}
