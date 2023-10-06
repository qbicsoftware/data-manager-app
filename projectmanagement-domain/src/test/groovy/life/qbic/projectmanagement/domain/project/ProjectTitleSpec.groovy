package life.qbic.projectmanagement.domain.project

import life.qbic.application.commons.ApplicationException
import spock.lang.Specification

import java.util.stream.Collectors
import java.util.stream.Stream

class ProjectTitleSpec extends Specification {
    def "expect creation with null parameters not possible"() {
        when:
        new ProjectTitle(null)
        then:
        thrown(NullPointerException)
    }

    def "expect creation with empty title fails"() {
        when:
        new ProjectTitle("")
        then:
        thrown(ApplicationException)
    }

    def "expect creation with exactly max chars allowed works"() {
        given: "an input with exactly the maximal amount of characters allowed"
        String inputExactlyMaxAllowedLength = maxLengthInput()
        when:
        def title = ProjectTitle.of(inputExactlyMaxAllowedLength)
        then:
        noExceptionThrown()
        Objects.nonNull(title)
    }

    def "expect creation with more characters than the max length throws RuntimeException"() {
        given: "input exceeding the max length"
        String inputExceedingMaxLength = maxLengthInput() + "a"
        when:
        ProjectTitle.of(inputExceedingMaxLength)
        then:
        thrown(RuntimeException)
    }

    private String maxLengthInput() {
        return Stream.of((0..<ProjectTitle.maxLength()).toArray())
                .map(it -> "i")
                .collect(Collectors.joining())
    }
}
