package life.qbic.projectmanagement.domain

import life.qbic.projectmanagement.domain.model.project.*
import spock.lang.Specification

class ProjectSpec extends Specification {

    def "expect project creation fails for null project intent"() {
        when: "project creation fails for null project intent"
        Project.create(null)
        then:
        thrown(RuntimeException)
    }

    def "expect projects with the same uuid are equal"() {
        given: "a uuid"
        UUID uuid = UUID.fromString("13c5eccd-31d4-47f0-9841-2b8865eaf458")
        ProjectIntent intentOne = ProjectIntent.of(new ProjectTitle("A project"), new ProjectObjective("an objective"))
        ProjectIntent intentTwo = ProjectIntent.of(new ProjectTitle("Another project"), new ProjectObjective("another objective"))

        expect: "projects with the same uuid are equal"
        def projectOne = Project.of(ProjectId.of(uuid), intentOne, ProjectCode.random(), new Contact("my name", "some@email.de", "", ""), new Contact("my name2", "some@email.de", "", ""), null)
        def projectTwo = Project.of(ProjectId.of(uuid), intentTwo, ProjectCode.random(), new Contact("my name", "some@email.de", "", ""), new Contact("my name2", "some@email.de", "", ""), null)

        projectOne == projectTwo
        projectOne.hashCode() == projectTwo.hashCode()
    }

    def "expect projects with different uuid are not equal"() {
        given: "a uuid"
        UUID uuidOne = UUID.fromString("13c5eccd-31d4-47f0-9841-2b8865eaf458")
        UUID uuidTwo = UUID.fromString("13c5eccd-31d4-47f0-0000-2b8865eaf458")
        def intent = ProjectIntent.of(ProjectTitle.of("A project"), ProjectObjective.create("an objective"))

        expect: "projects with different uuid are not equal"
        def projectOne = Project.of(ProjectId.of(uuidOne), intent, ProjectCode.random(), new Contact("my name", "some@email.de", "", ""), new Contact("my name2", "some@email.de", "", ""), null)
        def projectTwo = Project.of(ProjectId.of(uuidTwo), intent, ProjectCode.random(), new Contact("my name", "some@email.de", "", ""), new Contact("my name2", "some@email.de", "", ""), null)


        projectOne != projectTwo
        projectOne.hashCode() != projectTwo.hashCode()
    }
}
