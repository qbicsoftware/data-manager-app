package life.qbic.datamanager.views.login

import com.vaadin.flow.component.UI
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
    UI.setCurrent(ui)

    def identityService = new IdentityService(Stub(UserRepository))
    loginLayout = new LoginLayout(new LoginHandler("confirm-email"), identityService, "context")
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

    when: "the login view is entered"
    loginLayout.beforeEnter(event)

    then: "the navigation is rerouted to the project overview"
    event.reroutedTo == ProjectOverviewMain
  }

  def "anonymous session keeps access to the login form"() {
    given: "Spring Security's anonymous session"
    SecurityContextHolder.getContext().setAuthentication(
        new AnonymousAuthenticationToken("key", "anonymousUser",
            AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")))
    def event = beforeEnterEvent()

    when: "the login view is entered"
    loginLayout.beforeEnter(event)

    then: "the login form stays available"
    event.reroutedTo == null
  }

  def "request without authentication keeps access to the login form"() {
    given: "no authentication in the security context"
    SecurityContextHolder.clearContext()
    def event = beforeEnterEvent()

    when: "the login view is entered"
    loginLayout.beforeEnter(event)

    then: "the login form stays available"
    event.reroutedTo == null
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
