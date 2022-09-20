package life.qbic.projectmanagement


import life.qbic.projectmanagement.domain.project.Project2
import life.qbic.projectmanagement.domain.project.ProjectIntent2
import life.qbic.projectmanagement.domain.project.ProjectTitle
import spock.lang.Specification

/**
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
class ProjectSpec extends Specification {

  def "expect projects with the same uuid are equal"() {
    given: "a uuid"
    UUID uuid = UUID.fromString("13c5eccd-31d4-47f0-9841-2b8865eaf458")
    def intentOne = new ProjectIntent2(new ProjectTitle("A project"))
    def intentTwo = new ProjectIntent2(new ProjectTitle("Another project"))

    expect: "projects with the same uuid are equal"
    def projectOne = Project2.of(uuid, intentOne)
    def projectTwo = Project2.of(uuid, intentTwo)

    projectOne == projectTwo
    projectOne.hashCode() == projectTwo.hashCode()

  }

  def "expect projects with different uuid are not equal"() {
    given: "a uuid"
    UUID uuidOne = UUID.fromString("13c5eccd-31d4-47f0-9841-2b8865eaf458")
    UUID uuidTwo = UUID.fromString("13c5eccd-31d4-47f0-0000-2b8865eaf458")
    def intent = new ProjectIntent2(new ProjectTitle("A project"))

    expect: "projects with different uuid are not equal"
    def projectOne = Project2.of(uuidOne, intent)
    def projectTwo = Project2.of(uuidTwo, intent)

    projectOne != projectTwo
    projectOne.hashCode() != projectTwo.hashCode()
  }
}
