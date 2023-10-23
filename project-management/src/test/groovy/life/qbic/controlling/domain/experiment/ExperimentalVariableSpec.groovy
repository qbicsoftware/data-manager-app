package life.qbic.controlling.domain.experiment

import life.qbic.controlling.domain.model.experiment.ExperimentalValue
import life.qbic.controlling.domain.model.experiment.ExperimentalVariable
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
        variable.levels().get(0).experimentalValue.equals(ExperimentalValue.create("Simple", "cm"))
    }

    def "Created levels are provided with the variable name"() {
        when:
        def variable = ExperimentalVariable.create("environment", ExperimentalValue.create("control"), ExperimentalValue.create("altered"))
        then:
        variable.levels().every { it.variableName() == variable.name() }
        noExceptionThrown()
    }

    def "Adding a variable containing levels with different units via the ExperimentInformationService fails"() {
        given:
        String variableName = "My awesome variable"
        String unit = "This unit exists"
        def levels = ["level 1", "level 2"]
        ExperimentalValue experimentalValue1 = ExperimentalValue.create(levels[0], unit)
        ExperimentalValue experimentalValueNoUnit = ExperimentalValue.create(levels[1])

        when: "variables with disjunctive units are added to an experiment"

        ExperimentalVariable.create(variableName, experimentalValue1, experimentalValueNoUnit)

        then: "illegal argument exception is thrown"
        thrown(IllegalArgumentException)
    }

}
