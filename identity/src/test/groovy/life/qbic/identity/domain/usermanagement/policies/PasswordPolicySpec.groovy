package life.qbic.identity.domain.usermanagement.policies


import life.qbic.identity.domain.model.PasswordPolicy
import life.qbic.identity.domain.model.policy.PolicyCheckReport
import life.qbic.identity.domain.model.policy.PolicyStatus
import spock.lang.Specification

/**
 * <b>Tests for the {@link PasswordPolicy}</b>
 *
 * @since 1.0.0
 */
class PasswordPolicySpec extends Specification {

    def "Given a password length shorter than 8 characters, the policy check shall fail"() {
        when:
        PolicyCheckReport report = PasswordPolicy.instance().validate(password.toCharArray())

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
        PolicyCheckReport report = PasswordPolicy.instance().validate(password.toCharArray())

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
