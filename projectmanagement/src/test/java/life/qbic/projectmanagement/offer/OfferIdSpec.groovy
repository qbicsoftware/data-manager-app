package life.qbic.projectmanagement.offer

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
    def offerId = OfferId.from("O_koch_abcd_1")
    offerId != null
    offerId.projectConservedPart() == new ProjectConservedPart("koch")
    offerId.randomPart() == new RandomPart("abcd")
    offerId.version() == new Version(1)
  }


  def "expect creation with null parameters not possible"() {
    when: "attempting to create with null parameter"
    new OfferId(null, null, null)
    then: "an exception is thrown"
    thrown(NullPointerException)
  }
}
