package life.qbic.projectmanagement.application

import life.qbic.application.commons.ApplicationException
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository
import spock.lang.Specification

import static java.util.Objects.nonNull

class ProjectCreationServiceSpec extends Specification {

  ProjectRepository projectRepository = Stub()
  ProjectCreationService projectCreationService = new ProjectCreationService(projectRepository)

  def "expect null input will cause an exception"() {
    given:
    projectRepository.add(_) >> {}
    when: "null input is provided"
    projectCreationService.createProject(null)
    then: "an exception is thrown"
    def e = thrown(ProjectManagementException)
    e.errorCode() == ApplicationException.ErrorCode.INVALID_PROJECT_TITLE
  }

  def "expect an empty title will cause an exception"() {
    given:
    projectRepository.add(_) >> {}
    when: "empty title is provided"
    projectCreationService.createProject("")

    then: "an exception is thrown"
    def e = thrown(ProjectManagementException)
    e.errorCode() == ApplicationException.ErrorCode.INVALID_PROJECT_TITLE

  }

  def "expect project creation returnes the created project for a non-empty title"() {
    given:
    projectRepository.add(_) >> {}
    when: "a project is created with a non-empty title"
    def project = projectCreationService.createProject("test")

    then: "the created project is returned"
    nonNull(project)

  }

  def "expect unsuccessful save of a new project throws an exception"() {
    given:
    projectRepository.add(_) >> { throw new RuntimeException("expected exception") }

    when:
    projectCreationService.createProject("test")

    then:
    def e = thrown(ProjectManagementException)
    e.errorCode() == ApplicationException.ErrorCode.GENERAL
  }
}
