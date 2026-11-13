package life.qbic.datamanager.views.register

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.Router
import com.vaadin.flow.server.RouteRegistry
import com.vaadin.flow.server.VaadinService
import life.qbic.datamanager.views.login.passwordreset.ResetPasswordMain
import life.qbic.datamanager.views.login.passwordreset.SetNewPasswordComponent
import life.qbic.datamanager.views.register.UserRegistrationComponent.Mode
import life.qbic.identity.api.Passwords
import life.qbic.identity.api.UserInformationService
import life.qbic.identity.api.UserNames
import life.qbic.identity.application.throttle.AuthAttemptThrottle
import life.qbic.identity.application.throttle.AuthAttemptThrottle.Scope
import life.qbic.identity.domain.repository.AuthAttemptCounter
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Duration
import java.time.Instant

/**
 * <b>The guards from #1693 must keep applying to the merged registration form (FEAT-AUTH-FLOWS-04).</b>
 *
 * <p>Merging {@code UserRegistrationComponent} and {@code UserRegistrationOrcIdComponent} moved code that
 * two security fixes had just been applied to. Two things had to survive the move, and neither is visible
 * from a behavioural test of "registration works":</p>
 *
 * <ol>
 *   <li><b>The account-existence oracle stays gone.</b> The email validator used to call
 *       {@code isEmailAvailable} in <em>both</em> forms, so the merge was an opportunity to reintroduce it
 *       in the one place that now serves both flows. {@link UserRegistrationAccountOracleSpec} pins the
 *       behaviour; this spec pins the structure that makes the behaviour reach both modes.</li>
 *   <li><b>The throttle is still consulted before registration work.</b> {@code RegistrationSubmission}
 *       records an attempt against {@link Scope#REGISTRATION} before the identity port is called. A merge
 *       that moved the call into the form, or that built its own submission, would bypass it — silently,
 *       because registration would go on working.</li>
 * </ol>
 *
 * <p>The remaining cases are the ones the merge makes possible for the first time: the shared form is not
 * a {@code @SpringComponent} (an extra unused instance would be created per UI and a mode-dependent
 * construction could not be expressed), and the ORCID mode must never collect a password — storing a
 * factor the visitor did not choose is what {@code AUTH-R-07} exists to fix, not to introduce.</p>
 */
class MergedRegistrationFormSpec extends Specification {

  private static final List<Mode> MODES = [Mode.LOCAL, Mode.ORCID]

  UserInformationService userInformationService = Stub()

  def setup() {
    // The LOCAL mode builds a RouterLink to the reset form, whose href is resolved through the router at
    // construction time. Same scaffolding as UserRegistrationAccountOracleSpec.
    def registry = Stub(RouteRegistry)
    registry.getTargetUrl(ResetPasswordMain, _ as RouteParameters) >> Optional.of('reset-password')
    def service = Stub(VaadinService)
    service.getRouter() >> new Router(registry)
    service.getContext() >> null
    VaadinService.setCurrent(service)
    userInformationService = Stub(UserInformationService) {
      isUserNameAvailable(_) >> true
    }
  }

  def cleanup() {
    VaadinService.setCurrent(null)
  }

  // ---- the merged form is one component, reached twice ------------------------------------------

  @Unroll
  def "#viewClass builds the shared form in #mode mode"() {
    given: 'the view, constructed the way the application constructs it'
    def view = viewClass.declaredConstructors.find { !it.synthetic }
        .newInstance(userInformationService, new RegistrationSubmissionSpec.FakeIdentity(),
            allowingThrottle())

    when:
    def forms = view.children.toList().findAll { it instanceof UserRegistrationComponent }

    then: 'exactly one registration form is on the page, and it is the shared one'
    forms.size() == 1

    and: 'it renders the mode that belongs to this route'
    forms.first().@mode == mode

    where:
    viewClass             | mode
    UserRegistrationMain  | Mode.LOCAL
    RegistrationOrcIdMain | Mode.ORCID
  }

  def "the ORCID form class is gone, so the two validator chains cannot drift apart again"() {
    expect: 'no bytecode for a second registration form survives in the build output'
    UserRegistrationComponent.classLoader.getResource(
        'life/qbic/datamanager/views/register/UserRegistrationOrcIdComponent.class') == null
  }

  @Unroll
  def "the #mode form does not ask the account table whether an address is free"() {
    given: 'an account table that would answer, were it asked'
    def accountTable = Mock(UserInformationService)
    def form = new UserRegistrationComponent(accountTable, mode)
    accountTable.isUserNameAvailable(_) >> true

    when: 'the form is filled in the way a visitor would, and validated'
    form.setFullName('Ada Lovelace')
    form.setEmail('someone@example.org')
    form.setUsername('alovelace')
    if (mode.collectsPassword()) {
      form.@password.setValue('a-strong-passphrase')
    }
    def hasErrors = form.@registrationInformationBinder.validate().hasErrors()

    then: 'the address is never the subject of a question to the account table'
    0 * accountTable.isEmailAvailable(_)

    and: 'the rest of the validation is unaffected by the mode'
    !hasErrors

    where:
    mode << MODES
  }

  // ---- the throttle still runs first -------------------------------------------------------------

  @Unroll
  def "the #mode flow records the attempt before it registers"() {
    given: 'a submission over a counter that records the scope it is asked about'
    def counter = new RecordingCounter()
    def identity = new RegistrationSubmissionSpec.FakeIdentity()
    def submission = new RegistrationSubmission(identity, userInformationService,
        throttleOver(counter))

    and: 'what the shared form would submit in this mode'
    def form = new UserRegistrationComponent(userInformationService, mode)
    form.setFullName('Ada Lovelace')
    form.setEmail('new@example.org')
    form.setUsername('alovelace')
    if (mode.collectsPassword()) {
      form.@password.setValue('a-strong-passphrase')
    }
    def submitted = RegistrationSubmission.RegistrationForm.of(form.currentInput())

    when:
    if (mode == Mode.LOCAL) {
      submission.submitLocal(submitted, { Optional<String> ignored -> })
    } else {
      submission.submitOidc(submitted, 'issuer', 'subject', false,
          { Optional<String> ignored -> })
    }

    then: 'the registration scope was recorded, and nothing else was'
    counter.scopes == [Scope.REGISTRATION.name()]

    and: 'only then was the identity port used, exactly once'
    identity.localRegistrations + identity.openIdRegistrations == ['new@example.org']

    where:
    mode << MODES
  }

  def "the ORCID mode submits no password, so no factor is stored that the visitor did not choose"() {
    given: 'the ORCID form, prefilled the way RegistrationOrcIdMain prefills it'
    def form = new UserRegistrationComponent(userInformationService, Mode.ORCID)
    form.setFullName('Ada Lovelace')
    form.setEmail('ada@example.org')
    form.setUsername('alovelace')

    expect: 'the form offers no password field'
    form.children.toList().every { !(it instanceof PasswordField) }

    and: 'what it would submit carries an empty password (AUTH-R-07 adds one via invitation)'
    form.currentInput().password().isEmpty()
  }

  def "the ORCID form does not offer to reset a password that does not exist yet"() {
    given: 'both modes, so the difference is attributable to the mode and not to the merge'
    def orcid = new UserRegistrationComponent(userInformationService, Mode.ORCID)
    def local = new UserRegistrationComponent(userInformationService, Mode.LOCAL)

    expect: 'the local form offers the reset route, because that visitor chose a password'
    local.children.toList().findAll { it instanceof Span }.any {
      it.text.toLowerCase().contains('forgot your password')
    }

    and: 'the ORCID form does not, because a reset request for a passwordless account cannot help'
    !orcid.children.toList().findAll { it instanceof Span }.any {
      it.text.toLowerCase().contains('forgot your password')
    }
  }

  // ---- one password rule, one message ------------------------------------------------------------

  def "the merged form states the password rule from the shared constant"() {
    given:
    def form = new UserRegistrationComponent(userInformationService, Mode.LOCAL)

    expect: 'the hint is the shared one, so the number cannot go stale in this form alone'
    form.@password.getHelperText() == Passwords.hint()

    and: 'the reset form renders the same hint from the same source'
    new SetNewPasswordComponent().children.toList()
        .findAll { it instanceof PasswordField }
        .every { it.getHelperText() == Passwords.hint() }
  }

  def "the user name limit comes from one constant in both modes"() {
    given:
    def local = new UserRegistrationComponent(userInformationService, Mode.LOCAL)
    def orcid = new UserRegistrationComponent(userInformationService, Mode.ORCID)

    expect: 'the input is constrained to the shared maximum, and the helper text names it once'
    local.@username.getMaxLength() == UserNames.MAX_LENGTH
    orcid.@username.getMaxLength() == UserNames.MAX_LENGTH
    local.@username.getHelperText() == orcid.@username.getHelperText()
  }

  // ---- helpers -----------------------------------------------------------------------------------

  private static AuthAttemptThrottle allowingThrottle() {
    return throttleOver(new RegistrationSubmissionSpec.AllowingCounter())
  }

  private static AuthAttemptThrottle throttleOver(AuthAttemptCounter counter) {
    return new AuthAttemptThrottle(counter,
        10, Duration.ofMinutes(15), Duration.ofMinutes(15),
        3, Duration.ofMinutes(15), Duration.ofMinutes(15))
  }

  /** Records the scope of every increment, so scope and ordering can be asserted. */
  static class RecordingCounter implements AuthAttemptCounter {
    List<String> scopes = []

    @Override
    Counter increment(String scope, String key, Instant now, Duration window) {
      scopes << scope
      return new Counter(1, null)
    }

    @Override
    Counter peek(String scope, String key) { return new Counter(0, null) }

    @Override
    void block(String scope, String key, Instant blockedUntil) { }

    @Override
    void clear(String scope, String key) { }
  }
}
