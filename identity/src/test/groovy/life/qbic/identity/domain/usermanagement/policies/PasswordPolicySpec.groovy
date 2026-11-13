package life.qbic.identity.domain.usermanagement.policies


import life.qbic.identity.api.Passwords
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

    def "Given a password length shorter than the minimum, the policy check shall fail"() {
        when:
        PolicyCheckReport report = PasswordPolicy.instance().validate(password.toCharArray())

        then:
        report.status() == PolicyStatus.FAILED
        // The message must state the ACTUAL minimum. It used to say "8 characters" while the rule
        // enforced 12, so a user typing nine characters was told about a rule that was not applied.
        // Deriving the expectation from Passwords keeps this spec from pinning a stale number again.
        report.reason() == Passwords.tooShortMessage()
        report.reason().contains("${Passwords.MIN_LENGTH}")

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

    def "Given a password length longer or equal to the minimum, the policy check shall pass"() {
        when:
        PolicyCheckReport report = PasswordPolicy.instance().validate(password.toCharArray())

        then:
        report.status() == PolicyStatus.PASSED
        report.reason().isBlank()

        where:
        password << [
                "astrongpassphrase",
                "whatever0001",
                "123456789012",
                "a" * Passwords.MIN_LENGTH
        ]
    }

}
