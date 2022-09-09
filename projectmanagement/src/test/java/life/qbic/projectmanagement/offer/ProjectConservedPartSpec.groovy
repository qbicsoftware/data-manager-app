package life.qbic.projectmanagement.offer

import spock.lang.Specification

class ProjectConservedPartSpec extends Specification {

  def "expect creation with null parameters causes a NullPointerException"() {
    when:
    new ProjectConservedPart(null)

    then:
    thrown(NullPointerException)

  }
}
