package life.qbic.projectmanagement.application

import life.qbic.application.commons.ApplicationException
import life.qbic.application.commons.Result
import life.qbic.projectmanagement.domain.project.Project
import life.qbic.projectmanagement.domain.project.repository.ProjectRepository
import spock.lang.Specification

import static java.util.Objects.nonNull

class ProjectCreationServiceSpec extends Specification {

  ProjectRepository projectRepository = Stub()
  ProjectCreationService projectCreationService = new ProjectCreationService(projectRepository)

  def "invalid project title leads to INVALID_PROJECT_TITLE code"() {
    given:
    projectRepository.add(_) >> {}
    when: "null input is provided"
    Result<Project, ApplicationException> result = projectCreationService.createProject(null, "objective")
    Result<Project, ApplicationException> resultWithExperimentalDesign = projectCreationService.createProjectWithExperimentalDesign(null, "objective", "design")
    then: "an exception is thrown"
    result.isFailure()
    resultWithExperimentalDesign.isFailure()
    result.exception().errorCode() == ApplicationException.ErrorCode.INVALID_PROJECT_TITLE
    resultWithExperimentalDesign.exception().errorCode() == ApplicationException.ErrorCode.INVALID_PROJECT_TITLE
  }

  def "invalid objective leads to INVALID_PROJECT_OBJECTIVE code"() {
    given:
    projectRepository.add(_) >> {}
    when: "null input is provided"
    Result<Project, ApplicationException> result = projectCreationService.createProject("title", null)
    Result<Project, ApplicationException> resultWithExperimentalDesign = projectCreationService.createProjectWithExperimentalDesign("title", null, "design")
    then: "an exception is thrown"
    result.isFailure()
    resultWithExperimentalDesign.isFailure()
    result.exception().errorCode() == ApplicationException.ErrorCode.INVALID_PROJECT_OBJECTIVE
    resultWithExperimentalDesign.exception().errorCode() == ApplicationException.ErrorCode.INVALID_PROJECT_OBJECTIVE
  }

  def "invalid experimental design description leads to INVALID_EXPERIMENTAL_DESIGN code"() {
    given:
    projectRepository.add(_) >> {}
    when: "null input is provided"
    Result<Project, ApplicationException> resultWithExperimentalDesign = projectCreationService.createProjectWithExperimentalDesign("title", "objective", null)
    then: "an exception is thrown"
    resultWithExperimentalDesign.isFailure()
    resultWithExperimentalDesign.exception().errorCode() == ApplicationException.ErrorCode.INVALID_EXPERIMENTAL_DESIGN
  }

  def "expect project creation returns the created project for a non-empty title"() {
    given:
    projectRepository.add(_) >> {}
    when: "a project is created with a non-empty title"
    def result = projectCreationService.createProject("test", "objective")
    def resultWithExperimentalDesign = projectCreationService.createProject("test", "objective")

    then: "the created project is returned"
    result.isSuccess()
    resultWithExperimentalDesign.isSuccess()
    nonNull(result.value())
    nonNull(resultWithExperimentalDesign.value())
  }

  def "expect unsuccessful save of a new project returns GENERAL error code"() {
    given:
    projectRepository.add(_) >> { throw new RuntimeException("expected exception") }

    when:
    Result<Project, ApplicationException> result = projectCreationService.createProject("test", "objective")
    Result<Project, ApplicationException> resultWithExperimentalDesign = projectCreationService.createProject("test", "objective")

    then:
    result.isFailure()
    resultWithExperimentalDesign.isFailure()
    result.exception().errorCode() == ApplicationException.ErrorCode.GENERAL
    resultWithExperimentalDesign.exception().errorCode() == ApplicationException.ErrorCode.GENERAL
  }
}
