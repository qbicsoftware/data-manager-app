package life.qbic.projectmanagement.domain.project.experiment


import life.qbic.projectmanagement.domain.project.experiment.exception.ExperimentalVariableExistsException
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
    }

    //TODO test define condition
    //TODO test add level to variable

}
