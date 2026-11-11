package life.qbic.datamanager.views.register

import com.vaadin.flow.component.HasText
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEvent
import com.vaadin.flow.router.Location
import com.vaadin.flow.router.NavigationTrigger
import com.vaadin.flow.router.QueryParameters
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.Router
import com.vaadin.flow.server.RouteRegistry
import com.vaadin.flow.server.VaadinService
import life.qbic.datamanager.views.login.LoginLayout
import life.qbic.identity.api.VerificationLinkService
import life.qbic.identity.application.user.IdentityService
import life.qbic.identity.application.user.policy.EmailConfirmationLinkSupplier
import spock.lang.Specification

import java.time.Instant

/**
 * <b>Confirmation route behaviour (AUTH-R-05).</b>
 *
 * <p>The property that matters most is the one neither a DDL review nor a service spec can show:
 * retrieving the page must not confirm anything. Only the explicit button press may consume the
 * token, so a mail scanner or link prefetcher cannot spend the user's link before they see it.</p>
 */
class ConfirmEmailMainSpec extends Specification {

    Router router
    UI ui

    VerificationLinkService linkService
    IdentityService identityService
    life.qbic.identity.domain.model.User testUser
    ConfirmEmailMain view

    def setup() {
        // Router has a real constructor that asserts on null; build it the same way LoginLayoutSpec
        // does rather than mocking it.
        def registry = Stub(RouteRegistry)
        // The view's "go to login" link resolves its target through the registry at construction
        // time, so the login route has to appear registered.
        registry.getTargetUrl(LoginLayout, _ as RouteParameters) >> Optional.of("login")
        router = new Router(registry)
        ui = Stub(UI)
        // The view builds a RouterLink, whose href is resolved from the current VaadinService at
        // construction time. Install a minimal one so rendering works headlessly.
        def service = Stub(VaadinService)
        service.getRouter() >> router
        // Explicitly null: a Spock stub would otherwise return a mock VaadinContext, and Vaadin
        // then tries to resolve an ApplicationConfiguration through it.
        service.getContext() >> null
        VaadinService.setCurrent(service)
        linkService = Mock(VerificationLinkService)
        // IdentityService is final, so Spock cannot mock it without Mockito on the classpath.
        // A real instance over a stub repository that CONTAINS the user is enough: this spec's
        // property is whether the token was spent and what gets rendered afterwards.
        testUser = life.qbic.identity.domain.model.User.create(
                life.qbic.identity.domain.model.FullName.from("Test User"),
                life.qbic.identity.domain.model.EmailAddress.from("test@example.com"),
                "testuser",
                life.qbic.identity.domain.model.EncryptedPassword.from("aStrongPassw0rd!".toCharArray()))
        def userRepository = Stub(life.qbic.identity.domain.repository.UserRepository)
        userRepository.findById(_) >> Optional.of(testUser)
        userRepository.findByEmail(_) >> Optional.of(testUser)
        identityService = new IdentityService(userRepository)
        view = new ConfirmEmailMain(linkService, identityService,
                new EmailConfirmationLinkSupplier("https", "example.org", 443, "",
                        "/registration/confirm", "token"), "token")
    }

    def "retrieving the page with a valid token consumes nothing"() {
        when: "the page is entered — a mail scanner, prefetcher, or the user opening the link"
        view.beforeEnter(eventWithToken("tok-123"))

        then: "no token is consumed and no account state changes"
        0 * linkService.consume(*_)
    }

    def "retrieving the page repeatedly still consumes nothing"() {
        when:
        3.times { view.beforeEnter(eventWithToken("tok-123")) }

        then:
        0 * linkService.consume(*_)
    }

    def "a missing token renders the failure state without consuming anything"() {
        when:
        view.beforeEnter(eventWithToken(null))

        then:
        0 * linkService.consume(*_)
        renderedText(view).contains("no longer valid")
    }

    def "merely retrieving the page shows the prompt, not a confirmation result"() {
        given:
        linkService.consume("tok-123", VerificationLinkService.Purpose.EMAIL_CONFIRMATION,
                _ as Instant) >> Optional.of("u-1")

        when: "the page is only retrieved"
        view.beforeEnter(eventWithToken("tok-123"))

        then: "the user is asked to act, and nothing claims to have happened yet"
        renderedText(view).contains("Confirm your email address")
        !renderedText(view).contains("Email address confirmed")
    }

    def cleanup() {
        VaadinService.setCurrent(null)
    }

    // ------------------------------------------------- the click path (consumed token)

    def "a consumed token on click renders the failure state and does not throw"() {
        given: "the token is already used, so consumption returns empty"
        linkService.consume("tok-used", VerificationLinkService.Purpose.EMAIL_CONFIRMATION,
                _ as Instant) >> Optional.empty()
        view.beforeEnter(eventWithToken("tok-used"))

        when: "the user presses confirm"
        view.confirm()

        then: "it renders the failure panel — an unusable link must never blank the page"
        noExceptionThrown()
        renderedText(view).contains("no longer valid")
        !renderedText(view).contains("Confirm your email address")
    }

    def "an expired token on click renders the same failure state"() {
        given:
        linkService.consume("tok-old", VerificationLinkService.Purpose.EMAIL_CONFIRMATION,
                _ as Instant) >> Optional.empty()
        view.beforeEnter(eventWithToken("tok-old"))

        when:
        view.confirm()

        then: "identical wording, so the visitor cannot tell expired from consumed (AUTH-NFR-03)"
        renderedText(view).contains("no longer valid")
    }

    def "a valid token on click renders the success state"() {
        given:
        linkService.consume("tok-ok", VerificationLinkService.Purpose.EMAIL_CONFIRMATION,
                _ as Instant) >> Optional.of(testUser.id().get())
        view.beforeEnter(eventWithToken("tok-ok"))

        when:
        view.confirm()

        then:
        renderedText(view).contains("Email address confirmed")
    }

    def "a second click after success does nothing"() {
        given: "one successful confirmation"
        linkService.consume("tok-ok", VerificationLinkService.Purpose.EMAIL_CONFIRMATION,
                _ as Instant) >> Optional.of(testUser.id().get())
        view.beforeEnter(eventWithToken("tok-ok"))
        view.confirm()

        when: "the user clicks again"
        view.confirm()

        then: "the token is not consumed a second time — the double-click guard holds"
        0 * linkService.consume(*_)
    }

    // ------------------------------------------------------------------ helpers

    private static String renderedText(component) {
        def sink = []
        collectText(component, sink)
        return sink.join(" ")
    }

    private static void collectText(component, List<String> sink) {
        if (component instanceof HasText) {
            try {
                sink << component.getText()
            } catch (Exception ignored) {
                // Not attached to a UI; nothing to read.
            }
        }
        component.getChildren()?.each { collectText(it, sink) }
    }

    private BeforeEvent eventWithToken(String token) {
        QueryParameters params = token == null
                ? QueryParameters.empty()
                : QueryParameters.simple([token: token])
        // A real BeforeEnterEvent, not a mock: this classpath has no Objenesis and the Vaadin
        // event classes have no usable no-arg constructor. Same approach as LoginLayoutSpec.
        return new BeforeEnterEvent(router, NavigationTrigger.PROGRAMMATIC,
                new Location("registration/confirm", params), ConfirmEmailMain, ui, List.of())
    }
}
