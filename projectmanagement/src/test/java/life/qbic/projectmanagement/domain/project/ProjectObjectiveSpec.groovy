package life.qbic.projectmanagement.domain.project

import spock.lang.Specification

import java.util.stream.Collectors
import java.util.stream.Stream

class ProjectObjectiveSpec extends Specification {

    def "expect creation with null throws RuntimeException"() {
        when: "creation with null throws RuntimeException"
        ProjectObjective.create(null)
        then:
        thrown(RuntimeException)
    }

    def "expect two project objectives with equal value to be equal"() {
        expect: "two project objectives with equal value to be equal"
        ProjectObjective.create("some equal value") == ProjectObjective.create("some equal value")
    }

    def "expect two project objectives with non-equal value to not be equal"() {
        expect: "two project objectives with non-equal value to not be equal"
        ProjectObjective.create("some first value") != ProjectObjective.create("some second value")
    }

    def "expect project objective creation from input with max length to not throw an< exception"() {
        given: "an input with exactly max length length"
        String input = maxLengthInput()
        when:
        ProjectObjective.create(input)
        then:
        noExceptionThrown()
    }

    def "expect project objective creation from input exceeding max length to throw RuntimeException"() {
        given: "an input exceeding the maximal length"
        String input = maxLengthInput() + "a";
        when:
        ProjectObjective.create(input)
        then:
        thrown(RuntimeException)
    }

    private String maxLengthInput() {
        return Stream.of((0..<ProjectObjective.maxLength()).toArray())
                .map(it -> "i")
                .collect(Collectors.joining())
    }
}
