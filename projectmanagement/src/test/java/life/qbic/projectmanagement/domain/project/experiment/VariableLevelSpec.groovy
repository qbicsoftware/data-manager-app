package life.qbic.projectmanagement.domain.project.experiment


import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class VariableLevelSpec extends Specification {

    def "When at least one variable value is not part of a Variable level, throw an exception"() {
        given:
        ExperimentalValue value = ExperimentalValue.create("test", "unit")
        ExperimentalValue unknownValue = ExperimentalValue.create("unknown", "nothing")
        ExperimentalVariable variable = new ExperimentalVariable<>("testVariable", value)

        when:
        new VariableLevel<>(variable, unknownValue)

        then:
        thrown(UnknownVariableLevelException)
    }

    def "When all variable values are part of the variable levels, successfully create the level"() {
        given:
        ExperimentalValue value = ExperimentalValue.create("test", "unit")
        ExperimentalValue anotherValue = ExperimentalValue.create("unknown", "nothing")
        ExperimentalVariable variable = new ExperimentalVariable<>("testVariable", value, anotherValue)

        when:
        def level = new VariableLevel<>(variable, anotherValue)

        then:
        level.experimentalValue().equals(anotherValue)
    }


}
