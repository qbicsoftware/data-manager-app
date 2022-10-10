package life.qbic.projectmanagement.domain.project

import spock.lang.Specification

class ProjectCodeSpec extends Specification {

    def "Calling the random factory method creates a new random code"() {
        when:
        ProjectCode code = ProjectCode.random();
        ProjectCode code2 = ProjectCode.random();

        then:
        code.value().length() == 5
        code.value().startsWith("Q")
        code2.value().length() == 5
        code2.value().startsWith("Q")
        code != code2
    }

    def "Parsing a code with a blacklisted expression throws an IllegalArgumentException"() {
        when:
        ProjectCode.parse("Q" + blacklistedExpression as String)

        then:
        thrown(IllegalArgumentException)

        where:
        blacklistedExpression << [ProjectCode.BLACKLIST]
    }

    def "Parsing a project code with a valid expression returns its object oriented form"() {
        when:
        ProjectCode code = ProjectCode.parse("Q" + "ABCD")

        then:
        code.value().contains("ABCD")
    }

    def "Parsing a project code with a invalid length throws an IllegalArgumentException"() {
        when:
        ProjectCode.parse("Q" + wrongLength)

        then:
        thrown(IllegalArgumentException)

        where:
        wrongLength << ["ABC", "12345"]
    }



}
