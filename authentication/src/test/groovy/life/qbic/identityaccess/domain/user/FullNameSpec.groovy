package life.qbic.identityaccess.domain.user

import spock.lang.Specification

/**
 * <b>Tests for the {@link FullName}</b>
 *
 * @since 1.0.0
 */
class FullNameSpec extends Specification {

    def "Given an blank name, throw an IllegalArgumentException"() {
        when:
        FullName.from("  ")

        then:
        thrown(FullName.FullNameValidationException)
    }

    def "Given an empty name, throw an IllegalArgumentException"() {
        when:
        FullName.from("")

        then:
        thrown(FullName.FullNameValidationException)
    }

    def "Given a valid name, create an instance of a full name object"() {
        when:
        FullName name = FullName.from("Test Tester")

        then:
        name.get().equals("Test Tester")
    }
}
