package life.qbic.datamanager.views.general

import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.Router
import com.vaadin.flow.router.RouterLink
import com.vaadin.flow.server.RouteRegistry
import com.vaadin.flow.server.VaadinService
import life.qbic.datamanager.views.projects.overview.ProjectOverviewMain
import spock.lang.Specification

/**
 * Unit tests for the {@link HomeLink} navbar home affordance.
 *
 * <p>HomeLink is a {@link RouterLink} and therefore resolves its href from the
 * {@link VaadinService} router registry at construction time. These specs
 * install a minimal service/registry stub that maps {@link ProjectOverviewMain}
 * to its production route {@code projects/list}, mirroring route resolution
 * without booting a Spring context.</p>
 */
class HomeLinkSpec extends Specification {

  private static final String OVERVIEW_ROUTE = "projects/list"

  private VaadinService stubbedService

  def setup() {
    RouteRegistry registry = Stub(RouteRegistry)
    registry.getTargetUrl(ProjectOverviewMain, _ as RouteParameters) >> Optional.of(OVERVIEW_ROUTE)
    Router router = new Router(registry)
    VaadinService service = Stub(VaadinService)
    service.getRouter() >> router
    // A null context short-circuits Vaadin's dev-mode component tracker
    // (ComponentTracker.isDisabled), avoiding any ApplicationConfiguration lookup.
    service.getContext() >> null
    stubbedService = service
    VaadinService.setCurrent(stubbedService)
  }

  def cleanup() {
    VaadinService.setCurrent(null)
  }

  def "is a RouterLink so it renders a real link element"() {
    expect:
    new HomeLink() instanceof RouterLink
  }

  def "links to the logged-in user's home page (project overview)"() {
    given: "a home link with a router that resolves the overview route"
    def home = new HomeLink()

    expect: "the href points to the project overview route"
    home.getHref() == OVERVIEW_ROUTE
  }

  def "carries an accessible label describing its purpose"() {
    expect:
    new HomeLink().element.getAttribute("aria-label") == "Go to home page"
  }

  def "renders exactly one home icon"() {
    when: "the home link is expanded"
    def home = new HomeLink()
    def icons = home.getChildren().findAll { it instanceof Icon }.toList()

    then:
    icons.size() == 1
    icons[0].element.getAttribute("icon").contains("home")
  }

  def "declares a css class so the theme can style it consistently"() {
    expect:
    new HomeLink().element.classList.contains("home-link")
  }
}