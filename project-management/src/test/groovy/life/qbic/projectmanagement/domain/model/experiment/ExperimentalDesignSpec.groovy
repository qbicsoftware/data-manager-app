package life.qbic.projectmanagement.domain.model.experiment


import life.qbic.projectmanagement.domain.model.experiment.exception.ExperimentalVariableExistsException
import spock.lang.Specification

class ExperimentalDesignSpec extends Specification {

    def "When an experimental variable with a given name already is part of the design, return a failure result"() {
        given:
        def design = new ExperimentalDesign()
        design.addVariable("Caffeine Dosage", List.of(ExperimentalValue.create("10", "mmol/l"), ExperimentalValue.create("100", "mmol/l")))

        when:
        def result = design.addVariable("Caffeine Dosage", List.of(ExperimentalValue.create("10", "mmol/l"), ExperimentalValue.create("100", "mmol/l")))

        then:
        result.isError()
        result.getError() instanceof ExperimentalVariableExistsException
    }

    def "When an experimental variable is new to an design, add the new variable and return with a success result"() {
        given:
        def design = new ExperimentalDesign()
        design.addVariable("Caffeine Dosage", List.of(ExperimentalValue.create("10", "mmol/l"), ExperimentalValue.create("100", "mmol/l")))

        when:
        def result = design.addVariable("CBD Dosage", List.of(ExperimentalValue.create("5", "mmol/l"), ExperimentalValue.create("20", "mmol/l")))

        then:
        result.isValue()
        result.getValue().name().value().equals("CBD Dosage")
        design.isVariableDefined("CBD Dosage")
    }

    def "when an experimental group is defined with identical variable levels then fail"() {
        given:
        def design = new ExperimentalDesign()
        def variableName = VariableName.create("environment")
        design.addVariable(variableName.value(), [ExperimentalValue.create("normal",), ExperimentalValue.create("altered")])
        design.addExperimentalGroup("name", Arrays.asList(VariableLevel.create(variableName, ExperimentalValue.create("normal"))), 5)
        when: "an experimental group is defined with identical variable levels"
        var response = design.addExperimentalGroup("other name",
                Arrays.asList(VariableLevel.create(VariableName.create("environment"), ExperimentalValue.create("normal"))), 4)
        then: "an exception is thrown"
        response.getError() == ExperimentalDesign.AddExperimentalGroupResponse.ResponseCode.CONDITION_EXISTS
    }

    def "when an experimental group is not defined in the design, a new one is added"() {
        given:
        def design = new ExperimentalDesign()
        def variableName = VariableName.create("environment")
        design.addVariable(variableName.value(), [ExperimentalValue.create("normal",), ExperimentalValue.create("altered")])
        design.addExperimentalGroup("name", Arrays.asList(VariableLevel.create(variableName, ExperimentalValue.create("normal"))), 5)

        when: "a new experimental group is defined"
        design.addExperimentalGroup("name", Arrays.asList(VariableLevel.create(VariableName.create("environment"), ExperimentalValue.create("altered"))), 4)

        then: "both experimental groups are created and no exception is thrown"
        def groups = design.experimentalGroups
        groups.size() == 2
        def cond1 = Condition.create(Arrays.asList(VariableLevel.create(variableName, ExperimentalValue.create("normal"))))
        def cond2 = Condition.create(Arrays.asList(VariableLevel.create(variableName, ExperimentalValue.create("altered"))))
        def expectedConditions = new HashSet<Condition>(Arrays.asList(cond1, cond2))
        def returnedConditions = new HashSet<Condition>()
        for (ExperimentalGroup group : groups) {
            returnedConditions.add(group.condition())
        }
        returnedConditions.equals(expectedConditions)
    }

    def "when a level is added to an existing variable then the level is part of the variable"() {
        given:
        def design = new ExperimentalDesign()

        def variableName = "environment"
        def normalValue = ExperimentalValue.create("normal",)
        def alteredValue = ExperimentalValue.create("altered")
        def otherValue = ExperimentalValue.create("other")

        design.addVariable(variableName, [normalValue, alteredValue])

        when:
        def result = design.addLevelToVariable(variableName, otherValue)

        then:
        result.isValue()
        result.getValue() == new VariableLevel(VariableName.create(variableName), otherValue)
        design.variables.get(0).levels().any { it.experimentalValue() == otherValue }
    }

    def "when a level is added to an non-existent variable then the result is a failure"() {
        given:
        def design = new ExperimentalDesign()

        def variableName = "environment"
        def otherValue = ExperimentalValue.create("other")
        when:
        def result = design.addLevelToVariable(variableName, otherValue)
        then:
        result.isError()
    }

    def "when a level is added to an existing variable with the level already defined then the result is a success"() {
        given:
        def design = new ExperimentalDesign()

        def variableName = "environment"
        def normalValue = ExperimentalValue.create("normal",)
        def alteredValue = ExperimentalValue.create("altered")

        design.addVariable(variableName, [normalValue, alteredValue])

        when:
        def result = design.addLevelToVariable(variableName, normalValue)
        then:
        result.isValue()
        result.getValue() == new VariableLevel(VariableName.create(variableName), normalValue)
        design.variables.get(0).levels().any { it.experimentalValue() == normalValue }
    }


}
