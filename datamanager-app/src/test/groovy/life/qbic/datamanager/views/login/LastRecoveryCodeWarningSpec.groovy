package life.qbic.datamanager.views.login

import life.qbic.datamanager.views.account.TwoFactorSettingsCopy
import life.qbic.datamanager.views.account.MfaEnrolmentPrompt
import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>Regression: using the last recovery code must say so (MFA-41b)</b>
 *
 * <p>Recovery codes are the break-glass path (AUTH-R-03): the only way back in when the authenticator
 * is gone. A set is issued once, shown once, and each code works once — so when the last one is
 * redeemed, the account is one lost phone away from an identity check at the helpdesk, and nothing the
 * user can see tells them that.</p>
 *
 * <p>The QA run reported exactly that gap: "After log-in with the last valid recovery code, I expect a
 * warning that they all are consumed." Recorded as FAILURE (MFA-41b).</p>
 *
 * <h3>Where the notice has to live, and why the obvious place does not work</h3>
 * <p>The challenge page (<code>SecondFactorView</code>) cannot show it. Redeeming a code establishes the
 * session and redirects, so any sentence written into that page's components is discarded one
 * instruction later — a mistake found while writing this spec, not in review.</p>
 *
 * <p>So the count travels in the session and is rendered by {@link MfaEnrolmentPrompt}, which is
 * mounted in the application shell: it survives the redirect and appears at the top of the page the
 * user lands on. It is shown once (the attribute is consumed) because a notice about an event must not
 * become a permanent banner.</p>
 *
 * <h3>What the sentence must say, and must not</h3>
 * <ul>
 *   <li>that this was the last code, so the set is empty;</li>
 *   <li>that the way forward is a new authenticator, which issues a fresh set;</li>
 *   <li>nothing about e-mail, because mailbox control must never read as the way past a factor
 *       (ADR 0011 B1) — and a user in this state is the one most likely to look for that shortcut.</li>
 * </ul>
 */
class LastRecoveryCodeWarningSpec extends Specification {

  // ---------------------------------------------------------------- the empty set

  def "the empty-set heading names the loss"() {
    expect:
    TwoFactorSettingsCopy.recoveryCodesLeftTitle(0).toLowerCase().contains("no recovery codes")
  }

  def "the empty-set body names the consequence and the way forward"() {
    given:
    def body = TwoFactorSettingsCopy.recoveryCodesLeftBody(0).toLowerCase()

    expect: "what just happened"
    body.contains("last recovery code")

    and: "what is now missing"
    body.contains("none are left")

    and: "the action that restores the safety net"
    body.contains("set up a new authenticator app")
    body.contains("fresh set")
  }

  def "the empty-set body does not offer an e-mail reset"() {
    given:
    def body = TwoFactorSettingsCopy.recoveryCodesLeftBody(0).toLowerCase()

    expect:
    !body.contains("e-mail")
    !body.contains("email")
    !body.contains("support will")
  }

  // ---------------------------------------------------------------- codes still left

  @Unroll
  def "with #left left the notice states the count rather than warning about running out"() {
    given:
    def body = TwoFactorSettingsCopy.recoveryCodesLeftBody(left)

    expect: "the count is stated"
    body.contains(String.valueOf(left))

    and: "a plural count is plural, a single one is not"
    body.contains(expected)

    and: "it is not the empty-set warning"
    body != TwoFactorSettingsCopy.recoveryCodesLeftBody(0)

    where:
    left | expected
    4    | "4 codes are still unused"
    2    | "2 codes are still unused"
    1    | "1 code is still unused"
  }

  def "a single remaining code reads as singular, not '1 codes'"() {
    expect:
    !TwoFactorSettingsCopy.recoveryCodesLeftBody(1).contains("1 codes")
  }

  def "a spent set and a nearly-spent set are different messages"() {
    expect:
    TwoFactorSettingsCopy.recoveryCodesLeftTitle(0)
        != TwoFactorSettingsCopy.recoveryCodesLeftTitle(1)
  }

  // ---------------------------------------------------------------- the transport

  def "the challenge hands the count over in the session, because the redirect discards page text"() {
    expect: "the attribute the challenge writes is the one the prompt reads"
    MfaEnrolmentPrompt.RECOVERY_CODES_LEFT_ATTRIBUTE != null
    MfaEnrolmentPrompt.RECOVERY_CODES_LEFT_ATTRIBUTE.contains("recovery-codes-left")
  }
}
