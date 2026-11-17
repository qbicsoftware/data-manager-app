package life.qbic.datamanager.views.login

import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.page.History
import com.vaadin.flow.component.page.Page
import com.vaadin.flow.dom.Element
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.Location
import com.vaadin.flow.router.NavigationTrigger
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.Router
import com.vaadin.flow.server.RouteRegistry
import com.vaadin.flow.server.VaadinService
import life.qbic.datamanager.views.projects.overview.ProjectOverviewMain
import life.qbic.datamanager.views.register.UserRegistrationMain
import life.qbic.identity.application.user.IdentityService
import life.qbic.identity.domain.repository.UserRepository
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification

/**
 * Unit tests for the login view's access guard.
 *
 * <p>An authenticated user must not be presented with the login form again (issue #1304), so
 * {@link LoginLayout#beforeEnter(BeforeEnterEvent)} reroutes such users to the project overview.
 * Anonymous sessions (Spring Security's placeholder for unauthenticated requests) must still see
 * the login form.</p>
 */
class LoginLayoutSpec extends Specification {

  // Held as a field so the mock is not garbage collected: Vaadin stores the current UI in a
  // weak reference, so a local-only mock could disappear mid-test.
  UI ui
  Page page
  History history
  Router router

  LoginLayout loginLayout

  def setup() {
    // The registration section builds a RouterLink whose href is resolved from the router at
    // construction time, so a minimal service/registry stub is installed (mirroring
    // GroupsMainLayoutSpec).
    RouteRegistry registry = Stub(RouteRegistry)
    registry.getTargetUrl(UserRegistrationMain, _ as RouteParameters) >> Optional.of("registration")
    router = new Router(registry)
    VaadinService service = Stub(VaadinService)
    service.getRouter() >> router
    service.getContext() >> null
    VaadinService.setCurrent(service)
    // The OIDC registration section embeds an Image whose download handler builds a stream
    // resource URI through UI.getCurrentOrThrow(), so a current UI must be installed.
    ui = Mock(UI)
    // Page(UI) registers a page-visibility listener on ui.getElement(), so that element must be
    // real; Page/History have no no-arg constructors and this classpath has no Objenesis, so they
    // are created through their real constructors via constructorArgs.
    ui.getElement() >> new Element("div")
    page = Mock(Page, constructorArgs: [ui])
    history = Mock(History, constructorArgs: [ui])
    ui.getPage() >> page
    page.getHistory() >> history
    UI.setCurrent(ui)

    // LoginLayout no longer takes IdentityService: email confirmation moved to
    // /registration/confirm (ConfirmEmailMain) and no longer happens on GET, so the login view has
    // nothing to confirm and no use for the service. See AUTH-R-05 / decision B1.
    loginLayout = new LoginLayout("context")
  }

  def cleanup() {
    SecurityContextHolder.clearContext()
    UI.setCurrent(null)
    VaadinService.setCurrent(null)
  }

  def "authenticated user is rerouted to the project overview instead of seeing the login form"() {
    given: "an authenticated user"
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("user", "n/a",
            AuthorityUtils.createAuthorityList("ROLE_USER")))
    def event = beforeEnterEvent()
    Location updatedLocation

    when: "the login view is entered"
    loginLayout.beforeEnter(event)

    then: "the navigation is rerouted to the project overview"
    event.reroutedTo == ProjectOverviewMain

    and: "the browser URL is updated to the projects route"
    1 * history.replaceState(null, _ as Location) >> { args -> updatedLocation = args[1] }
    updatedLocation.path == "projects/list"
  }

  def "anonymous session keeps access to the login form"() {
    given: "Spring Security's anonymous session"
    SecurityContextHolder.getContext().setAuthentication(
        new AnonymousAuthenticationToken("key", "anonymousUser",
            AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")))
    def event = beforeEnterEvent()

    when: "the login view is entered"
    loginLayout.beforeEnter(event)

    then: "the login form stays available and the URL is left alone"
    event.reroutedTo == null
    0 * history.replaceState(_, _)
  }

  def "request without authentication keeps access to the login form"() {
    given: "no authentication in the security context"
    SecurityContextHolder.clearContext()
    def event = beforeEnterEvent()

    when: "the login view is entered"
    loginLayout.beforeEnter(event)

    then: "the login form stays available and the URL is left alone"
    event.reroutedTo == null
    0 * history.replaceState(_, _)
  }

  def "the login page tells returning ORCID users how to get in, without naming any account"() {
    given: 'the page as a new visitor sees it'
    def texts = descendants(loginLayout).findAll { it instanceof Span }
        .collect { (it.text ?: '').toLowerCase() }

    expect: 'there is a notice pointing at the password reset'
    texts.any { it.contains('forgot your password') }

    and: 'it mentions ORCID, because that is the account the reader remembers using'
    texts.any { it.contains('orcid') }

    and: 'it discloses nothing about any account: no address, no "you had", no condition'
    texts.every { !it.contains('@') }
    texts.every { !it.contains('your account exists') }
    texts.every { !it.contains('orcid account') }

    and: 'it is unconditional — the same text for every visitor (AUTH-NFR-03)'
    // The notice is rendered in the constructor, not behind a check on a query parameter or a lookup.
    // If it ever becomes conditional, this assertion should be replaced by one that proves the
    // condition cannot be probed — but it must not quietly become conditional.
    texts.count { it.contains('forgot your password') } == 1
  }

  def "the login page still offers no ORCID sign-in"() {
    given:
    def texts = descendants(loginLayout).findAll { it instanceof Span }
        .collect { (it.text ?: '').toLowerCase() }

    expect: 'the notice explains ORCID is not a login; it must not invite one'
    texts.every { !it.contains('login with orcid') }
    texts.every { !it.contains('sign in with orcid') }
  }

  private static List descendants(com.vaadin.flow.component.Component component) {
    return component.children.toList().collectMany { [it] + descendants(it) }
  }

  private CapturingBeforeEnterEvent beforeEnterEvent() {
    new CapturingBeforeEnterEvent(router, ui)
  }

  /**
   * A {@link BeforeEnterEvent} that records the reroute target instead of resolving it against a
   * router, which keeps the test independent of the routing registry.
   */
  private static class CapturingBeforeEnterEvent extends BeforeEnterEvent {

    Class reroutedTo

    CapturingBeforeEnterEvent(Router router, UI ui) {
      super(router, NavigationTrigger.PROGRAMMATIC, new Location("login"), LoginLayout, ui, List.of())
    }

    @Override
    void rerouteTo(Class routeTargetType) {
      this.reroutedTo = routeTargetType
    }
  }
}
