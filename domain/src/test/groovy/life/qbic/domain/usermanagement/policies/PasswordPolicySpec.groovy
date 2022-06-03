package life.qbic.domain.usermanagement.policies


import spock.lang.Specification

/**
 * <b>Tests for the {@link PasswordPolicy}</b>
 *
 * @since 1.0.0
 */
class PasswordPolicySpec extends Specification {

    def "Given a password length shorter than 8 characters, the policy check shall fail"() {
        when:
        PolicyCheckReport report = PasswordPolicy.create().validate(password.toCharArray())

        then:
        report.status() == PolicyStatus.FAILED
        report.reason().equalsIgnoreCase("EncryptedPassword shorter than 8 characters.")

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
        PolicyCheckReport report = PasswordPolicy.create().validate(password.toCharArray())

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
