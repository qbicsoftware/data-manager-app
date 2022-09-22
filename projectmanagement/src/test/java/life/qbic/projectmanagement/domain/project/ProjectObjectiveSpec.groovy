package life.qbic.projectmanagement.domain.project

import spock.lang.Specification

class ProjectObjectiveSpec extends Specification {
  def "expect creation with null throws RuntimeException"() {
    when: "creation with null throws RuntimeException"
    ProjectObjective.create(null)
    then:
    thrown(RuntimeException)
  }

  def "expect two project objectives with equal value to be equal"() {
    expect: "two project objectives with equal value to be equal"
    ProjectObjective.create("some equal value") == ProjectObjective.create("some equal value")
  }

  def "expect two project objectives with non-equal value to not be equal"() {
    expect: "two project objectives with non-equal value to not be equal"
    ProjectObjective.create("some first value") != ProjectObjective.create("some second value")
  }
}
