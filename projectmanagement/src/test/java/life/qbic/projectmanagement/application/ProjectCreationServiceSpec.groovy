package life.qbic.projectmanagement.application


import life.qbic.projectmanagement.domain.ProjectRepository
import spock.lang.Specification

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
class ProjectCreationServiceSpec extends Specification {

  ProjectRepository projectRepository = Stub()
  ProjectCreationService projectCreationService = new ProjectCreationService(projectRepository)

  def "expect null input will cause a failure response"() {
    given:
    projectRepository.add(_) >> {}
    when: "null input is provided"
    def response = projectCreationService.createProject(null)
    then: "the response is not successful"
    !response.isSuccess()
  }

  def "expect an empty title to fail"() {
    given:
    projectRepository.add(_) >> {}
    when: "empty title is provided"
    def response = projectCreationService.createProject("")

    then: "the response is not successful"
    !response.isSuccess()
  }

  def "expect project creation is successful for a non-empty title"() {
    given:
    projectRepository.add(_) >> {}
    when: "a project is created with a non-empty title"
    def response = projectCreationService.createProject("test")

    then: "the response is a success"
    response.isSuccess()
    response.projectCreatedEvent().isPresent()
  }

  def "expect unsuccessful save of a new project causes failure response"() {
    given:
    projectRepository.add(_) >> { throw new RuntimeException("expected exception") }

    when:
    def response = projectCreationService.createProject("test")

    then:
    !response.isSuccess()
  }
}
