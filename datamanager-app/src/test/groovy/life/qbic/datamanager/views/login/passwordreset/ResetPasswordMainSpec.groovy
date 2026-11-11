package life.qbic.datamanager.views.login.passwordreset

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.Router
import com.vaadin.flow.server.RouteRegistry
import com.vaadin.flow.server.VaadinService
import life.qbic.datamanager.views.login.LoginLayout
import life.qbic.identity.application.user.IdentityService
import life.qbic.identity.domain.repository.UserDataStorage
import life.qbic.identity.domain.repository.UserRepository
import spock.lang.Specification

/**
 * <b>The reset request must not reveal whether an account exists (AUTH-NFR-03, #1674).</b>
 *
 * <p>Before this, the form surfaced "User not found" and "User not active" verbatim, which made it
 * an account-enumeration oracle: knowing that an address is registered is the first step of every
 * credential attack, and knowing an account is unconfirmed identifies the weaker target.</p>
 */
class ResetPasswordMainSpec extends Specification {

    ResetPasswordComponent resetPasswordComponent
    ResetEmailSentComponent resetEmailSentComponent
    ResetPasswordMain view
    IdentityService identityService

    def setup() {
        def registry = Stub(RouteRegistry)
        registry.getTargetUrl(LoginLayout, _ as RouteParameters) >> Optional.of("login")
        // ResetPasswordComponent also builds a link to the registration form.
        registry.getTargetUrl(life.qbic.datamanager.views.register.UserRegistrationMain,
                _ as RouteParameters) >> Optional.of("registration")
        def router = new Router(registry)
        def service = Stub(VaadinService)
        service.getRouter() >> router
        service.getContext() >> null
        VaadinService.setCurrent(service)

        // Unknown address -> the service reports failure internally.
        def storage = Stub(UserDataStorage)
        storage.findUserByEmailAddress(_) >> []
        identityService = new IdentityService(new UserRepository(storage))

        // ResetPasswordComponent requires a UserInformationService, which it uses for its own
        // field validation only.
        resetPasswordComponent = new ResetPasswordComponent(
                Stub(life.qbic.identity.api.UserInformationService))
        resetEmailSentComponent = new ResetEmailSentComponent()
        view = new ResetPasswordMain(identityService, resetPasswordComponent, resetEmailSentComponent)
    }

    def cleanup() {
        VaadinService.setCurrent(null)
    }

    def "an unknown address shows the same 'check your inbox' screen as a known one"() {
        when: "an address with no account is submitted"
        view.resetPassword("nobody@example.com")

        then: "no account-state message is shown"
        !renderedText(resetPasswordComponent).toLowerCase().contains("not found")
        !renderedText(resetPasswordComponent).toLowerCase().contains("not active")

        and: "the generic outcome is shown instead"
        resetEmailSentComponent.isVisible()
        !resetPasswordComponent.isVisible()
    }

    def "a malformed address is still reported, because that is about the input not the account"() {
        when:
        view.resetPassword("not-an-email")

        then:
        renderedText(resetPasswordComponent).toLowerCase().contains("invalid")

        and: "the form stays visible so the visitor can correct it"
        resetPasswordComponent.isVisible()
    }

    private static String renderedText(component) {
        def sink = []
        collect(component, sink)
        return sink.join(" ")
    }

    private static void collect(component, List<String> sink) {
        if (component instanceof com.vaadin.flow.component.HasText) {
            try { sink << component.getText() } catch (Exception ignored) { }
        }
        component.getChildren()?.each { collect(it, sink) }
    }
}
