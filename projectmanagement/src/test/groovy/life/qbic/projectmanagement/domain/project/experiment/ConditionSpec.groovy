package life.qbic.projectmanagement.domain.project.experiment

import spock.lang.Specification


class ConditionSpec extends Specification {

    def "If an experimental variable is defined in the condition, return its configured level"() {
        given:
        ExperimentalValue experimentalValue = ExperimentalValue.create("10", "cm")
        ExperimentalVariable experimentalVar = ExperimentalVariable.create("test variable", experimentalValue)
        def level = VariableLevel.create(experimentalVar.name(), experimentalValue)
        def condition = Condition.create(new HashSet<>(Arrays.asList(level)))

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
        def level = VariableLevel.create(experimentalVar.name(), experimentalValue)
        def condition = Condition.create(new HashSet<>(Arrays.asList(level)))

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

        def level = VariableLevel.create(experimentalVar.name(), experimentalValue)
        def level2 = VariableLevel.create(experimentalVar2.name(), experimentalValue)

        when:
        Condition.create(new HashSet<>(Arrays.asList(level, level2)))

        then:
        thrown(IllegalArgumentException)
    }

    def "If the number of provided variable levels is less than one, throw an IllegalArgumentException" () {
        when:
        Condition.create(new HashSet<>(Arrays.asList()))

        then:
        thrown(IllegalArgumentException)

    }

}
