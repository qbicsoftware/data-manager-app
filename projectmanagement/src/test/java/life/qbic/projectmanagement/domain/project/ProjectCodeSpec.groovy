package life.qbic.projectmanagement.domain.project

import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class ProjectCodeSpec extends Specification {

    def "Calling the factory method creates a new random code"() {
        when:
        ProjectCode code = ProjectCode.random();

        then:
        code.value().length() == 5
        code.value().startsWith("Q")
    }

}
