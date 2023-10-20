package life.qbic.controlling.domain


import spock.lang.Specification

class ProjectCodeSpec extends Specification {

    def "Calling the random factory method creates a new random code"() {
        when:
        life.qbic.controlling.domain.model.project.ProjectCode code = life.qbic.controlling.domain.model.project.ProjectCode.random();
        life.qbic.controlling.domain.model.project.ProjectCode code2 = life.qbic.controlling.domain.model.project.ProjectCode.random();

        then:
        code.value().length() == 5
        code.value().startsWith("Q")
        code2.value().length() == 5
        code2.value().startsWith("Q")
        code != code2
    }

    def "Parsing a code with a blacklisted expression throws an IllegalArgumentException"() {
        when:
        life.qbic.controlling.domain.model.project.ProjectCode.parse("Q" + blacklistedExpression as String)

        then:
        thrown(IllegalArgumentException)

        where:
        blacklistedExpression << [life.qbic.controlling.domain.model.project.ProjectCode.BLACKLIST]
    }

    def "Parsing a project code with a valid expression returns its object oriented form"() {
        when:
        life.qbic.controlling.domain.model.project.ProjectCode code = life.qbic.controlling.domain.model.project.ProjectCode.parse("Q" + "ABCD")

        then:
        code.value().contains("ABCD")
    }

    def "Parsing a project code with a invalid length throws an IllegalArgumentException"() {
        when:
        life.qbic.controlling.domain.model.project.ProjectCode.parse("Q" + wrongLength)

        then:
        thrown(IllegalArgumentException)

        where:
        wrongLength << ["ABC", "12345"]
    }

    def "Parsing a project code with a invalid character throws an IllegalArgumentException"() {
        when:
        life.qbic.controlling.domain.model.project.ProjectCode.parse("Q" + invalidChar)

        then:
        thrown(IllegalArgumentException)

        where:
        invalidChar << ["ABCÖ", "YABC", "~TES"]
    }


}
