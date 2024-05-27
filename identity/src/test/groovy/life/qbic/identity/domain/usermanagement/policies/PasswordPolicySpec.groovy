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

    def "Given a password length shorter than 12 characters, the policy check shall fail"() {
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
                "abcdefg",
                "abcdefgh",
                "abcdefghi",
                "abcdefghij",
                "abcdefghijk",
        ]
    }

    def "Given a password length longer or equal to 812 characters, the policy check shall pass"() {
        when:
        PolicyCheckReport report = PasswordPolicy.instance().validate(password.toCharArray())

        then:
        report.status() == PolicyStatus.PASSED
        report.reason().isBlank()

        where:
        password << [
                "astrongpassphrase",
                "whatever0001",
                "123456789012"
        ]
    }

}
