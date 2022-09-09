package life.qbic.projectmanagement.offer

import spock.lang.Specification

class RandomPartSpec extends Specification {

  def "expect creation with null parameters causes an NPE"() {
    when:
    new RandomPart(null)

    then:
    thrown(NullPointerException)

  }
}
