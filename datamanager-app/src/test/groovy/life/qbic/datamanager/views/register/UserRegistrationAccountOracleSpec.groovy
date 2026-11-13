package life.qbic.datamanager.views.register

import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.Router
import com.vaadin.flow.server.RouteRegistry
import com.vaadin.flow.server.VaadinService
import life.qbic.datamanager.views.login.passwordreset.ResetPasswordMain
import life.qbic.identity.api.Passwords
import life.qbic.identity.api.UserInformationService
import spock.lang.Specification

/**
 * <b>Registration must not answer "does this account exist?" (AUTH-NFR-03, FEAT-AUTH-FLOWS-03).</b>
 *
 * <p>The registration form used to disclose account existence in <em>two</em> places, and both had to
 * change together:</p>
 *
 * <ol>
 *   <li>the email field validator called {@code isEmailAvailable} and answered <em>"Email address
 *       already in use"</em> — a cheap enumeration primitive, because field validation is a
 *       round-trip the visitor can trigger repeatedly without submitting;</li>
 *   <li>the submit path rendered {@code UserExistsException} with the same sentence. Fixing only the
 *       first would have moved the disclosure one step later, and fixing only the second would have
 *       left the validator answering first.</li>
 * </ol>
 *
 * <p>These specs pin both halves, plus the password rule whose rendered message disagreed with the
 * rule actually enforced.</p>
 */
class UserRegistrationAccountOracleSpec extends Specification {

  UserInformationService userInformationService = Stub()

  def setup() {
    // The component builds a RouterLink to the reset form, whose href is resolved through the router at
    // construction time (same requirement as ResetPasswordMainSpec).
    def registry = Stub(RouteRegistry)
    registry.getTargetUrl(ResetPasswordMain, _ as RouteParameters) >> Optional.of('reset-password')
    def service = Stub(VaadinService)
    service.getRouter() >> new Router(registry)
    // Needed because the RouterLink resolves its href through the service context at construction.
    service.getContext() >> null
    VaadinService.setCurrent(service)
  }

  def cleanup() {
    VaadinService.setCurrent(null)
  }

  /** A component whose binder is populated the way a visitor would fill the form in. */
  private static Binder binderOf(UserRegistrationComponent component) {
    component.declaredFields
        .findAll { it.name.contains('registrationInformationBinder') }
        .collect { it.setAccessible(true); it.get(component) }
        .first() as Binder
  }

  def "the email field does not ask the account table whether the address is free"() {
    given: 'a mock that would record the question, were it asked'
    def accountTable = Mock(UserInformationService)
    def component = new UserRegistrationComponent(accountTable)

    when: 'the form is constructed and validated'
    component.@registrationInformationBinder.isValid()

    then: 'the account table is never consulted from the form'
    0 * accountTable.isEmailAvailable(_)
    0 * accountTable._
  }

  def "a registered address is not called out in field validation"() {
    given: 'a service that reports every address as taken'
    def taken = Stub(UserInformationService) {
      isEmailAvailable(_) >> false
      isUserNameAvailable(_) >> true
    }
    def component = new UserRegistrationComponent(taken)

    when: 'the email field is populated and validated'
    component.@email.setValue('someone@example.org')
    def status = component.@registrationInformationBinder.validate()

    then: 'no message mentions the address being in use'
    status.getFieldValidationErrors().every { error ->
      !error.getMessage().get().toLowerCase().contains('already in use')
    }
  }

  def "the password message states the minimum that is actually enforced"() {
    given:
    def component = new UserRegistrationComponent(userInformationService)

    expect: 'the rendered hint and the rejection share one source with the domain rule'
    component.@password.getHelperText() == Passwords.hint()
    component.@password.getHelperText().contains(Passwords.MIN_LENGTH.toString())

    and: 'a password one character too short is rejected with that same message'
    component.@password.setValue('a' * (Passwords.MIN_LENGTH - 1))
    def status = component.@registrationInformationBinder.validate()
    status.getFieldValidationErrors().any { error ->
      error.getMessage().get() == Passwords.tooShortMessage()
    }

    and: 'a password of the minimum length is accepted'
    component.@password.setValue('a' * Passwords.MIN_LENGTH)
    component.@registrationInformationBinder.validate().getFieldValidationErrors()
        .every { error -> !error.getMessage().get().contains('Password') }
  }

  def "the ORCID form carries the same email rule, so the two forms cannot drift"() {
    given: 'an account table that would answer "taken", were it asked'
    def accountTable = Mock(UserInformationService)
    def component = new UserRegistrationOrcIdComponent(accountTable)

    when:
    component.@registrationInformationBinder.isValid()

    then: 'the form never asks it'
    0 * accountTable.isEmailAvailable(_)
  }
}
