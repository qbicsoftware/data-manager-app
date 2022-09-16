package life.qbic.projectmanagement

import life.qbic.projectmanagement.project.ProjectManagementDomainException
import life.qbic.projectmanagement.project.ProjectTitle
import spock.lang.Specification

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
}
