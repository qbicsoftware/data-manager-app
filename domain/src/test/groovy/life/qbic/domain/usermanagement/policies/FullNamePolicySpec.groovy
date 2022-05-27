package life.qbic.domain.usermanagement.policies


import spock.lang.Specification

/**
 * <b>Tests for the {@link FullNamePolicy}</b>
 *
 * @since 1.0.0
 */
class FullNamePolicySpec extends Specification {

  def "A full name that has not the required length shall result in a FAILED policy check"() {
    when:
    PolicyCheckReport policyCheckReport = FullNamePolicy.create().validate(fullName as String)

    then:
    policyCheckReport.status() == PolicyStatus.FAILED
    policyCheckReport.reason().equalsIgnoreCase("Full Name shorter than 1 character.")

    where:
    fullName << [
            ""
    ]
  }

  def "A full name that has the required length shall result in a PASSED policy check"() {
    when:
    PolicyCheckReport policyCheckReport = FullNamePolicy.create().validate(fullName as String)

    then:
    policyCheckReport.status() == PolicyStatus.PASSED
    policyCheckReport.reason().isBlank()

    where:
    fullName << [
            "John Doe",
            "Jane Van Damme"
    ]
  }

}
