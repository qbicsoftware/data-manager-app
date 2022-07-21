package life.qbic.newsreader.usermanagement.passwordreset


import spock.lang.Specification

class PasswordResetLinkSupplierSpec extends Specification {

    def "when a registration link is requested, the url contains the correct path and some token"() {
        given:
        def linkSupplier = new PasswordResetLinkSupplier("https", "hostname", 8080, "endpoint", "confirmation-parameter")
        expect:
        linkSupplier.passwordResetUrl("some-user") == "https://hostname:8080/endpoint?confirmation-parameter=some-user"
    }

}
