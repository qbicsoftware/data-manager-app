package life.qbic.projectmanagement

import life.qbic.projectmanagement.domain.ProjectIntent
import spock.lang.Specification

class ProjectIntentSpec extends Specification {

  def "expect creation with null parameters not possible"() {
    when: "creation with null parameters is attempted"
    new ProjectIntent(null)
    then: "an exception is thrown"
    thrown(NullPointerException)
  }
}
