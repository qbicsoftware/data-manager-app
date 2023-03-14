package life.qbic.projectmanagement.domain.project.experiment

import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class ConditionSpec extends Specification {

    def "If an experimental variable is defined in the condition, return its configured level"() {
        given:
        ExperimentalValue experimentalValue = ExperimentalValue.create("10", "cm")
        ExperimentalVariable experimentalVar = ExperimentalVariable.create("test variable", experimentalValue)
        def level = new VariableLevel(experimentalVar, experimentalValue)
        def condition = Condition.create("my condition", level)

        when:
        Optional<ExperimentalValue> result = condition.valueOf(experimentalVar.name().value())

        then:
        result.isPresent()
        result.get().equals(experimentalValue)
    }

    def "If an experimental variable is not part of the condition, return an empty result"() {
        given:
        ExperimentalValue experimentalValue = ExperimentalValue.create("10", "cm")
        ExperimentalVariable experimentalVar = ExperimentalVariable.create("test variable", experimentalValue)
        def level = new VariableLevel(experimentalVar, experimentalValue)
        def condition = Condition.create("my condition", level)

        when:
        Optional<ExperimentalValue> result = condition.valueOf("unknown variable")

        then:
        result.isEmpty()
    }


    def "If variable levels origin from the same experimental variables, return an exception"() {
        given:
        ExperimentalValue experimentalValue = ExperimentalValue.create("10", "cm")
        ExperimentalVariable experimentalVar = ExperimentalVariable.create("test variable", experimentalValue)
        ExperimentalVariable experimentalVar2 = ExperimentalVariable.create("test variable", experimentalValue)

        def level = new VariableLevel(experimentalVar, experimentalValue)
        def level2 = new VariableLevel(experimentalVar2, experimentalValue)

        when:
        Condition.create("my condition", level, level2)

        then:
        thrown(IllegalArgumentException)
    }

    def "If the number of provided variable levels is less than one, throw an IllegalArgumentException" () {
        when:
        Condition.create("my condition")

        then:
        thrown(IllegalArgumentException)

    }

}
