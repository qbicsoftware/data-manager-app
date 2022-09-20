package life.qbic.projectmanagement.domain


import life.qbic.projectmanagement.domain.project.Project
import life.qbic.projectmanagement.domain.project.ProjectId
import life.qbic.projectmanagement.domain.project.ProjectIntent
import life.qbic.projectmanagement.domain.project.ProjectTitle
import spock.lang.Specification

class ProjectSpec extends Specification {

  def "expect projects with the same uuid are equal"() {
    given: "a uuid"
    UUID uuid = UUID.fromString("13c5eccd-31d4-47f0-9841-2b8865eaf458")
    def intentOne = new ProjectIntent(new ProjectTitle("A project"))
    def intentTwo = new ProjectIntent(new ProjectTitle("Another project"))

    expect: "projects with the same uuid are equal"
    def projectOne = Project.of(new ProjectId(uuid), intentOne)
    def projectTwo = Project.of(new ProjectId(uuid), intentTwo)

    projectOne == projectTwo
    projectOne.hashCode() == projectTwo.hashCode()

  }

  def "expect projects with different uuid are not equal"() {
    given: "a uuid"
    UUID uuidOne = UUID.fromString("13c5eccd-31d4-47f0-9841-2b8865eaf458")
    UUID uuidTwo = UUID.fromString("13c5eccd-31d4-47f0-0000-2b8865eaf458")
    def intent = new ProjectIntent(new ProjectTitle("A project"))

    expect: "projects with different uuid are not equal"
    def projectOne = Project.of(new ProjectId(uuidOne), intent)
    def projectTwo = Project.of(new ProjectId(uuidTwo), intent)

    projectOne != projectTwo
    projectOne.hashCode() != projectTwo.hashCode()
  }
}
