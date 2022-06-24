package life.qbic.usermanagement.registration

import life.qbic.usermanagement.registration.EmailConfirmationLinkSupplier
import spock.lang.Specification

class EmailConfirmationLinkSupplierSpec extends Specification {

  def "when a registration link is requested, the url contains the correct path and some token"() {
    given:
    def linkSupplier = new EmailConfirmationLinkSupplier("hostname", "0007", "endpoint", "confirmation-parameter")
    expect:
    linkSupplier.emailConfirmationUrl("some-user").startsWith("hostname:0007/endpoint?confirmation-parameter=")
  }

}
