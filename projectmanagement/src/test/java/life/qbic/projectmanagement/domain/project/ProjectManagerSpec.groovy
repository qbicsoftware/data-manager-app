package life.qbic.projectmanagement.domain.project

import spock.lang.Specification

class ProjectManagerSpec extends Specification {

    def "expect project managers cannot be created of null"() {
        when:
        ProjectManager.of(null)

        then:
        thrown(RuntimeException)
    }

    def "expect project managers provide the full name of which they were created"() {
        given:
        String name = "Frank Sinatra"

        expect:
        ProjectManager.of(name).fullName() == name

    }

    def "expect project managers with equal names are equal"() {
        given:
        String fullName = "John Doe"

        expect:
        ProjectManager.of(fullName) == ProjectManager.of(fullName)
    }

    def "expect managers with different fullname are different"() {
        given:
        String fullNameOne = "John Doe"
        String fullNameTwo = "Jane Dutton"

        expect:
        ProjectManager.of(fullNameOne) != ProjectManager.of(fullNameTwo)
    }
}
