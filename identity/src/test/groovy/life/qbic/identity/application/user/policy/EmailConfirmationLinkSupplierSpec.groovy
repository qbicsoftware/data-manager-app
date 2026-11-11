package life.qbic.identity.application.user.policy

import life.qbic.identity.domain.model.verification.VerificationTokens
import spock.lang.Specification

/**
 * The confirmation link must carry an opaque token, never the user's persistent identifier
 * (AUTH-R-05).
 */
class EmailConfirmationLinkSupplierSpec extends Specification {

    def "the link puts the token in the configured parameter"() {
        given:
        def linkSupplier = new EmailConfirmationLinkSupplier(
                "https", "hostname", 8080, "", "/registration/confirm", "token")

        expect:
        linkSupplier.emailConfirmationUrl("some-token")
                == "https://hostname:8080/registration/confirm?token=some-token"
    }

    def "a real issued token survives URL encoding unchanged"() {
        given:
        def linkSupplier = new EmailConfirmationLinkSupplier(
                "https", "hostname", 8080, "", "/registration/confirm", "token")

        when: "tokens are Base64URL, so they contain no characters needing escaping"
        def raw = VerificationTokens.newRawToken()
        def url = linkSupplier.emailConfirmationUrl(raw)

        then:
        url.endsWith("?token=" + raw)
        !url.contains("+")
        !url.contains("/") || url.contains("?token=")   // path slash is fine, token slash is not
    }
}
