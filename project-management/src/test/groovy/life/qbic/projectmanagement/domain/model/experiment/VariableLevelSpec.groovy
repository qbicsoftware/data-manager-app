package life.qbic.projectmanagement.domain.model.experiment


import life.qbic.projectmanagement.domain.model.experiment.ExperimentalValue
import life.qbic.projectmanagement.domain.model.experiment.ExperimentalVariable
import life.qbic.projectmanagement.domain.model.experiment.VariableLevel
import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class VariableLevelSpec extends Specification {

    def "When all variable values are part of the variable levels, successfully create the level"() {
        given:
        ExperimentalValue value = ExperimentalValue.create("test", "sameunit")
        ExperimentalValue anotherValue = ExperimentalValue.create("unknown", "sameunit")
        ExperimentalVariable variable = ExperimentalVariable.create("testVariable", value, anotherValue)

        when:
        def level = new VariableLevel(variable.name(), anotherValue)

        then:
        level.experimentalValue().equals(anotherValue)
    }


}
