package life.qbic.usermanagement.registration

import spock.lang.Specification

class EmailConfirmationLinkSupplierSpec extends Specification {

  def "when a registration link is requested, the url contains the correct path and some token"() {
    given:
    def linkSupplier = new EmailConfirmationLinkSupplier("hostname", "0007", "confirmation-endpoint")
    expect:
    linkSupplier.emailConfirmationUrl("some-user").startsWith("hostname:0007/confirmation-endpoint/")
  }

}
