package life.qbic.projectmanagement.domain.model.experiment

import life.qbic.projectmanagement.domain.model.experiment.exception.ExperimentalVariableExistsException
import org.junit.jupiter.api.Assertions
import spock.lang.Specification

class ExperimentalDesignSpec extends Specification {

    def "When adding a variable with a name already defined, throw if the contents are not the same"() {
        given:
        def design = new ExperimentalDesign()
        def variable = ExperimentalVariable.create("Caffeine Dosage", ExperimentalValue.create("10", "mmol/l"), ExperimentalValue.create("100", "mmol/l"))
        design.addExperimentalVariable(variable)
        when:
        def variableWithSameName = ExperimentalVariable.create("Caffeine Dosage", ExperimentalValue.create("42", "mmol/l"))
        def result = design.addExperimentalVariable(variableWithSameName)
        then:
        thrown(ExperimentalVariableExistsException)
    }

    def "When an adding the same variable twice, only the first operation changes the design"() {
        given:
        def design = new ExperimentalDesign()
        def variable = ExperimentalVariable.create("Caffeine Dosage", ExperimentalValue.create("10", "mmol/l"), ExperimentalValue.create("100", "mmol/l"))
        when:
        def firstAttempt = design.addExperimentalVariable(variable)
        def secondAttempt = design.addExperimentalVariable(variable)
        then:
        firstAttempt
        !secondAttempt
        design.isVariableDefined(variable.name().value())
    }

    def "When an experimental variable is new to a design, add the new variable and return true"() {
        given:
        def design = new ExperimentalDesign()

        def variableName = "Caffeine Dosage"
        def variable = ExperimentalVariable.create(variableName, ExperimentalValue.create("10", "mmol/l"), ExperimentalValue.create("100", "mmol/l"))


        when:
        def result = design.addExperimentalVariable(variable)

        then:
        design.isVariableDefined(variableName)
        Assertions.assertTrue(result)
    }

    def "When setting the variable levels to the exact same, no change is performed"() {
        given:
        def design = new ExperimentalDesign()

        def variableName = "Caffeine Dosage"
        def levels = [ExperimentalValue.create("10", "mmol/l"), ExperimentalValue.create("100", "mmol/l")]
        def variable = ExperimentalVariable.create(variableName, levels.toArray(ExperimentalValue[]::new))
        design.addExperimentalVariable(variable)

        when:
        def wasModified = design.setVariableLevels(variableName, levels)
        then:
        !wasModified
    }

    def "When setting the variable levels of a variable without levels, the design changes and the levels are added"() {

        given:
        def design = new ExperimentalDesign()

        def variableName = "Caffeine Dosage"
        def levels = [ExperimentalValue.create("10", "mmol/l"), ExperimentalValue.create("100", "mmol/l")]
        def variable = ExperimentalVariable.create(variableName, levels[0])
        design.addExperimentalVariable(variable)
        def newlySetLevels = levels
        when:
        def wasModified = design.setVariableLevels(variableName, newlySetLevels)

        then:
        wasModified
        design.getVariable(variableName)
                .map(vari -> vari.levels().stream().allMatch { levels.contains(it.experimentalValue()) })
                .orElse(false)
    }

    def "When setting the variable levels to a subsection of existing levels, the design changes and only the subsection is present afterwards"() {

        given:
        def design = new ExperimentalDesign()

        def variableName = "Caffeine Dosage"
        def levels = [ExperimentalValue.create("10", "mmol/l"), ExperimentalValue.create("100", "mmol/l")]
        def variable = ExperimentalVariable.create(variableName, levels[0])
        design.addExperimentalVariable(variable)
        def newlySetLevels = List.of(levels[1])

        when:
        def wasModified = design.setVariableLevels(variableName, newlySetLevels)

        then:
        wasModified
        design.getVariable(variableName)
                .map(vari -> vari.levels().stream().map(it -> it.experimentalValue()).allMatch(itt -> newlySetLevels.contains(itt)))
                .orElse(false)
        design.getVariable(variableName).map { it.levels().size().equals(newlySetLevels.size()) }.orElse(false)

    }

    def "When setting the variable levels to a contain additional levels, the design changes and all the levels are present afterwards"() {

        given:
        def design = new ExperimentalDesign()

        def variableName = "Caffeine Dosage"
        def levels = [ExperimentalValue.create("10", "mmol/l"), ExperimentalValue.create("100", "mmol/l")]
        def variable = ExperimentalVariable.create(variableName, levels[0])
        design.addExperimentalVariable(variable)
        def newlySetLevels = new ArrayList()
        newlySetLevels.addAll(levels)
        newlySetLevels.add(ExperimentalValue.create("88", "mmol/l"))

        when:
        def wasModified = design.setVariableLevels(variableName, newlySetLevels)

        then:
        wasModified
        design.getVariable(variableName)
                .map(vari -> vari.levels().stream().map { it -> it.experimentalValue() }.allMatch { newlySetLevels.contains(it) })
                .orElse(false)
        design.getVariable(variableName).map { it.levels().size().equals(newlySetLevels.size()) }.orElse(false)

    }


    def "when an experimental group is defined with identical variable levels then fail"() {
        given:
        def design = new ExperimentalDesign()
        def variableName = VariableName.create("environment")
        var variable = ExperimentalVariable.create(variableName.value(), ExperimentalValue.create("normal",), ExperimentalValue.create("altered"))
        design.addExperimentalVariable(variable)
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
        var variable = ExperimentalVariable.create(variableName.value(), ExperimentalValue.create("normal",), ExperimentalValue.create("altered"))
        design.addExperimentalVariable(variable)
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


}
