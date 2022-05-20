package life.qbic.domain.usermanagement.policies

import life.qbic.domain.usermanagement.policies.PasswordPolicy
import life.qbic.domain.usermanagement.policies.PolicyCheckReport
import life.qbic.domain.usermanagement.policies.PolicyStatus
import spock.lang.Specification

/**
 * <b>Tests for the {@link PasswordPolicy}</b>
 *
 * @since 1.0.0
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
