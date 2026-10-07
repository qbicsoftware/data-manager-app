package life.qbic.identity.domain.user

import life.qbic.identity.api.UserNames
import life.qbic.identity.domain.model.UserNamePolicy
import life.qbic.identity.domain.model.policy.PolicyStatus
import spock.lang.Specification

/**
 * <b>Tests for the {@link UserNamePolicy}</b>
 *
 * @since 1.20.0
 */
class UserNamePolicySpec extends Specification {

    def "Given a username shorter than or equal to the maximum length, the policy check shall pass"() {
        when:
        def report = UserNamePolicy.instance().validate(userName)

        then:
        report.status() == PolicyStatus.PASSED
        report.reason().isBlank()

        where:
        userName << [
                "a",
                "jdoe",
                "a" * UserNames.MAX_LENGTH,
                "   padded-name   ".trim(),
                // Exactly at the limit after stripping surrounding whitespace.
                " " + ("a" * UserNames.MAX_LENGTH) + " ",
        ]
    }

    def "Given a username longer than the maximum length, the policy check shall fail"() {
        when:
        def report = UserNamePolicy.instance().validate(userName)

        then:
        report.status() == PolicyStatus.FAILED
        report.reason().equalsIgnoreCase(
                "Username must not exceed " + UserNames.MAX_LENGTH + " characters.")

        where:
        userName << [
                "a" * (UserNames.MAX_LENGTH + 1),
                "a" * (UserNames.MAX_LENGTH * 3),
                // Surrounding whitespace must not be able to sneak past the limit.
                " " + ("a" * (UserNames.MAX_LENGTH + 1)) + " ",
        ]
    }

    def "Given an empty or null username, the policy check shall fail"() {
        when:
        def report = UserNamePolicy.instance().validate(userName)

        then:
        report.status() == PolicyStatus.FAILED
        report.reason().equalsIgnoreCase("Username must not be empty.")

        where:
        userName << [null, "", "   "]
    }

    def "A persisted username longer than the maximum length shall not be rejected on read"() {
        given: "a grandfathered username that predates the length limit"
        def legacyUserName = "a-very-long-legacy-username-well-past-the-limit"

        when: "the value is inspected by the policy"
        def exceeds = UserNamePolicy.exceedsMaxLength(legacyUserName)

        then: "it is reported as over-long instead of throwing"
        exceeds

        and: "a compliant username is not reported"
        !UserNamePolicy.exceedsMaxLength("jdoe")
    }
}
