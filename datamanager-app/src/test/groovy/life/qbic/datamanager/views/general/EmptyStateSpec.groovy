package life.qbic.datamanager.views.general

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.icon.VaadinIcon
import spock.lang.Specification

/**
 * Covers the restatable placeholder and the order-independence of its action.
 *
 * <p>Both were defects found in a running instance, not in a test: the grid registers its action
 * listener in its constructor, long before it knows whether the empty condition will be a filtered
 * one (which offers an action) or an unfiltered one (which must not). An {@code EmptyState} built
 * without an action label therefore had no button to bind to and threw on construction, which
 * surfaced as an error page rather than as a missing button.</p>
 */
class EmptyStateSpec extends Specification {

    private static Button actionOf(EmptyState state) {
        return state.children
                .filter { it instanceof Button }
                .map { (Button) it }
                .findFirst()
                .orElse(null)
    }

    def "a listener registered before an action exists still fires once the action appears"() {
        given: "a placeholder with no action, as the grid builds it"
        def state = new EmptyState(VaadinIcon.FILTER, "Nothing to show", "No items match.")
        boolean fired = false

        when: "the caller registers first, and a filtered condition introduces the button later"
        state.addActionListener { fired = true }
        state.setContent(VaadinIcon.FILTER, "No matching measurements", "None match.", "Clear search")

        then: "the button the reader sees is wired to that listener"
        actionOf(state) != null
        actionOf(state).click()
        fired
    }

    def "registering a listener without an action is not an error"() {
        when: "the grid does exactly this in its constructor"
        def state = new EmptyState(VaadinIcon.FILTER, "Nothing to show", "No items match.")
        state.addActionListener { }

        then: "nothing is thrown, and there is simply no button yet"
        actionOf(state) == null
    }

    def "an unfiltered restatement leaves no call to action"() {
        given: "a placeholder that offered an action for the filtered condition"
        def state = new EmptyState(VaadinIcon.FILTER, "Nothing to show", "No items match.")
        state.addActionListener { }
        state.setContent(VaadinIcon.FILTER, "No matching", "None.", "Clear search")

        when: "the condition becomes a genuinely empty list"
        state.setContent(VaadinIcon.FILTER, "No measurements yet", "None registered.", null)

        then: "the action is withdrawn, because the reader cannot resolve this from here"
        actionOf(state) == null
    }

    def "restating does not double-fire a listener registered once"() {
        given:
        def state = new EmptyState(VaadinIcon.FILTER, "Nothing to show", "No items match.")
        int clicks = 0
        state.addActionListener { clicks++ }

        when: "the placeholder is restated, then the action is clicked"
        state.setContent(VaadinIcon.FILTER, "No matching", "None.", "Clear search")
        state.setContent(VaadinIcon.FILTER, "No matching", "None.", "Clear search")
        actionOf(state).click()

        then: "one click is one event"
        clicks == 1
    }
}
