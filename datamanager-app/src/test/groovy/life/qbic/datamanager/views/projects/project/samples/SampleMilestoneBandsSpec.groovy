package life.qbic.datamanager.views.projects.project.samples

import spock.lang.Specification

/**
 * Covers the two bands that state the registration milestone: the prerequisite gate (no
 * experimental group yet, so nothing can be registered) and the unlocked milestone (a group exists,
 * nothing registered yet).
 *
 * The distinction matters because the page used to express both states by hiding itself, so the
 * user lost the page title and any evidence of what they had just achieved. Neither band is a
 * variant of the other: the gate states a missing prerequisite and is never dismissible, the
 * milestone states an achievement and can be dismissed.
 */
class SampleMilestoneBandsSpec extends Specification {

    def "the prerequisite gate states the missing counts, never a bare zero"() {
        given: "a gate for an experiment without variables or groups"
        def gate = new SamplePrerequisiteGate()

        when: "the evidence line is updated"
        gate.update(0, 0)

        then: "the counts are named, so the user knows what is missing"
        def text = gate.element.getTextRecursively()
        text.contains("0 experimental groups")
        text.contains("0 variables")
    }

    def "the prerequisite gate pluralises its evidence correctly"() {
        given: "a gate"
        def gate = new SamplePrerequisiteGate()

        when: "there is one group and one variable"
        gate.update(1, 1)

        then: "the singular form is used"
        def text = gate.element.getTextRecursively()
        text.contains("1 experimental group")
        text.contains("1 variable")
        !text.contains("1 experimental groups")
        !text.contains("1 variables")
    }

    def "the gate starts hidden so it never renders before its state is known"() {
        expect: "a freshly built gate is not visible"
        !new SamplePrerequisiteGate().isVisible()
    }

    def "the milestone states the facts that unlocked registration"() {
        given: "a milestone band"
        def band = new RegistrationMilestoneBand()

        when: "an experiment with one group and two variables is described"
        band.setExperiment("experiment-1", 1, 2)

        then: "the achievement and its evidence are stated"
        def text = band.element.getTextRecursively()
        text.contains("Sample registration is unlocked")
        text.contains("Unlocked")
        text.contains("1 experimental group")
        text.contains("2 variables")
    }

    def "the milestone starts hidden so a populated page never shows it as furniture"() {
        expect: "a freshly built milestone is not visible"
        !new RegistrationMilestoneBand().isVisible()
    }

    def "the milestone exposes the page's primary action"() {
        given: "a milestone band"
        def band = new RegistrationMilestoneBand()
        def registerRequested = false
        band.addRegisterListener({ registerRequested = true })

        when: "the primary action is taken"
        band.fireEvent(new RegistrationMilestoneBand.RegisterRequested(band, true))

        then: "the page is told to open registration"
        registerRequested
    }

    def "the gate exposes the route to the experimental design"() {
        given: "a gate"
        def gate = new SamplePrerequisiteGate()
        def designRequested = false
        gate.addDefineDesignRequestedListener({ designRequested = true })

        when: "the action is taken"
        gate.fireEvent(new SamplePrerequisiteGate.DefineDesignRequested(gate, true))

        then: "the page is told to route to the design surface"
        designRequested
    }
}
