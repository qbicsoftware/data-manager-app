package life.qbic.projectmanagement.domain.project


import spock.lang.Specification

import java.util.stream.Collectors
import java.util.stream.Stream

class ProjectTitleSpec extends Specification {
  def "expect creation with null parameters not possible"() {
    when:
    new ProjectTitle(null)
    then:
    thrown(NullPointerException)
  }

  def "expect creation with empty title fails"() {
    when:
    new ProjectTitle("")
    then:
    thrown(ProjectManagementDomainException)
  }

  def "expect creation with more characters than the max length throws RuntimeException"() {
    given: "input exceeding the max length"
    String inputExceedingMaxLength = maxLengthInput() + "a"
    when:
    ProjectTitle.create(inputExceedingMaxLength)
    then:
    thrown(RuntimeException)

  }

  private String maxLengthInput() {
    return Stream.of(0..<ProjectTitle.maxLength())
            .map(it -> "i")
            .collect(Collectors.joining())
  }
}
