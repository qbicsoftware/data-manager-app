package life.qbic.datamanager.views.login

import spock.lang.Specification

/**
 * <b>The login failure must name the field the user actually fills in (MFA-94)</b>
 *
 * <p>Reported: "We get a 'wrong username or password' error when we log in with a correct email and
 * wrong password… even though one should/can not log in with username."</p>
 *
 * <p>That is exactly right, and it is not cosmetic. The only login identifier is the e-mail address —
 * {@code UserDetailsServiceImpl} looks the account up by {@code findByEmail} and nothing else. Telling
 * a user their <em>username</em> is wrong sends them looking for a field that does not exist, and it
 * implies a second login path that the application does not support.</p>
 *
 * <h3>What must not change</h3>
 * <p>The message has to stay one message for every failure — wrong password, unknown address,
 * unconfirmed account, throttled attempt. Distinguishing them is the enumeration oracle AUTH-NFR-03
 * forbids, and the throttle depends on this wording to make a refusal indistinguishable. So this is a
 * correction of <em>which field is named</em>, not a step toward being more specific.</p>
 */
class LoginErrorMessageSpec extends Specification {

  def "the failure names the email address, which is the only login identifier"() {
    expect:
    LoginLayout.INVALID_CREDENTIALS_TITLE.toLowerCase().contains("email")
  }

  def "it no longer claims a username is involved"() {
    expect: "there is no username field and no username lookup anywhere in the login path"
    !LoginLayout.INVALID_CREDENTIALS_TITLE.toLowerCase().contains("username")
  }

  def "it is still one message for every failure, not a more specific one"() {
    expect: "specificity per cause would rebuild the enumeration oracle (AUTH-NFR-03)"
    LoginLayout.INVALID_CREDENTIALS_TITLE == "Incorrect email address or password"
    LoginLayout.INVALID_CREDENTIALS_DESCRIPTION == "Please try again."
  }

  def "the description does not speculate about which of the two was wrong"() {
    expect:
    !LoginLayout.INVALID_CREDENTIALS_DESCRIPTION.toLowerCase().contains("not found")
    !LoginLayout.INVALID_CREDENTIALS_DESCRIPTION.toLowerCase().contains("unknown")
  }
}
