package life.qbic.datamanager.views.login.passwordreset

import com.vaadin.flow.component.UI
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.data.binder.BinderValidationStatus
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.Router
import com.vaadin.flow.server.RouteRegistry
import com.vaadin.flow.server.VaadinService
import life.qbic.datamanager.views.register.UserRegistrationMain
import life.qbic.identity.api.UserInformationService
import spock.lang.Specification

/**
 * <b>The reset form must not answer "does this account exist?" in its field validation
 * (AUTH-NFR-03, #1681).</b>
 *
 * <p>The submit path was fixed first: {@code IdentityService.requestPasswordReset} returns the same
 * response for an unknown, an unconfirmed and a confirmed account, and the view renders one fixed
 * "check your inbox" screen. But the form itself still validated the address against the account
 * table and rejected an unregistered one with <em>"No user with the provided mail address is
 * known."</em> — before the visitor submitted anything at all.</p>
 *
 * <p>That is the same disclosure one step earlier, and it is worse in one respect: field validation
 * is a round-trip that the visitor can trigger repeatedly without submitting, so it is a cheap
 * enumeration primitive. A uniform submit response is worthless while the form answers the question
 * first.</p>
 */
class ResetPasswordComponentAccountOracleSpec extends Specification {

  UserInformationService userInformationService

  def setup() {
    // The component builds a RouterLink to the registration form, whose href is resolved through
    // the router at construction time (mirrors ResetPasswordMainSpec).
    def registry = Stub(RouteRegistry)
    registry.getTargetUrl(UserRegistrationMain, _ as RouteParameters) >> Optional.of('registration')
    def service = Stub(VaadinService)
    service.getRouter() >> new Router(registry)
    service.getContext() >> null
    VaadinService.setCurrent(service)
  }

  def cleanup() {
    VaadinService.setCurrent(null)
  }

  def "field validation never asks whether an account exists"() {
    given: 'a service that would answer, were it asked'
    userInformationService = Mock(UserInformationService)

    and:
    def component = new ResetPasswordComponent(userInformationService)

    when: 'the email field is validated for an address with no account'
    validate(component, 'nobody@example.org')

    then: 'the account-existence question is not asked at all'
    0 * userInformationService.isEmailAvailable(_)
    0 * userInformationService.findByEmail(_)
  }

  def "an unknown address passes field validation, so submit is what produces the generic outcome"() {
    given:
    userInformationService = Mock(UserInformationService) {
      isEmailAvailable(_) >> true   // "no such account", if the validator existed
      findByEmail(_) >> Optional.empty()
    }
    def component = new ResetPasswordComponent(userInformationService)

    when:
    def status = validate(component, 'nobody@example.org')

    then: 'no account-state error is attached to the field'
    status.hasErrors() == false
    !messagesOf(status).toLowerCase().contains('known')
    !messagesOf(status).toLowerCase().contains('not found')
  }

  def "a well-formed unknown address produces no error text that reveals account state"() {
    given:
    userInformationService = Mock(UserInformationService)
    def component = new ResetPasswordComponent(userInformationService)

    when:
    def status = validate(component, 'nobody@example.org')

    then:
    status.hasErrors() == false
  }

  def "a malformed address is still rejected, because that is about the input"() {
    given:
    userInformationService = Mock(UserInformationService)
    def component = new ResetPasswordComponent(userInformationService)

    when:
    def status = validate(component, 'not-an-email')

    then: 'the visitor who mistyped their own address is told'
    status.hasErrors()
  }

  // ---- helpers -------------------------------------------------------------------------------

  private static BinderValidationStatus<?> validate(ResetPasswordComponent component, String email) {
    def binder = binderOf(component)
    def emailField = fieldOf(component)
    emailField.setValue(email)
    return binder.validate()
  }

  private static Binder binderOf(ResetPasswordComponent component) {
    return component.@emailBinder
  }

  private static TextField fieldOf(ResetPasswordComponent component) {
    return component.@emailField
  }

  private static String messagesOf(BinderValidationStatus<?> status) {
    return status.getFieldValidationErrors().collect { it.message.orElse('') }.join(' ')
  }
}
