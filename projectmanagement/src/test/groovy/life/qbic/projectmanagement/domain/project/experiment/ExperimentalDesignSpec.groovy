package life.qbic.projectmanagement.domain.project.experiment

import life.qbic.application.commons.ApplicationException
import life.qbic.projectmanagement.application.ProjectManagementException
import life.qbic.projectmanagement.domain.project.experiment.exception.ExperimentalVariableExistsException
import spock.lang.Specification


class ExperimentalDesignSpec extends Specification {

    def "When an experimental variable with a given name already is part of the design, return a failure result"() {
        given:
        def design = new ExperimentalDesign()
        design.addVariable("Caffeine Dosage", List.of(ExperimentalValue.create("10", "mmol/l"), ExperimentalValue.create("100", "mmol/l")))

        when:
        def result = design.addVariable("Caffeine Dosage", List.of(ExperimentalValue.create("10", "mmol/l"), ExperimentalValue.create("100", "mmol/l")))

        then:
        result.isFailure()
        result.exception() instanceof ExperimentalVariableExistsException
    }

    def "When an experimental variable is new to an design, add the new variable and return with a success result"() {
        given:
        def design = new ExperimentalDesign()
        design.addVariable("Caffeine Dosage", List.of(ExperimentalValue.create("10", "mmol/l"), ExperimentalValue.create("100", "mmol/l")))

        when:
        def result = design.addVariable("CBD Dosage", List.of(ExperimentalValue.create("5", "mmol/l"), ExperimentalValue.create("20", "mmol/l")))

        then:
        result.isSuccess()
        result.value().value().equals("CBD Dosage")
        design.isVariableDefined("CBD Dosage")
    }

    def "when a variable level is requested for a defined variable then return the variable level"() {
        given:
        def design = new ExperimentalDesign()
        design.addVariable("Caffeine Dosage", List.of(ExperimentalValue.create("10", "mmol/l"), ExperimentalValue.create("100", "mmol/l")))
        when: "a variable level is requested for a defined variable"
        def result = design.getLevel("Caffeine Dosage", ExperimentalValue.create("10", "mmol/l"))
        then: "return the variable level"
        result.isPresent()
        result.get() == VariableLevel.create(VariableName.create("Caffeine Dosage"), ExperimentalValue.create("10", "mmol/l"))
    }

    def "when a variable level is requested for a defined variable that cannot have the value then an exception is thrown"() {
        given:
        def design = new ExperimentalDesign()

        def smallAmount = ExperimentalValue.create("10", "mmol/l")
        def largeAmount = ExperimentalValue.create("100", "mmol/l")
        def levels = List.of(smallAmount, largeAmount)
        design.addVariable("Caffeine Dosage", levels)

        when: "a variable level is requested for a defined variable that cannot have the value"
        def tinyAmount = ExperimentalValue.create("1", "mmol/l")
        design.getLevel("Caffeine Dosage", tinyAmount)

        then: "an exception is thrown"
        def e = thrown(ProjectManagementException)
        e.errorCode() == ApplicationException.ErrorCode.UNDEFINED_VARIABLE_LEVEL
    }

    def "when a variable level is requested for a defined variable then the level is returned"() {
        given:
        def design = new ExperimentalDesign()
        def smallAmount = ExperimentalValue.create("10", "mmol/l")
        def largeAmount = ExperimentalValue.create("100", "mmol/l")
        def levels = List.of(smallAmount, largeAmount)
        design.addVariable("Caffeine Dosage", levels)

        def expectedLevel = VariableLevel.create(VariableName.create("Caffeine Dosage"), smallAmount)

        when: "a variable level is requested for a defined variable"
        def result = design.getLevel("Caffeine Dosage", smallAmount)

        then: "the expected result is returned"
        result.isPresent()
        result.get() == expectedLevel
    }

    def "when a variable level is requested for a variable not part of the design, then Optional.empty is returned"() {
        given:
        def design = new ExperimentalDesign()
        def smallAmount = ExperimentalValue.create("10", "mmol/l")

        when: "a variable level is requested for a defined variable"
        def result = design.getLevel("Caffeine Dosage", smallAmount)

        then: "the expected result is returned"
        result.isEmpty()
    }

    def "when a condition is defined with identical variable levels then fail"() {
        given:
        def design = new ExperimentalDesign()
        def variableName = VariableName.create("environment")
        design.addVariable(variableName.value(), [ExperimentalValue.create("normal",), ExperimentalValue.create("altered")])
        def controlCondition = Condition.create("my condition", VariableLevel.create(variableName, ExperimentalValue.create("normal")))
        design.conditions.add(controlCondition)
        when: "a condition is defined with identical variable levels"
        def result = design.defineCondition("control", design.getLevel("environment", ExperimentalValue.create("normal")).get())
        then: "an exception is thrown"
        result.isFailure()
    }

    def "when a condition is defined with the same label then fail"() {
        given:
        def design = new ExperimentalDesign()
        def variableName = VariableName.create("environment")
        design.addVariable(variableName.value(), [ExperimentalValue.create("normal",), ExperimentalValue.create("altered")])
        def controlCondition = Condition.create("control", VariableLevel.create(variableName, ExperimentalValue.create("altered")))
        design.conditions.add(controlCondition)
        when: "a condition is defined with identical variable levels"
        def result = design.defineCondition("control", design.getLevel("environment", ExperimentalValue.create("normal")).get())
        then: "an exception is thrown"
        result.isFailure()
    }

    def "when a condition is not defined in the design, create a new condition"() {
        given:
        def design = new ExperimentalDesign()
        def variableName = VariableName.create("environment")
        design.addVariable(variableName.value(), [ExperimentalValue.create("normal",), ExperimentalValue.create("altered")])
        def expectedCondition = Condition.create("control", VariableLevel.create(variableName, ExperimentalValue.create("normal")))

        when: "a condition is defined"
        def result = design.defineCondition("control", design.getLevel("environment", ExperimentalValue.create("normal")).get())

        then: "the condition is returned"
        result.isSuccess()
        result.value().hasSameLevelsDefined(expectedCondition)
        result.value().equals(expectedCondition)
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
        result.isSuccess()
        result.value() == new VariableLevel(VariableName.create(variableName), otherValue)
        design.variables.get(0).levels().contains(otherValue)
    }

    def "when a level is added to an non-existent variable then the result is a failure"() {
        given:
        def design = new ExperimentalDesign()

        def variableName = "environment"
        def otherValue = ExperimentalValue.create("other")
        when:
        def result = design.addLevelToVariable(variableName, otherValue)
        then:
        result.isFailure()
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
        result.isSuccess()
        result.value() == new VariableLevel(VariableName.create(variableName), normalValue)
        design.variables.get(0).levels().contains(normalValue)
    }


}
