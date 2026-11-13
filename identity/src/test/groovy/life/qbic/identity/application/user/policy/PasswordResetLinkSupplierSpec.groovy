package life.qbic.identity.application.user.policy

import life.qbic.identity.domain.model.verification.VerificationTokens
import spock.lang.Specification

/**
 * The reset link grants control of an existing account, so it must carry a single-use token and
 * never the user's persistent identifier (AUTH-R-05).
 */
class PasswordResetLinkSupplierSpec extends Specification {

    def "the link puts the token in the configured parameter"() {
        given:
        def linkSupplier = new PasswordResetLinkSupplier(
                "https", "hostname", 8080, "", "/registration/new-password", "token")

        expect:
        linkSupplier.passwordResetUrl("some-token")
                == "https://hostname:8080/registration/new-password?token=some-token"
    }

    def "a real issued token survives URL encoding unchanged"() {
        given:
        def linkSupplier = new PasswordResetLinkSupplier(
                "https", "hostname", 8080, "", "/registration/new-password", "token")

        when:
        def raw = VerificationTokens.newRawToken()
        def url = linkSupplier.passwordResetUrl(raw)

        then:
        url.endsWith("?token=" + raw)
    }
}
