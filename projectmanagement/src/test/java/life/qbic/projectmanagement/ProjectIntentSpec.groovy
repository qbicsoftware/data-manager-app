package life.qbic.projectmanagement

import life.qbic.projectmanagement.domain.project.ProjectIntent2
import spock.lang.Specification

class ProjectIntentSpec extends Specification {

  def "expect creation with null parameters not possible"() {
    when: "creation with null parameters is attempted"
    new ProjectIntent2(null)
    then: "an exception is thrown"
    thrown(NullPointerException)
  }
}
