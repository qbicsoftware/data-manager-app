package life.qbic.datamanager.views.general

import com.vaadin.flow.component.html.Span
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.Router
import com.vaadin.flow.router.RouterLink
import com.vaadin.flow.server.RouteRegistry
import com.vaadin.flow.server.VaadinService
import life.qbic.datamanager.views.groups.MyGroupsMain
import life.qbic.datamanager.views.projects.overview.ProjectOverviewMain
import spock.lang.Specification

/**
 * Unit tests for the top-level navigation (GitHub-style primary tabs).
 *
 * <p>The component renders {@link RouterLink}s, which resolve their href from the
 * {@link VaadinService} router registry at construction time. These specs install a minimal
 * service/registry stub mapping {@link ProjectOverviewMain} and {@link MyGroupsMain} to their
 * production routes, mirroring {@code HomeLinkSpec}.</p>
 */
class TopLevelNavigationComponentSpec extends Specification {

  private static final String OVERVIEW_ROUTE = "projects/list"
  private static final String GROUPS_ROUTE = "groups"

  private VaadinService stubbedService

  def setup() {
    RouteRegistry registry = Stub(RouteRegistry)
    registry.getTargetUrl(ProjectOverviewMain, _ as RouteParameters) >> Optional.of(OVERVIEW_ROUTE)
    registry.getTargetUrl(MyGroupsMain, _ as RouteParameters) >> Optional.of(GROUPS_ROUTE)
    Router router = new Router(registry)
    VaadinService service = Stub(VaadinService)
    service.getRouter() >> router
    service.getContext() >> null
    stubbedService = service
    VaadinService.setCurrent(stubbedService)
  }

  def cleanup() {
    VaadinService.setCurrent(null)
  }

  def "renders exactly the two primary surfaces as link tabs"() {
    given: "a top-level navigation"
    def nav = new TopLevelNavigationComponent()

    expect: "one link per surface"
    tabsOf(nav).size() == 2
  }

  def "links to the project overview and the groups hub"() {
    given: "a top-level navigation with a stubbed registry"
    def nav = new TopLevelNavigationComponent()

    expect: "the hrefs point at the canonical routes"
    def links = tabsOf(nav)
    links.any { it.href == OVERVIEW_ROUTE && textOf(it) == "Projects" }
    links.any { it.href == GROUPS_ROUTE && textOf(it) == "Groups" }
  }

  def "declares css classes so the theme can style the tabs"() {
    given: "a top-level navigation"
    def nav = new TopLevelNavigationComponent()

    expect: "component and tab classes are present"
    nav.element.classList.contains("top-level-navigation")
    nav.tabs().element.classList.contains("top-level-navigation-tabs")
    tabsOf(nav).every { it.element.classList.contains("top-level-navigation-tab") }
  }

  private static List<RouterLink> tabsOf(TopLevelNavigationComponent nav) {
    nav.tabs().getChildren().iterator().collect { it as com.vaadin.flow.component.Component }
        .findAll { it instanceof RouterLink }.collect { it as RouterLink }
  }

  private static String textOf(RouterLink link) {
    link.getChildren().iterator().collect { it as com.vaadin.flow.component.Component }
        .findAll { it instanceof Span }.collect { (it as Span).getText() }.join(" ")
  }
}