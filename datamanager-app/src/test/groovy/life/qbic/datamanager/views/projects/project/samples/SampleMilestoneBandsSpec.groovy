package life.qbic.datamanager.views.projects.project.samples

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button

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

    def "the gate hides its action for a user who cannot change the design"() {
        given: "a gate"
        def gate = new SamplePrerequisiteGate()

        when: "the action is hidden (a read-only user)"
        gate.setActionVisible(false)

        then: "the prerequisite and its evidence are still stated"
        def text = gate.element.getTextRecursively()
        text.contains("Define experimental design first")
        text.contains("0 experimental groups")
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

    def "the milestone's own buttons fire the events the page listens for"() {
        given: "a milestone band with the listeners the page attaches"
        def band = new RegistrationMilestoneBand()
        def registered = false
        def designRequested = false
        band.addRegisterListener({ registered = true })
        band.addReviewDesignListener({ designRequested = true })

        when: "the primary action button is clicked"
        button(band, "sample-milestone-action").click()

        then: "registration is requested"
        registered

        when: "the design review button is clicked"
        button(band, "sample-milestone-review").click()

        then: "the design surface is requested"
        designRequested
    }

    def "dismissing the milestone hides it"() {
        given: "a visible milestone band"
        def band = new RegistrationMilestoneBand()
        band.setVisible(true)

        when: "the dismiss control is clicked"
        button(band, "sample-milestone-dismiss").click()

        then: "the band is hidden, so the achievement is not permanent furniture"
        !band.isVisible()
    }

    def "nothing is reported as dismissed outside a session"() {
        expect: "without a Vaadin session there is no dismissal to remember"
        !RegistrationMilestoneBand.isDismissed("experiment-1")
        !RegistrationMilestoneBand.isDismissed(null)
    }

    def "the milestone's action can be hidden while its explanation stays"() {
        given: "a milestone band"
        def band = new RegistrationMilestoneBand()

        when: "the action is hidden for a read-only user"
        band.setActionVisible(false)

        then: "the achievement is still explained"
        band.element.textRecursively.contains("Sample registration is unlocked")
    }

    def "the gate's own action button asks the page to route to the design surface"() {
        given: "a gate with the listener the page attaches"
        def gate = new SamplePrerequisiteGate()
        def designRequested = false
        gate.addDefineDesignRequestedListener({ designRequested = true })

        when: "the action button is clicked"
        button(gate, "sample-gate-action").click()

        then: "the page is told to route"
        designRequested
    }

    private static Button button(Component root, String className) {
        def found = descendantsOf(root).find {
            it instanceof Button && it.getClassNames().contains(className)
        }
        assert found != null: "no button carrying class ${className}"
        return (Button) found
    }

    private static List<Component> descendantsOf(Component root) {
        def found = []
        root.getChildren().forEach { child ->
            found.add(child)
            found.addAll(descendantsOf(child))
        }
        return found
    }
}
