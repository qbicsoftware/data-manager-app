package life.qbic.datamanager.security

import com.vaadin.flow.component.html.Span
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.Router
import com.vaadin.flow.server.RouteRegistry
import com.vaadin.flow.server.VaadinService
import life.qbic.datamanager.views.login.LoginLayout
import life.qbic.datamanager.views.register.UserRegistrationMain
import spock.lang.Specification

/**
 * <b>ORCID must not be a way into the application (AUTH-R-07).</b>
 *
 * <p>This is the guard for the whole story. The milestone's exit criterion is "no password-only login",
 * which only becomes a meaningful statement once there is exactly one password login and no second
 * path beside it. While {@code oauth2Login()} is configured, an ORCID assertion produces an
 * authenticated session, and every second-factor check is applied to the <em>other</em> path.</p>
 *
 * <p>Behavioural specs cannot pin this. A suite can pass while the filter chain still accepts provider
 * logins, because nothing exercises the provider redirect. So the assertions here are structural: they
 * read the shipped configuration and the shipped bytecode.</p>
 *
 * <p>Four things must stay true, and each has been true-and-then-broken in this codebase's history:</p>
 * <ol>
 *   <li><b>The chain has a client grant, not a login.</b> {@code oauth2Client()} stores an
 *       {@code OAuth2AuthorizedClient} for the current principal; {@code oauth2Login()} mints an
 *       {@code Authentication}. Only the first is compatible with linking-never-logs-in.</li>
 *   <li><b>The login page offers no provider affordance.</b> A card that starts a round trip is a second
 *       entry point even if the chain would reject it — it advertises the path.</li>
 *   <li><b>The provider principal type is gone.</b> {@code QbicOidcUser} was the shape an ORCID session
 *       had; whether it exists is a direct read of whether that session can exist.</li>
 *   <li><b>The provider registration route is gone.</b> That is what created accounts with no password.</li>
 * </ol>
 */
class OrcidIsNotALoginPathSpec extends Specification {

  private static String sourceOf(Class<?> type) {
    def resource = type.name.replace('.', '/') + '.class'
    def stream = type.classLoader.getResourceAsStream(resource)
    assert stream != null: "no bytecode for $type"
    // The constant pool is plain text in the class file, so reading it as Latin-1 is enough to see
    // whether a method reference exists. This is deliberately not a decompiler: it asserts about what
    // is deployed, which is the property that matters.
    return stream.withCloseable { new String(it.bytes, 'ISO-8859-1') }
  }

  private static List descendants(com.vaadin.flow.component.Component component) {
    return component.children.toList().collectMany { [it] + descendants(it) }
  }

  private static boolean classExists(String binaryName) {
    typeExists(binaryName.replace('.', '/') + '.class')
  }

  private static boolean typeExists(String resourcePath) {
    return OrcidIsNotALoginPathSpec.classLoader.getResource(resourcePath) != null
  }

  def "the security chain configures a client grant and not a login"() {
    given:
    def bytecode = sourceOf(SecurityConfiguration)

    expect: 'the OAuth2 client DSL is present, because linking needs it'
    bytecode.contains('oauth2Client')

    and: 'the OAuth2 login DSL is absent — this is the assertion the whole story exists for'
    !bytecode.contains('oauth2Login')
  }

  def cleanup() {
    VaadinService.setCurrent(null)
  }

  def "the login page offers no provider affordance"() {
    given: 'the router scaffolding the layout needs, because it builds a registration RouterLink'
    def registry = Stub(RouteRegistry)
    registry.getTargetUrl(UserRegistrationMain, _ as RouteParameters) >> Optional.of("registration")
    def service = Stub(VaadinService)
    service.getRouter() >> new Router(registry)
    service.getContext() >> null
    VaadinService.setCurrent(service)

    and:
    def layout = new LoginLayout('context')

    when: 'the page is rendered'
    def texts = descendants(layout).findAll { it instanceof Span }
        .collect { (it.text ?: '').toLowerCase() }

    then: 'no link, card or label invites the visitor to sign in with ORCID'
    //
    // Mentioning ORCID is allowed and necessary: the page explains that it no longer signs people in.
    // What must not exist is an *affordance* — an invitation or a control that starts a provider login.
    // The distinction is the whole point of this spec, so it is asserted on the invitation wording
    // rather than on the string "orcid".
    texts.every { !it.contains('login with orcid') }
    texts.every { !it.contains('sign in with orcid') }
    texts.every { !it.contains('login with') }

    and: 'nothing on the page links to the OAuth2 authorization endpoint'
    descendants(layout).every { component ->
      def href = component.element.getAttribute('href')
      href == null || !href.contains('/oauth2/authorization')
    }

    and: 'the ordinary registration route is still offered, via its RouterLink'
    descendants(layout).any {
      it instanceof com.vaadin.flow.router.RouterLink &&
          (it.text ?: '').toLowerCase().contains('register')
    }
  }

  def "the provider principal type is gone from the build output"() {
    expect: 'no ORCID-derived principal exists to be produced or branched on'
    !classExists('life.qbic.projectmanagement.application.authorization.QbicOidcUser')

    and: 'the login user-details service that produced it is gone too'
    !classExists('life.qbic.datamanager.security.OidcUserDetailsService')

    and: 'the login success handler that could link-and-continue is gone'
    !classExists('life.qbic.datamanager.security.StoredRequestAwareOidcAuthenticationSuccessHandler')
  }

  def "the provider registration route no longer exists"() {
    expect: 'the route class is gone, so the route cannot be resolved'
    !classExists('life.qbic.datamanager.views.register.RegistrationOrcIdMain')

    and: 'and no route constant points at it'
    !sourceOf(life.qbic.datamanager.views.AppRoutes).contains('register/oidc')
  }

  def "an account cannot be created without a password"() {
    expect: 'the domain factory that built a passwordless account is gone'
    !sourceOf(life.qbic.identity.domain.model.User).contains('createOidc')

    and: 'and the application service that called it is gone'
    !sourceOf(life.qbic.identity.application.user.IdentityService).contains('registerOpenIdUser')
  }
}
