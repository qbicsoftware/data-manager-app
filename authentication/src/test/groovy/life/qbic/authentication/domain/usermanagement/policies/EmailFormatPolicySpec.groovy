package life.qbic.authentication.domain.usermanagement.policies


import life.qbic.authentication.domain.policy.PolicyCheckReport
import life.qbic.authentication.domain.policy.PolicyStatus
import life.qbic.authentication.domain.user.policy.EmailFormatPolicy
import spock.lang.Specification

/**
 * <b>Tests for the {@link EmailFormatPolicy}</b>
 *
 * @since 1.0.0
 */
class EmailFormatPolicySpec extends Specification {

    def "An email format that violates the RFC5322 specification shall result in a FAILED policy check"() {
        when:
        PolicyCheckReport policyCheckReport = EmailFormatPolicy.instance().validate(email as String)

        then:
        policyCheckReport.status() == PolicyStatus.FAILED
        policyCheckReport.reason().equalsIgnoreCase("Invalid mail address format.")

        where:
        email << [
                "my@ohmy",
                "@test.de",
                "address.de",
                "my@address"
        ]
    }

    def "An email format that honors the RFC5322 specification shall result in a PASSED policy check"() {
        when:
        PolicyCheckReport policyCheckReport = EmailFormatPolicy.instance().validate(email as String)

        then:
        policyCheckReport.status() == PolicyStatus.PASSED
        policyCheckReport.reason().isBlank()

        where:
        email << [
                "valid.address@example.com",
                "address@subdomain.domain.de"
        ]
    }

}
