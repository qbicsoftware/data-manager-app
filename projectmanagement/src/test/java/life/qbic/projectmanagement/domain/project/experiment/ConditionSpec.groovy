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
        ExperimentalVariable<ExperimentalValue> experimentalVar = new ExperimentalVariable<>("test variable", experimentalValue)
        def level = new VariableLevel<>(experimentalVar, experimentalValue)
        def condition = Condition.createForExperiment(level)

        when:
        Optional<ExperimentalValue> result = condition.valueOf(experimentalVar)

        then:
        result.isPresent()
        result.get().equals(experimentalValue)
    }

    def "If an experimental variable is not part of the condition, return an empty result"() {
        given:
        ExperimentalValue experimentalValue = ExperimentalValue.create("10", "cm")
        ExperimentalVariable<ExperimentalValue> experimentalVar = new ExperimentalVariable<>("test variable", experimentalValue)
        def level = new VariableLevel<>(experimentalVar, experimentalValue)
        def condition = Condition.createForExperiment(level)

        when:
        Optional<ExperimentalValue> result = condition.valueOf(new ExperimentalVariable<ExperimentalValue>("unknown variable", experimentalValue))

        then:
        result.isEmpty()
    }

    def "Return all experimental variables when requested"() {
        given:
        ExperimentalValue experimentalValue = ExperimentalValue.create("10", "cm")
        ExperimentalVariable<ExperimentalValue> experimentalVar = new ExperimentalVariable<>("test variable", experimentalValue)
        ExperimentalVariable<ExperimentalValue> experimentalVar2 = new ExperimentalVariable<>("another variable", experimentalValue)
        ExperimentalVariable<ExperimentalValue> experimentalVar3 = new ExperimentalVariable<>("yet another variable", experimentalValue)

        def level = new VariableLevel<>(experimentalVar, experimentalValue)
        def level2 = new VariableLevel<>(experimentalVar2, experimentalValue)
        def level3 = new VariableLevel<>(experimentalVar3, experimentalValue)

        def condition = Condition.createForExperiment(level, level2, level3)

        when:
        List<ExperimentalVariable> result = condition.experimentalVariables()

        then:
        result.size() == 3
    }

    def "If variable levels origin from the same experimental variables, return an exception"() {
        given:
        ExperimentalValue experimentalValue = ExperimentalValue.create("10", "cm")
        ExperimentalVariable<ExperimentalValue> experimentalVar = new ExperimentalVariable<>("test variable", experimentalValue)
        ExperimentalVariable<ExperimentalValue> experimentalVar2 = new ExperimentalVariable<>("test variable", experimentalValue)

        def level = new VariableLevel<>(experimentalVar, experimentalValue)
        def level2 = new VariableLevel<>(experimentalVar2, experimentalValue)

        when:
        Condition.createForExperiment(level, level2)

        then:
        thrown(IllegalArgumentException)
    }

    def "If the number of provided variable levels is less than one, throw an IllegalArgumentException" () {
        when:
        Condition.createForExperiment()

        then:
        thrown(IllegalArgumentException)

    }

}
