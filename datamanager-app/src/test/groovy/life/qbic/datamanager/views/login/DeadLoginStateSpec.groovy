package life.qbic.datamanager.views.login

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.page.History
import com.vaadin.flow.component.page.Page
import com.vaadin.flow.dom.Element
import com.vaadin.flow.router.BeforeEvent
import com.vaadin.flow.router.Location
import com.vaadin.flow.router.QueryParameters
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.Router
import com.vaadin.flow.server.RouteRegistry
import com.vaadin.flow.server.VaadinService
import life.qbic.datamanager.views.notifications.InformationMessage
import life.qbic.datamanager.views.projects.overview.ProjectOverviewMain
import life.qbic.datamanager.views.register.EmailConfirmationMain
import life.qbic.datamanager.views.register.UserRegistrationMain
import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>The login page must not render state that nothing produces (FEAT-AUTH-FLOWS-04).</b>
 *
 * <p>{@code LoginLayout} showed a "Registration mail sent / please check your inbox" notice whenever the
 * URL carried {@code ?userRegistered}. Nothing in the application produced that parameter then, and
 * nothing produces it now: registration navigates to
 * {@code /register/pending-email-confirmation}, which states the same thing on a page of its own.</p>
 *
 * <p>A dead state is not harmless. It looks like a feature, so it is preserved and maintained; it invites
 * a future change to "wire it up" through a query parameter, which is exactly the pattern the rest of
 * this slice removed (a user id as a credential, state changing on GET); and it teaches the next reader
 * that {@code /login?userRegistered=…} is a supported entry point.</p>
 *
 * <p>These specs pin both directions: the parameter renders nothing, and no code path produces it.</p>
 */
class DeadLoginStateSpec extends Specification {

  UI ui
  Page page
  History history
  Router router

  def setup() {
    RouteRegistry registry = Stub(RouteRegistry)
    registry.getTargetUrl(UserRegistrationMain, _ as RouteParameters) >> Optional.of("registration")
    registry.getTargetUrl(_, _ as RouteParameters) >> Optional.of("login")
    router = new Router(registry)
    VaadinService service = Stub(VaadinService)
    service.getRouter() >> router
    service.getContext() >> null
    VaadinService.setCurrent(service)
    // The OIDC card builds a stream resource through UI.getCurrentOrThrow(), so a UI must be installed
    // (same scaffolding as LoginLayoutSpec).
    ui = Mock(UI)
    ui.getElement() >> new Element("div")
    page = Mock(Page, constructorArgs: [ui])
    history = Mock(History, constructorArgs: [ui])
    ui.getPage() >> page
    page.getHistory() >> history
    UI.setCurrent(ui)
  }

  def cleanup() {
    UI.setCurrent(null)
    VaadinService.setCurrent(null)
  }

  @Unroll
  def "login renders no notice for ?#parameter, which nothing produces"() {
    given: 'the login view, entered with the query parameter that used to trigger a notice'
    def view = new LoginLayout("context")

    when:
    view.handle(event(parameter))

    then: 'no notification of any kind is rendered'
    notifications(view).isEmpty()

    where:
    parameter << ['userRegistered', 'userRegistered=true', 'confirm-email']
  }

  def "no source file produces the userRegistered query parameter"() {
    given: 'the compiled views that could navigate to the login page with a parameter'
    def candidates = [LoginLayout, UserRegistrationMain, EmailConfirmationMain,
                      ProjectOverviewMain]

    expect: 'none of them contains the parameter name, so the state cannot come back by accident'
    candidates.every { !bytecodeOf(it).contains('userRegistered') }
  }

  def "the notice wording survives only on the page that actually sends the mail"() {
    given: 'the pending-confirmation page, which is where registration lands'
    def loginSource = bytecodeOf(LoginLayout)
    def pendingSource = bytecodeOf(EmailConfirmationMain)

    expect: 'the login page no longer promises an email'
    !loginSource.contains('Registration mail sent')

    and: 'the page that dispatches the email still states it'
    pendingSource.contains('verification link')
  }

  // ---- helpers -----------------------------------------------------------------------------------

  private static List notifications(Component view) {
    return ([view] + view.children.toList().collectMany { notifications(it) })
        .findAll { it instanceof InformationMessage }
  }

  private BeforeEvent event(String parameter) {
    return new QueryEvent(router, ui, parameter)
  }

  private static String bytecodeOf(Class type) {
    def resource = type.name.replace('.', '/') + '.class'
    return type.classLoader.getResourceAsStream(resource)
        .withCloseable { new String(it.bytes, 'ISO-8859-1') }
  }

  /** A {@link BeforeEvent} over a location with a single query parameter, without a router round-trip. */
  private static class QueryEvent extends BeforeEvent {

    QueryEvent(Router router, UI ui, String parameter) {
      super(router, com.vaadin.flow.router.NavigationTrigger.PROGRAMMATIC,
          new Location('login', QueryParameters.simple([(parameter): 'true'])),
          LoginLayout, ui, List.of())
    }
  }
}
