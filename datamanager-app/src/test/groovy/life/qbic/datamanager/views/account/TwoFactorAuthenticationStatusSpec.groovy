package life.qbic.datamanager.views.account

import life.qbic.identity.domain.model.mfa.MfaPolicyMode
import life.qbic.identity.domain.model.mfa.SecondFactorRemoval
import spock.lang.Specification

import java.time.Instant

/**
 * <b>The status block shows one state, and it is the current one.</b>
 *
 * <p>Reported from a deployed build: after enrolling, the shield stayed warning-coloured until the page
 * was reloaded. The cause was a class that was added and never removed — {@code mfa-status--deadline}
 * was missing from the list {@code render()} clears, and its CSS rule sits after
 * {@code mfa-status--enrolled} with the same specificity, so it kept winning even though both classes
 * were present.</p>
 *
 * <p>So these assertions are about the <em>element</em>, not about the inputs: a component that renders
 * the right sentence and the wrong colour is exactly the bug, and a test on the copy alone cannot see
 * it.</p>
 */
class TwoFactorAuthenticationStatusSpec extends Specification {

    static final List<String> STATE_CLASSES = [
            "mfa-status--enrolled", "mfa-status--deadline", "mfa-status--empty"
    ]

    /**
     * Walks the whole element tree, not the direct children: the status block sits inside a
     * {@code SettingsSection}, and asserting against the top level would silently find nothing.
     */
    private static List<String> stateClassesOf(TwoFactorAuthenticationComponent component) {
        def found = []
        def pending = new ArrayDeque<>([component.element])
        while (!pending.isEmpty()) {
            def element = pending.poll()
            if (element.getClassList().contains("mfa-status")) {
                found.add(element)
            }
            element.children.forEach { pending.add(it) }
        }
        assert found.size() == 1: "expected exactly one .mfa-status element, found ${found.size()}"
        found[0].getClassList().findAll { it in STATE_CLASSES } as List<String>
    }

    def "an unenrolled account under REQUIRED shows the deadline state"() {
        given:
        def component = new TwoFactorAuthenticationComponent()
        component.setPolicyMode(MfaPolicyMode.REQUIRED)
        component.setGraceDeadline(Instant.now().plusSeconds(3600))

        expect:
        stateClassesOf(component) == ["mfa-status--deadline"]
    }

    def "enrolling replaces the deadline state instead of adding to it"() {
        given: "the exact sequence the page performs: deadline first, then a factor"
        def component = new TwoFactorAuthenticationComponent()
        component.setPolicyMode(MfaPolicyMode.REQUIRED)
        component.setGraceDeadline(Instant.now().plusSeconds(3600))

        when: "the enrolment completes and the page refreshes"
        component.setFactors([life.qbic.identity.domain.model.mfa.SecondFactor.enrolTotp(
                "u-1", stubSecret(), "Authenticator app", Instant.now())],
                [1L: SecondFactorRemoval.Outcome.NOT_REMOVABLE])
        component.setGraceDeadline(null)

        then: "green, not warning-coloured — the reported symptom, on the element"
        stateClassesOf(component) == ["mfa-status--enrolled"]
    }

    def "removing the factor goes back to exactly one state, with no leftovers"() {
        given:
        def component = new TwoFactorAuthenticationComponent()
        component.setPolicyMode(MfaPolicyMode.REQUIRED)
        component.setGraceDeadline(Instant.now().plusSeconds(3600))
        component.setFactors([life.qbic.identity.domain.model.mfa.SecondFactor.enrolTotp(
                "u-1", stubSecret(), "Authenticator app", Instant.now())], [:])

        when:
        component.setFactors([], [:])

        then:
        stateClassesOf(component) == ["mfa-status--deadline"]
    }

    def "without enforcement an enrolled account is still shown as protected"() {
        given:
        def component = new TwoFactorAuthenticationComponent()
        component.setPolicyMode(MfaPolicyMode.OPTIONAL)
        component.setFactors([life.qbic.identity.domain.model.mfa.SecondFactor.enrolTotp(
                "u-1", stubSecret(), "Authenticator app", Instant.now())], [:])

        expect: "OPTIONAL must not fall into the empty state just because nothing is enforced"
        stateClassesOf(component) == ["mfa-status--enrolled"]
    }

    def "an unenrolled account under OPTIONAL is empty, not on a deadline"() {
        given:
        def component = new TwoFactorAuthenticationComponent()
        component.setPolicyMode(MfaPolicyMode.OPTIONAL)

        expect:
        stateClassesOf(component) == ["mfa-status--empty"]
    }

    // ---------------------------------------------------------------- recovery-code meter

    def "the meter shows one slot per code in a full set, filled for the unused ones"() {
        given: "seven codes left out of ten"
        def component = new TwoFactorAuthenticationComponent()
        component.setPolicyMode(MfaPolicyMode.OPTIONAL)
        component.setFactors([life.qbic.identity.domain.model.mfa.SecondFactor.enrolTotp(
                "u-1", stubSecret(), "Authenticator app", Instant.now())], [:])
        component.setRecoveryCodeCount(7)

        when:
        def meter = elementWithClass(component, "mfa-recovery-meter")

        then: "ten slots, in the order the eye reads them: used ones first, unused after"
        slotStates(meter) == (["mfa-recovery-meter__slot--unused"] * 7) +
                (["mfa-recovery-meter__slot--used"] * 3)

        and: "the same fact for a screen reader, as one label rather than ten items"
        meter.getAttribute("aria-label") == "7 of 10 recovery codes unused"
    }

    def "a full set is ten filled slots, and a spent set is ten hollow ones"() {
        given:
        def component = new TwoFactorAuthenticationComponent()
        component.setPolicyMode(MfaPolicyMode.OPTIONAL)
        component.setFactors([life.qbic.identity.domain.model.mfa.SecondFactor.enrolTotp(
                "u-1", stubSecret(), "Authenticator app", Instant.now())], [:])

        when:
        component.setRecoveryCodeCount(10)
        def full = slotStates(elementWithClass(component, "mfa-recovery-meter"))
        component.setRecoveryCodeCount(0)
        def empty = slotStates(elementWithClass(component, "mfa-recovery-meter"))

        then:
        full.every { it == "mfa-recovery-meter__slot--unused" }
        empty.every { it == "mfa-recovery-meter__slot--used" }
        full.size() == 10
        empty.size() == 10
    }

    def "without a factor the meter is hidden, because no set can be redeemed"() {
        given: "no factor means no challenge, so drawing ten hollow slots would suggest spent codes"
        def component = new TwoFactorAuthenticationComponent()
        component.setPolicyMode(MfaPolicyMode.OPTIONAL)

        expect:
        !elementWithClass(component, "mfa-recovery-meter").isVisible()
    }

    def "a count outside the set size is clamped rather than drawn"() {
        given: "the count comes from the database and the set size from the domain"
        def component = new TwoFactorAuthenticationComponent()
        component.setPolicyMode(MfaPolicyMode.OPTIONAL)
        component.setFactors([life.qbic.identity.domain.model.mfa.SecondFactor.enrolTotp(
                "u-1", stubSecret(), "Authenticator app", Instant.now())], [:])

        when:
        component.setRecoveryCodeCount(99)
        def tooMany = slotStates(elementWithClass(component, "mfa-recovery-meter"))
        component.setRecoveryCodeCount(-4)
        def negative = slotStates(elementWithClass(component, "mfa-recovery-meter"))

        then: "eleven slots or a negative one would be a rendering bug on a break-glass screen"
        tooMany.size() == 10
        tooMany.every { it == "mfa-recovery-meter__slot--unused" }
        negative.size() == 10
        negative.every { it == "mfa-recovery-meter__slot--used" }
    }

    // ---------------------------------------------------------------- helpers

    /** Finds exactly one element with the given class anywhere in the tree. */
    private static com.vaadin.flow.dom.Element elementWithClass(
            TwoFactorAuthenticationComponent component, String cssClass) {
        def found = []
        def pending = new ArrayDeque<>([component.element])
        while (!pending.isEmpty()) {
            def element = pending.poll()
            if (element.getClassList().contains(cssClass)) {
                found.add(element)
            }
            element.children.forEach { pending.add(it) }
        }
        assert found.size() == 1: "expected exactly one .${cssClass}, found ${found.size()}"
        found[0]
    }

    private static List<String> slotStates(com.vaadin.flow.dom.Element meter) {
        def states = []
        def pending = new ArrayDeque<>([meter])
        while (!pending.isEmpty()) {
            def element = pending.poll()
            def classes = element.getClassList()
            if (classes.contains("mfa-recovery-meter__slot--unused")) {
                states.add("mfa-recovery-meter__slot--unused")
            } else if (classes.contains("mfa-recovery-meter__slot--used")) {
                states.add("mfa-recovery-meter__slot--used")
            }
            element.children.forEach { pending.add(it) }
        }
        states
    }

    private static life.qbic.identity.domain.model.mfa.ProtectedSecret stubSecret() {
        life.qbic.identity.domain.model.mfa.ProtectedSecret.ofBase64(
                Base64.encoder.encodeToString(new byte[32]))
    }
}
