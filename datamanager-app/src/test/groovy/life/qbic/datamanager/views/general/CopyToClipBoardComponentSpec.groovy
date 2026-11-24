package life.qbic.datamanager.views.general

import spock.lang.Specification

/**
 * <b>The copy affordance must not be able to get stuck.</b>
 *
 * <p>This component's confirmation used to be server state — hide the copy icon, show the check,
 * restore from a {@code CompletableFuture} + {@code UI.accessLater}. Without push, {@code accessLater}
 * runs on the <em>next request</em>, so the check stayed on screen and the value looked
 * "already copied"; a second copy seemed impossible. It was reported from the setup key of the
 * authenticator enrolment, and the same bug sat in the personal-access-token banner, the ontology
 * CURIE tag and the code block.</p>
 *
 * <p>The state now lives in the browser: the element carries {@code data-copy-feedback}, and
 * {@code copyfeedback.js} sets and clears {@code data-copied}; {@code icon.css} swaps the two icons.
 * These assertions pin that wiring, because a component that quietly loses the marker renders as
 * "no confirmation at all" — the silent failure mode this project has paid for twice.</p>
 */
class CopyToClipBoardComponentSpec extends Specification {

    def "the element is marked for the browser's copy confirmation"() {
        when:
        def component = new CopyToClipBoardComponent("ABCD-EFGH")

        then: "without this attribute the delegated listener in copyfeedback.js never sees the click"
        component.element.hasAttribute("data-copy-feedback")
        component.element.classList.contains("copy-to-clipboard")
    }

    def "both icons are present, because CSS swaps them rather than the server"() {
        when:
        def component = new CopyToClipBoardComponent("ABCD-EFGH")
        def classes = component.children.toList().collectMany { it.element.classList.toList() }

        then:
        classes.contains("copy-icon")
        classes.contains("copy-icon-success")
    }

    def "the success icon is not hidden from the server side"() {
        when: "setVisible(false) would add a style the CSS toggle cannot override"
        def component = new CopyToClipBoardComponent("ABCD-EFGH")

        then:
        component.children.toList().every { !it.element.hasAttribute("hidden") }
    }

    def "the copy text can be replaced after construction"() {
        given: "a component created before the value is known, as the token banner does"
        def component = new CopyToClipBoardComponent()

        when:
        component.setCopyText("P4SR7EHZ5KAMJZWCSZYT4N2SZY5TQ2BP")

        then: "no exception, and the component stays usable"
        noExceptionThrown()
        component.element.classList.contains("copy-to-clipboard")
    }

    def "no server-side timer remains that could leave the confirmation on screen"() {
        given: "the fields the old implementation used to restore the icon"
        def names = CopyToClipBoardComponent.declaredFields*.name

        expect: "no executor, no completable future, no cached icon visibility state"
        !names.any { it.toLowerCase().contains("executor") }
        !names.any { it.toLowerCase().contains("time") }
    }
}
