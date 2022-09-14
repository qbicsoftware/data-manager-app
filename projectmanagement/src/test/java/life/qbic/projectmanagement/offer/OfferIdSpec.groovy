package life.qbic.projectmanagement.offer

import life.qbic.projectmanagement.domain.offer.OfferId
import spock.lang.Specification

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
class OfferIdSpec extends Specification {

  def "expect creation from String to work"() {
    expect: "creation from String to work"
    def offerId = new OfferId("SomeValue")
    offerId != null
    offerId.value() == "SomeValue"
  }


  def "expect creation with null parameters not possible"() {
    when: "attempting to create with null parameter"
    new OfferId(null)
    then: "an exception is thrown"
    thrown(NullPointerException)
  }
}
