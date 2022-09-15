package life.qbic.projectmanagement.application


import life.qbic.projectmanagement.domain.ProjectRepository
import spock.lang.Specification

import static java.util.Objects.nonNull

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

  def "expect null input will cause an exception"() {
    given:
    projectRepository.add(_) >> {}
    when: "null input is provided"
    projectCreationService.createProject(null)
    then: "the response is not successful"
    thrown(RuntimeException)
  }

  def "expect an empty title to fail"() {
    given:
    projectRepository.add(_) >> {}
    when: "empty title is provided"
    projectCreationService.createProject("")

    then: "an exception is thrown"
    thrown(Exception)
  }

  def "expect project creation is successful for a non-empty title"() {
    given:
    projectRepository.add(_) >> {}
    when: "a project is created with a non-empty title"
    def project = projectCreationService.createProject("test")

    then: "the created project is returned"
    nonNull(project)

  }

  def "expect unsuccessful save of a new project causes failure response"() {
    given:
    projectRepository.add(_) >> { throw new RuntimeException("expected exception") }

    when:
    projectCreationService.createProject("test")

    then:
    thrown(Exception)
  }
}
