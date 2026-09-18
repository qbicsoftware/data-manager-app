package life.qbic.datamanager.views.projects.overview.components

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.router.RouterLink
import life.qbic.projectmanagement.application.pinned.PinnedProjectView
import life.qbic.projectmanagement.domain.model.project.ProjectId
import spock.lang.Specification

import java.time.Instant
import java.util.function.Supplier

/**
 * Unit tests for the pinned-project quick-access row.
 *
 * <p>Covers the behaviours that are easy to break silently and that need no router: the row
 * disappears entirely for a user who has pinned nothing, and a pin whose project can no longer be
 * read is rendered as a placeholder — no navigation target, only the label captured at pin time, and
 * still removable by its owner.
 *
 * <p>The accessible-pin card is deliberately not exercised here: it wraps its body in a
 * {@link RouterLink}, which needs a {@code VaadinService} with a registered route. Its data selection
 * (live label, snapshot ignored) is covered by {@code PinnedProjectServiceSpec}.
 */
class PinnedProjectsComponentSpec extends Specification {

  List<PinnedProjectView> pins = []
  List<Map<String, Object>> toggleCalls = []
  PinnedProjectsComponent component

  def setup() {
    Supplier<List<PinnedProjectView>> pinsSupplier = { -> pins } as Supplier
    component = new PinnedProjectsComponent(pinsSupplier,
        { ProjectId projectId, boolean pin ->
          toggleCalls << [projectId: projectId, pin: pin]
        } as PinnedProjectsComponent.ToggleHandler)
  }

  def "is completely hidden while the user has pinned nothing"() {
    given: "the user has no pins"
    pins = []

    when:
    component.refresh()

    then:
    !component.visible
    renderedCards().isEmpty()
  }

  def "shows one card per pin and reports the pinned project ids for the overview card toggles"() {
    given: "the user holds two pins, newest first"
    def first = ProjectId.create()
    def second = ProjectId.create()
    pins = [placeholderView(first, "2024_010"), placeholderView(second, "2024_011")]

    when:
    component.refresh()

    then:
    component.visible
    renderedCards().size() == 2
    component.pinnedProjectIds() as List == [first, second]
    component.isPinned(first)
    !component.isPinned(ProjectId.create())
  }

  def "renders an unreadable pin as a placeholder without a navigation target"() {
    given: "a pin whose project the user cannot read any more"
    def lost = ProjectId.create()
    pins = [placeholderView(lost, "2024_001", "Title captured when pinned")]

    when:
    component.refresh()

    then: "the card is not a link and shows only the pin-time label"
    def card = renderedCards()[0]
    cardChildren(card).every { !(it instanceof RouterLink) }
    textOf(card).contains("2024_001")
    textOf(card).contains("Title captured when pinned")
    textOf(card).toLowerCase().contains("no longer have access")

    and: "the marker class makes the placeholder recognisable to the theme"
    card.element.classList.contains("no-access")
  }

  def "keeps an unreadable pin removable, because a pin nobody can free would hold its place forever"() {
    given:
    def lost = ProjectId.create()
    pins = [placeholderView(lost)]

    when:
    component.refresh()
    clickUnpin(renderedCards()[0])

    then: "the unpin is delegated as a removal, without any project access"
    toggleCalls == [[projectId: lost, pin: false]]
  }

  def "re-renders from the supplier, so a pin removed elsewhere disappears from the row"() {
    given: "the row shows one pin"
    pins = [placeholderView(ProjectId.create())]
    component.refresh()

    when: "the user has no pins any more and the row is refreshed"
    pins = []
    component.refresh()

    then:
    renderedCards().isEmpty()
    !component.visible
  }

  private List<Div> renderedCards() {
    component.@cards.children.toList() as List<Div>
  }

  private static List<Component> cardChildren(Div card) {
    card.children.toList() as List<Component>
  }

  private void clickUnpin(Div card) {
    // The unpin action now lives in the context menu attached to the kebab button.
    // Reaching it through the component's registered actions keeps the test on the
    // server side without simulating browser menu interaction.
    List<Button> buttons = cardChildren(card).findAll { it instanceof Button }
    assert buttons.size() == 1
    component.@unpinActions.values().first().run()
  }

  private static String textOf(Component component) {
    def text = new StringBuilder()
    appendText(component, text)
    return text.toString()
  }

  private static void appendText(Component component, StringBuilder target) {
    target.append(component.element.text ?: "")
    component.children.forEach { child -> appendText(child, target) }
  }

  private static PinnedProjectView placeholderView(ProjectId projectId, String code = "2024_001",
      String title = "Pinned time title") {
    return new PinnedProjectView(projectId, Instant.parse("2026-01-02T00:00:00Z"),
        PinnedProjectView.AccessState.REVOKED, code, title, null)
  }
}
