package life.qbic.datamanager.views.login.passwordreset

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.Location
import com.vaadin.flow.router.NavigationTrigger
import com.vaadin.flow.router.QueryParameters
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.Router
import com.vaadin.flow.server.RouteRegistry
import com.vaadin.flow.server.VaadinService
import life.qbic.identity.api.VerificationLinkService
import life.qbic.datamanager.views.login.LoginLayout
import life.qbic.identity.application.user.IdentityService
import life.qbic.identity.domain.repository.UserRepository
import spock.lang.Specification

import java.lang.reflect.Field
import java.time.Instant

/**
 * <b>The reset form must be reachable only by consuming a token (AUTH-R-05).</b>
 *
 * <p>The regression this guards: the reset link used to carry the user's persistent id as
 * {@code ?user-id=<uuid>}, and the view read that parameter directly. Anyone holding a user id — a
 * value that also appears in ACL records — could open the form and set a new password. This spec
 * fails if the view ever goes back to trusting a URL parameter instead of the token.</p>
 */
class SetNewPasswordMainSpec extends Specification {

    VerificationLinkService linkService
    SetNewPasswordComponent setNewPasswordComponent
    NewPasswordSetComponent newPasswordSetComponent
    SetNewPasswordMain view

    Router router

    def setup() {
        def registry = Stub(RouteRegistry)
        // The "Go to login" link resolves its target through the registry at construction time.
        registry.getTargetUrl(LoginLayout, _ as RouteParameters) >> Optional.of("login")
        router = new Router(registry)
        def service = Stub(VaadinService)
        service.getRouter() >> router
        service.getContext() >> null
        VaadinService.setCurrent(service)

        linkService = Mock(VerificationLinkService)
        setNewPasswordComponent = new SetNewPasswordComponent()
        newPasswordSetComponent = new NewPasswordSetComponent()
        def identityService = new IdentityService(Stub(UserRepository))
        view = new SetNewPasswordMain(identityService, setNewPasswordComponent,
                newPasswordSetComponent, linkService)
        view.tokenParameter = "token"
    }

    def cleanup() {
        VaadinService.setCurrent(null)
    }

    def "a user-id in the URL authorises nothing"() {
        when: "the OLD link format is opened: ?user-id=<uuid>"
        view.setParameter(eventWith(["user-id": "3f2a-user-uuid"]), null)

        then: "no token is consumed, so no account is resolved"
        0 * linkService.consume(*_)

        and: "the password form stays hidden"
        !setNewPasswordComponent.isVisible()
        readField(view, "currentUserId") == null
        readField(view, "resetAuthorised") == false
    }

    def "a valid token resolves the user and reveals the form"() {
        given:
        linkService.consume("tok-abc", VerificationLinkService.Purpose.PASSWORD_RESET,
                _ as Instant) >> Optional.of("u-1")

        when:
        view.setParameter(eventWith([token: "tok-abc"]), null)

        then: "the account comes from the token, not from the URL"
        readField(view, "currentUserId") == "u-1"
        readField(view, "resetAuthorised") == true
        setNewPasswordComponent.isVisible()
        !readField(view, "invalidLinkComponent").isVisible()
    }

    def "an unusable token leaves the form hidden"() {
        given:
        linkService.consume("tok-expired", VerificationLinkService.Purpose.PASSWORD_RESET,
                _ as Instant) >> Optional.empty()

        when:
        view.setParameter(eventWith([token: "tok-expired"]), null)

        then:
        !setNewPasswordComponent.isVisible()
        readField(view, "currentUserId") == null
        readField(view, "resetAuthorised") == false

        and: "consumed/expired tokens must not render a blank page"
        readField(view, "invalidLinkComponent").isVisible()
    }

    def "a missing token leaves the form hidden"() {
        when:
        view.setParameter(eventWith([:]), null)

        then:
        0 * linkService.consume(*_)
        !setNewPasswordComponent.isVisible()
        readField(view, "resetAuthorised") == false

        and:
        readField(view, "invalidLinkComponent").isVisible()
    }

    // ------------------------------------------------------------------ helpers

    private static Object readField(Object target, String name) {
        Field f = target.getClass().getDeclaredField(name)
        f.setAccessible(true)
        return f.get(target)
    }

    private BeforeEnterEvent eventWith(Map<String, String> params) {
        def query = params.isEmpty() ? QueryParameters.empty() : QueryParameters.simple(params)
        return new BeforeEnterEvent(router, NavigationTrigger.PROGRAMMATIC,
                new Location("registration/new-password", query),
                SetNewPasswordMain, Stub(UI), List.of())
    }
}
