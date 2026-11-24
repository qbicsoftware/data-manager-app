package life.qbic.datamanager.views.register

import life.qbic.datamanager.views.login.passwordreset.ResetPasswordMain
import life.qbic.datamanager.views.notifications.ErrorMessage
import life.qbic.identity.api.UserInformationService
import life.qbic.identity.api.UserNames
import life.qbic.identity.application.user.IdentityService.EmptyUserNameException
import life.qbic.identity.application.user.IdentityService.UserExistsException
import life.qbic.identity.application.user.IdentityService.UserNameNotAvailableException
import life.qbic.identity.application.user.IdentityService.UserNameTooLongException
import com.vaadin.flow.component.Component
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.Router
import com.vaadin.flow.server.RouteRegistry
import com.vaadin.flow.server.VaadinService
import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>One registration failure, one piece of text — and never the one that discloses account existence.</b>
 *
 * <p>{@code UserRegistrationMain} and {@code RegistrationOrcIdMain} carried identical copies of the same
 * {@code handleRegistrationFailure} switch. Two copies of a user-facing message is a drift bug waiting to
 * happen, and it had already happened in this flow: the "email already in use" case was removed from both
 * by hand when {@code AUTH-NFR-03} forbade it. One surviving copy would have been enough to restore the
 * oracle.</p>
 *
 * <p>So the mapping now lives in one place ({@link RegistrationFailureMessage}) and this spec pins the
 * properties that matter: the three user-name cases render the wording the visitor already knows, anything
 * else is a generic failure, and no input produces an account-existence statement.</p>
 */
class RegistrationFailureMessageSpec extends Specification {

  @Unroll
  def "a #reason failure renders its own wording"() {
    when:
    def message = RegistrationFailureMessage.of(failure)

    then:
    message.title() == title
    message.description() == description

    where:
    reason               | failure                             | title                       | description
    'taken user name'    | new UserNameNotAvailableException()  | 'Username already in use'   | 'Please try another username'
    'empty user name'    | new EmptyUserNameException()         | 'Username must not be empty'| 'Please try another username'
    'over-long user name'| new UserNameTooLongException()       | 'Username is too long'      | 'User name must not exceed ' + UserNames.MAX_LENGTH + ' characters'
    'anything else'      | new IllegalStateException('boom')    | 'Registration failed'       | 'Please try again.'
  }

  def "a duplicate account has no wording here at all"() {
    when: 'the failure that used to render "email already in use" is mapped'
    def message = RegistrationFailureMessage.of(new UserExistsException())

    then: 'it falls through to the generic case, so the oracle cannot be rendered from this class'
    message.title() == 'Registration failed'

    and: 'no rendering of it mentions the address, the account, or being "in use"'
    !message.description().toLowerCase().contains('already')
    !message.description().toLowerCase().contains('in use')
    !message.description().toLowerCase().contains('exist')
  }

  def "the failure is rendered on the merged form, whatever mode produced it"() {
    given: 'a service context, because the local form builds a RouterLink to the reset route'
    def registry = Stub(RouteRegistry)
    registry.getTargetUrl(ResetPasswordMain, _ as RouteParameters) >> Optional.of('reset-password')
    def service = Stub(VaadinService)
    service.getRouter() >> new Router(registry)
    service.getContext() >> null
    VaadinService.setCurrent(service)

    and: 'the shared form'
    def form = new UserRegistrationComponent(Stub(UserInformationService) {
      isUserNameAvailable(_) >> true
    }, UserRegistrationComponent.Mode.LOCAL)

    when: 'a failure is rendered through the shared mapping'
    RegistrationFailureMessage.render(form, new UserNameNotAvailableException())

    then: 'the form shows the title and description, in the notification area it already had'
    def rendered = descendants(form).findAll { it instanceof ErrorMessage }
    rendered.size() == 1
    rendered.first().title() == 'Username already in use'
    rendered.first().message() == 'Please try another username'

    cleanup:
    VaadinService.setCurrent(null)
  }

  /** Flattens the component tree, because the message is added to a nested notification area. */
  private static List descendants(Component component) {
    return component.children.toList().collectMany { [it] + descendants(it) }
  }
}
