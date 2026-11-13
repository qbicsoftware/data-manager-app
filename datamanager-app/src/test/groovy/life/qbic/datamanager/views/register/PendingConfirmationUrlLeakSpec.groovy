package life.qbic.datamanager.views.register

import com.vaadin.flow.router.HasUrlParameter
import com.vaadin.flow.router.Route
import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>The pending-confirmation URL must not carry the user id (AUTH-NFR-03).</b>
 *
 * <p>After registration the browser used to land on
 * {@code /register/pending-email-confirmation/<user-uuid>}. That put a persistent identifier into browser
 * history, server access logs and {@code Referer} headers — and it is the same identifier the old
 * confirmation links used as the credential itself, before the R1 slice replaced them with single-use
 * tokens.</p>
 *
 * <p>It also reopened the spam vector this change had just closed through email: anyone holding that URL
 * could press "Resend verification link" and mail the account owner, repeatedly.</p>
 *
 * <p>The id now travels in the session, scoped to the visitor who just registered. These specs assert the
 * structural facts, because a behavioural spec cannot see a URL that no longer exists.</p>
 */
class PendingConfirmationUrlLeakSpec extends Specification {

  @Unroll
  def "#view navigates to the pending-confirmation route without an id"() {
    given: 'every constructor of the registration view'
    def constructors = view.declaredConstructors.findAll { !it.synthetic }

    and: 'the compiled bytecode of the view'
    def bytes = bytecodeOf(view)

    expect: 'the route class accepts no URL parameter, so navigating with an id does not compile'
    !HasUrlParameter.isAssignableFrom(EmailConfirmationMain)

    and: 'the view no longer calls the two-argument navigate overload'
    !bytes.contains('pending-email-confirmation/')

    and: 'it hands the id to the session instead'
    bytes.contains('PENDING_REGISTRATION_USER_ID')

    where:
    view << [UserRegistrationMain, RegistrationOrcIdMain]
  }

  def "the page still declares exactly one route, so no id-bearing variant exists"() {
    given:
    def route = EmailConfirmationMain.getAnnotation(Route)

    expect:
    route != null
    route.value() == 'register/pending-email-confirmation'

    and: 'no route template or parameter placeholder'
    !route.value().contains('{')
    !route.value().contains(':')
  }

  def "the id is stored under a namespaced session key, not a global string"() {
    expect: 'a bare key would collide with anything else using the session'
    EmailConfirmationMain.PENDING_REGISTRATION_USER_ID.contains(EmailConfirmationMain.name)
  }

  private static String bytecodeOf(Class type) {
    def resource = type.name.replace('.', '/') + '.class'
    return type.classLoader.getResourceAsStream(resource)
        .withCloseable { new String(it.bytes, 'ISO-8859-1') }
  }
}
