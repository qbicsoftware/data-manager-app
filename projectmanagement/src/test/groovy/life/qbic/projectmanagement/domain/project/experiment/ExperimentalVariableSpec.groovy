package life.qbic.projectmanagement.domain.project.experiment

import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class ExperimentalVariableSpec extends Specification {

    def "If no levels are provided, throw an IllegalArgumentException"() {
        when:
        new ExperimentalVariable("Test")


        then:
        thrown(IllegalArgumentException)
    }

    def "If at least one level is provided, create the experimental variable"() {
        when:
        def variable = ExperimentalVariable.create("Test", ExperimentalValue.create("Simple", "cm"))

        then:
        noExceptionThrown()
        variable.name().value().equals("Test")
        variable.levels().get(0).equals(ExperimentalValue.create("Simple", "cm"))
    }

    def "Created levels are provided with the variable name"() {
        given:
        def variable = ExperimentalVariable.create("environment", ExperimentalValue.create("control"), ExperimentalValue.create("altered"))
        when:
        def level = variable.getLevel(ExperimentalValue.create("altered"))
        then:
        level.variableName() == variable.name()
        noExceptionThrown()
    }

}
