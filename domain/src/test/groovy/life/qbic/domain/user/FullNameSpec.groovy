package life.qbic.domain.user

import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class FullNameSpec extends Specification {

    def "Given an blank name, throw an IllegalArgumentException"() {
        when:
        FullName.from("  ")

        then:
        thrown(FullName.InvalidFullNameException)
    }

    def "Given an empty name, throw an IllegalArgumentException"() {
        when:
        FullName.from("")

        then:
        thrown(FullName.InvalidFullNameException)
    }

    def "Given a valid name, create an instance of a full name object"() {
        when:
        FullName name = FullName.from("Test Tester")

        then:
        name.name().equals("Test Tester")
    }
}
