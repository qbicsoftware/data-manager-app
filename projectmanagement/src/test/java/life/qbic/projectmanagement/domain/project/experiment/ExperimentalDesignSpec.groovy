package life.qbic.projectmanagement.domain.project.experiment

import life.qbic.application.commons.Result
import life.qbic.projectmanagement.domain.project.experiment.exception.ExperimentalVariableExistsException
import life.qbic.projectmanagement.domain.project.experiment.exception.SampleGroupExistsException
import life.qbic.projectmanagement.domain.project.experiment.exception.UnknownConditionException
import spock.lang.Specification

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
class ExperimentalDesignSpec extends Specification {

    def "When an experimental variable with a given name already is part of the design, return a failure result"() {
        given:
        def design = new ExperimentalDesign()
        design.createExperimentalVariable("Caffeine Dosage", ExperimentalValue.create("10", "mmol/l"), ExperimentalValue.create("100", "mmol/l"))

        when:
        def result = design.createExperimentalVariable("Caffeine Dosage", ExperimentalValue.create("5", "mmol/l"), ExperimentalValue.create("20", "mmol/l"))

        then:
        result.isFailure()
        result.exception() instanceof ExperimentalVariableExistsException
    }

    def "When an experimental variable is new to an design, add the new variable and return with a success result"() {
        given:
        def design = new ExperimentalDesign()
        design.createExperimentalVariable("Caffeine Dosage", ExperimentalValue.create("10", "mmol/l"), ExperimentalValue.create("100", "mmol/l"))

        when:
        def result = design.createExperimentalVariable("CBD Dosage", ExperimentalValue.create("5", "mmol/l"), ExperimentalValue.create("20", "mmol/l"))

        then:
        result.isSuccess()
        result.value().name().equals("CBD Dosage")
    }

    def "If the provided condition is not part of the experimental design, do not create a sample group and return a failure response"() {
        given:
        def design = new ExperimentalDesign()

        when:
        Result<SampleGroup, Exception> result = design.createSampleGroup("Sample Group A", 10, 1985L)

        then:
        result.isFailure()
        result.exception() instanceof UnknownConditionException

    }

    def "If the provided condition is part of the experimental design, create a sample group and return a success response"() {
        given:
        def design = new ExperimentalDesign()
        def result =  design.createExperimentalVariable("Caffeine Dosage", ExperimentalValue.create("10", "mmol/l"), ExperimentalValue.create("100", "mmol/l"))

        when:
        def conditionResult = design.createCondition(new VariableLevel<>(result.value(), ExperimentalValue.create("10", "mmol/l")))
        def sampleGroupResult =  design.createSampleGroup("Sample Group A", 10, conditionResult.value().id())

        then:
        sampleGroupResult.isSuccess()
        design.sampleGroups().size() == 1
        design.sampleGroupIterator().next().condition().id() == conditionResult.value().id()
    }

    def "If a sample group with the provided name already exists in the design, return a failure response"() {
        given:
        def design = new ExperimentalDesign()
        def result =  design.createExperimentalVariable("Caffeine Dosage", ExperimentalValue.create("10", "mmol/l"), ExperimentalValue.create("100", "mmol/l"))

        when:
        def conditionResult = design.createCondition(new VariableLevel<>(result.value(), ExperimentalValue.create("10", "mmol/l")))
        def sampleGroupResult =  design.createSampleGroup("Sample Group A", 10, conditionResult.value().id())
        // second addition
        design.createSampleGroup("Sample Group A", 10, conditionResult.value().id())

        then:
        sampleGroupResult.isFailure()
        sampleGroupResult.exception() instanceof SampleGroupExistsException
    }

}
