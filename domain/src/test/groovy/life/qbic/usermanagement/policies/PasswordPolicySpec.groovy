package life.qbic.usermanagement.policies

import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class PasswordPolicySpec extends Specification {

    def "Given a password length shorter than 8 characters, the policy check shall fail"() {
        when:
        PolicyCheckReport report = PasswordPolicy.create().validate(password)

        then:
        report.status() == PolicyStatus.FAILED
        report.reason().equalsIgnoreCase("Password shorter than 8 characters.")

        where:
        password << [
                "a",
                "ab",
                "abc",
                "abcd",
                "abcde",
                "abcdef",
                "abcdefg"
        ]
    }

    def "Given a password length longer or equal to 8 characters, the policy check shall pass"() {
        when:
        PolicyCheckReport report = PasswordPolicy.create().validate(password)

        then:
        report.status() == PolicyStatus.PASSED
        report.reason().isBlank()

        where:
        password << [
                "astrongpassphrase",
                "whatever"
        ]
    }

}
