package life.qbic.usermanagement.passwordreset

import life.qbic.usermanagement.registration.EmailConfirmationLinkSupplier
import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class PasswordResetLinkSupplierSpec extends Specification {

    def "when a registration link is requested, the url contains the correct path and some token"() {
        given:
        def linkSupplier = new PasswordResetLinkSupplier("https", "hostname", 8080, "endpoint", "confirmation-parameter")
        expect:
        linkSupplier.passwordResetUrl("some-user").startsWith("https://hostname:8080/endpoint?confirmation-parameter=")
    }

}
