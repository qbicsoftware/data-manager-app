package life.qbic.datamanager.views.register

import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.Router
import com.vaadin.flow.server.RouteRegistry
import com.vaadin.flow.server.VaadinService
import life.qbic.datamanager.views.login.passwordreset.ResetPasswordMain
import life.qbic.datamanager.views.register.UserRegistrationComponent.Mode
import life.qbic.identity.api.Passwords
import life.qbic.identity.api.UserInformationService
import life.qbic.identity.api.UserNames
import spock.lang.Specification
import spock.lang.Unroll

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
 *
 * <p><b>Both modes are covered by the same assertions.</b> The two forms were once separate classes
 * with a copy each of the email validator, so the oracle could have come back in either. They are now
 * one component selected by {@link Mode}, and the {@code @Unroll} below exists to keep it that way: if
 * a future change reintroduces a per-mode fork, the ORCID case fails.</p>
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

  private static Binder binderOf(UserRegistrationComponent component) {
    component.declaredFields
        .findAll { it.name.contains('registrationInformationBinder') }
        .collect { it.setAccessible(true); it.get(component) }
        .first() as Binder
  }

  @Unroll
  def "the email field does not ask the account table whether the address is free (#mode)"() {
    given: 'a mock that would record the question, were it asked'
    def accountTable = Mock(UserInformationService)
    def component = new UserRegistrationComponent(accountTable, mode)

    when: 'the form is constructed and validated'
    component.@registrationInformationBinder.isValid()

    then: 'the account table is never consulted from the form'
    0 * accountTable.isEmailAvailable(_)
    0 * accountTable._

    where:
    mode << [Mode.LOCAL, Mode.ORCID]
  }

  @Unroll
  def "a registered address is not called out in field validation (#mode)"() {
    given: 'a service that reports every address as taken'
    def taken = Stub(UserInformationService) {
      isEmailAvailable(_) >> false
      isUserNameAvailable(_) >> true
    }
    def component = new UserRegistrationComponent(taken, mode)

    when: 'the email field is populated and validated'
    component.@email.setValue('someone@example.org')
    def status = component.@registrationInformationBinder.validate()

    then: 'no message mentions the address being in use'
    status.getFieldValidationErrors().every { error ->
      !error.getMessage().get().toLowerCase().contains('already in use')
    }

    where:
    mode << [Mode.LOCAL, Mode.ORCID]
  }

  def "the password message states the minimum that is actually enforced"() {
    given:
    def component = new UserRegistrationComponent(userInformationService, Mode.LOCAL)

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

  def "the ORCID form does not ask for a password at all"() {
    given: 'the mode that was authenticated by ORCID, so the visitor already proved who they are'
    def component = new UserRegistrationComponent(userInformationService, Mode.ORCID)

    expect: 'no password field exists to be filled in the first place'
    component.children.toList().every { !(it instanceof PasswordField) }

    and: 'a submitted registration carries no password, so nothing can store one by accident'
    component.currentInput().password() == ''

    and: 'the reset route is not offered, because there is no password to reset'
    !component.children.toList().findAll { it instanceof Span }.any {
      it.text.toLowerCase().contains('forgot your password')
    }
  }

  def "both modes submit the same value type, so the submission has one shape"() {
    given:
    def local = new UserRegistrationComponent(userInformationService, Mode.LOCAL)
    def orcid = new UserRegistrationComponent(userInformationService, Mode.ORCID)

    when: 'the same visitor details are entered into both'
    [local, orcid].each { form ->
      form.setFullName('Ada Lovelace')
      form.setEmail('ada@example.org')
      form.setUsername('alovelace')
    }
    local.@password.setValue('a-strong-passphrase')

    then: 'the email, name and user name are identical, and only the password differs'
    local.currentInput().fullName() == orcid.currentInput().fullName()
    local.currentInput().email() == orcid.currentInput().email()
    local.currentInput().userName() == orcid.currentInput().userName()
    local.currentInput().password() == 'a-strong-passphrase'
    orcid.currentInput().password() == ''
  }

  @Unroll
  def "the user name field is bounded by the shared maximum (#mode)"() {
    given:
    def component = new UserRegistrationComponent(
        Stub(UserInformationService) { isUserNameAvailable(_) >> true }, mode)

    expect: 'the input is truncated at the constant, in both modes, from the same source'
    component.@username.getMaxLength() == UserNames.MAX_LENGTH
    component.@username.getHelperText().contains(UserNames.MAX_LENGTH.toString())

    where:
    mode << [Mode.LOCAL, Mode.ORCID]
  }
}
