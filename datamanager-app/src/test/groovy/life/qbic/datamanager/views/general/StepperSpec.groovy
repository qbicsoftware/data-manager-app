package life.qbic.datamanager.views.general

import life.qbic.datamanager.views.general.Stepper.StepIndicator
import life.qbic.datamanager.views.general.Stepper.StepState
import spock.lang.Specification

/**
 * Covers the step state model. The bug these guard against is that every step which was not the
 * current one rendered identically, so a completed step and one never reached looked the same.
 *
 * @since 1.11.0
 */
class StepperSpec extends Specification {

    def "a new step starts as NOT_STARTED"() {
        given:
        def stepper = new Stepper()

        when:
        StepIndicator step = stepper.addStep("First")

        then:
        step.getState() == StepState.NOT_STARTED
    }

    def "selecting a step makes it CURRENT"() {
        given:
        def stepper = new Stepper()
        def first = stepper.addStep("First")
        def second = stepper.addStep("Second")

        when:
        stepper.setSelectedStep(second)

        then:
        second.getState() == StepState.CURRENT
        first.getState() == StepState.NOT_STARTED
    }

    def "leaving a step behind keeps its recorded outcome"() {
        given:
        def stepper = new Stepper()
        def first = stepper.addStep("First")
        def second = stepper.addStep("Second")
        stepper.setSelectedStep(first)
        stepper.markCompleted(first)

        when:
        stepper.setSelectedStep(second)

        then:
        first.getState() == StepState.COMPLETED
        second.getState() == StepState.CURRENT
    }

    def "an optional step that was passed empty is SKIPPED, not COMPLETED"() {
        given:
        def stepper = new Stepper()
        def required = stepper.addStep("Required")
        def optional = stepper.addStep("Optional")
        stepper.setSelectedStep(required)

        when: "the user leaves the optional step without filling it in"
        stepper.setSelectedStep(optional)
        stepper.markSkipped(optional)

        then:
        optional.getState() == StepState.SKIPPED
        optional.getState() != required.getState()
    }

    def "revisiting a step makes it CURRENT again"() {
        given:
        def stepper = new Stepper()
        def first = stepper.addStep("First")
        def second = stepper.addStep("Second")
        stepper.setSelectedStep(first)
        stepper.markSkipped(first)
        stepper.setSelectedStep(second)

        when: "the user navigates back"
        stepper.setSelectedStep(first)

        then:
        first.getState() == StepState.CURRENT
    }

    def "resetting a step returns it to NOT_STARTED"() {
        given:
        def stepper = new Stepper()
        def optional = stepper.addStep("Optional")
        def last = stepper.addStep("Last")
        stepper.setSelectedStep(optional)
        stepper.markCompleted(optional)

        when: "the entry that completed the step is removed again"
        stepper.markNotStarted(optional)
        stepper.setSelectedStep(last)

        then:
        optional.getState() == StepState.NOT_STARTED
    }

    def "state is mirrored onto the element for the styling to key off"() {
        given:
        def stepper = new Stepper()
        def step = stepper.addStep("First")

        when:
        stepper.setSelectedStep(step)
        def currentAttribute = step.getElement().getAttribute("data-state")
        stepper.markSkipped(step)
        def skippedAttribute = step.getElement().getAttribute("data-state")

        then:
        currentAttribute == StepState.CURRENT.name()
        skippedAttribute == StepState.SKIPPED.name()
    }

    def "a completed step shows a check glyph instead of its number"() {
        given:
        def stepper = new Stepper()
        def step = stepper.addStep("First")
        def marker = step.getChildren()
                .filter { it.getClassNames().contains("step-marker") }
                .findFirst().orElseThrow()
        def ordinal = marker.getChildren().findFirst().orElseThrow()

        expect: "an unfinished step shows its ordinal as text, not a glyph"
        ordinal.getText() == "1"
        !(ordinal instanceof com.vaadin.flow.component.icon.Icon)

        when:
        stepper.markCompleted(step)

        then: "the ordinal is replaced by a check icon, so done is a glyph and not only a colour"
        def glyph = marker.getChildren().findFirst().orElseThrow()
        glyph instanceof com.vaadin.flow.component.icon.Icon
    }

    def "stepping forward and back uses the defined order"() {
        given:
        def stepper = new Stepper()
        def first = stepper.addStep("First")
        def second = stepper.addStep("Second")
        def third = stepper.addStep("Third")
        stepper.setSelectedStep(first)

        when:
        stepper.selectNextStep()
        stepper.selectNextStep()

        then:
        stepper.getSelectedStep() == third
        stepper.isLastStep(third)

        when: "next at the last step is a no-op"
        stepper.selectNextStep()

        then:
        stepper.getSelectedStep() == third

        when:
        stepper.selectPreviousStep()
        stepper.selectPreviousStep()

        then:
        stepper.getSelectedStep() == first
        stepper.isFirstStep(first)
    }
}
