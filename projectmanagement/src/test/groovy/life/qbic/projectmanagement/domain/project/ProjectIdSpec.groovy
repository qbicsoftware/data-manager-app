package life.qbic.projectmanagement.domain.project

import spock.lang.Specification

class ProjectIdSpec extends Specification {

    def "expect project ids cannot be created of null"() {
        when: "project ids cannot be created of null"
        ProjectId.of(null)

        then:
        thrown(RuntimeException)
    }

    def "expect two new projects are different"() {
        expect:
        ProjectId.create() != ProjectId.create()
    }

    def "expect project ids with equal uuid are equal"() {
        given:
        UUID uuid = UUID.fromString("dfb64eca-0258-4059-9603-ffbf47441b4e")

        expect: "project ids with equal uuid are equal"
        ProjectId.of(uuid) == ProjectId.of(uuid)
    }

    def "expect project ids with different uuid are different"() {
        given:
        UUID uuidOne = UUID.fromString("dfb64eca-0258-4059-9603-ffbf47441b4e")
        UUID uuidTwo = UUID.fromString("d2feeb72-6ab8-4674-a67d-92429197f31f")

        expect:
        ProjectId.of(uuidOne) != ProjectId.of(uuidTwo)
    }
}
