package life.qbic.datamanager.views.groups

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.Router
import com.vaadin.flow.router.RouterLink
import com.vaadin.flow.server.RouteRegistry
import com.vaadin.flow.server.VaadinService
import life.qbic.usergroups.api.GroupRole
import life.qbic.usergroups.api.GroupType
import life.qbic.usergroups.api.MyGroupMembership
import spock.lang.Specification

import java.util.function.Supplier

/**
 * Unit tests for the My Groups summary panel on the project overview.
 *
 * <p>Covers the dashboard behaviours that need no real UI: the panel is hidden entirely while the
 * caller belongs to no group, and each membership renders as a clickable card carrying name, type
 * badge, role badge and member count. The card is a {@link RouterLink} to the group detail route,
 * resolved through a minimal {@link VaadinService}/registry stub like {@code HomeLinkSpec}.</p>
 */
class MyGroupSummaryComponentSpec extends Specification {

  private static final String GROUP_DETAIL_ROUTE = "groups"

  List<MyGroupMembership> memberships = []
  MyGroupSummaryComponent component
  VaadinService stubbedService

  def setup() {
    RouteRegistry registry = Stub(RouteRegistry)
    registry.getTargetUrl({ Class targetClass }, { RouteParameters parameters }) >>
        { Class targetClass, RouteParameters parameters ->
          if (targetClass == GroupDetailMain) {
            return Optional.of("groups/" + parameters.get("groupId").orElse(""))
          }
          return Optional.of("groups")
        }
    Router router = new Router(registry)
    VaadinService service = Stub(VaadinService)
    service.getRouter() >> router
    service.getContext() >> null
    stubbedService = service
    VaadinService.setCurrent(stubbedService)

    Supplier<List<MyGroupMembership>> supplier = { -> memberships } as Supplier
    component = new MyGroupSummaryComponent(supplier)
  }

  def cleanup() {
    VaadinService.setCurrent(null)
  }

  def "is completely hidden while the caller belongs to no group"() {
    given: "the caller has no memberships"
    memberships = []

    when:
    component.refresh()

    then:
    !component.visible
  }

  def "shows one card per membership with name, type and role badges"() {
    given: "the caller belongs to an ad-hoc group as owner and an org group as member"
    memberships = [
        membership("group-1", "Bioinformatics Lab", GroupType.ADHOC, GroupRole.OWNER, 4),
        membership("group-2", "NGS Lab", GroupType.ORG, GroupRole.MEMBER, 12),
    ]

    when:
    component.refresh()

    then: "the panel is visible and each membership gets a link card"
    component.visible
    renderedCards().size() == 2

    and: "each card carries name, badges and a link to the group detail route"
    def cards = renderedCards()
    textOf(cards[0]).contains("Bioinformatics Lab")
    textOf(cards[0]).contains("User Group")
    textOf(cards[0]).contains("Owner")
    textOf(cards[0]).contains("4 members")
    cards[0] instanceof RouterLink
    (cards[0] as RouterLink).href.contains("group-1")

    textOf(cards[1]).contains("NGS Lab")
    textOf(cards[1]).contains("Org")
    textOf(cards[1]).contains("Member")
    textOf(cards[1]).contains("12 members")
    cards[1] instanceof RouterLink
    (cards[1] as RouterLink).href.contains("group-2")
  }

  private List<RouterLink> renderedCards() {
    def cardsContainer = component.children.iterator().collect { it as Component }
        .find { it instanceof Div && it.element.classList.contains("my-group-summary__cards") }
    if (cardsContainer == null) {
      return []
    }
    return cardsContainer.children.iterator().collect { it as Component }
        .findAll { it instanceof RouterLink }.collect { it as RouterLink }
  }

  private static String textOf(RouterLink link) {
    return collectText(link)
  }

  private static String collectText(Component component) {
    StringBuilder sb = new StringBuilder()
    if (component instanceof Span span) {
      sb.append(span.getText()).append(" ")
    }
    component.children.iterator().collect { it as Component }.each { child ->
      sb.append(collectText(child))
    }
    return sb.toString()
  }

  private static MyGroupMembership membership(String id, String name, GroupType type,
      GroupRole role, int count) {
    new MyGroupMembership(id, name, null, type, role, count)
  }
}