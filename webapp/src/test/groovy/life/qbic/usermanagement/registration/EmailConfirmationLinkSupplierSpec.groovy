package life.qbic.usermanagement.registration


import spock.lang.Specification

class EmailConfirmationLinkSupplierSpec extends Specification {

  def "when a registration link is requested, the url contains the correct path and some token"() {
    given:
    def linkSupplier = new EmailConfirmationLinkSupplier("https", "hostname", 8080, "endpoint", "confirmation-parameter")
    expect:
    linkSupplier.emailConfirmationUrl("some-user") == "https://hostname:8080/endpoint?confirmation-parameter=some-user"
  }

}
