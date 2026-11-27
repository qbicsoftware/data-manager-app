package life.qbic.datamanager.views.account

import spock.lang.Specification
import spock.lang.Unroll

/**
 * <b>Adding a second authenticator must not destroy the recovery codes already saved (regression)</b>
 *
 * <p>Reported from testing: "when users add a new authenticator, they also get prompted with new
 * recovery keys. This does not make any sense." It does not — and it is worse than a pointless
 * prompt.</p>
 *
 * <h3>What the prompt actually does</h3>
 * <p>{@code RecoveryCodeService.generate} retires every existing code in the same transaction
 * ({@code invalidateAllFor} → <code>retired</code> count in the log line). So a user who adds a second
 * app as redundancy — the intended reason to add one — is shown a fresh sheet that silently invalidates
 * the sheet they printed. If they dismiss it, believing their old codes still work, they hold ten codes
 * that are all dead, and the next lost phone is an identity check at the helpdesk.</p>
 *
 * <p>That is the exact failure the codes exist to prevent, caused by the action meant to make the
 * account safer.</p>
 *
 * <h3>The rule, and where it already lived</h3>
 * <p>A new set is issued only when the account has <b>none left</b>, or when the user explicitly asks.
 * The original settings flow had precisely this guard —</p>
 * <pre>
 * if (recoveryCodeService.remaining(userId) &gt; 0) { return; }
 * </pre>
 * <p>— and it was dropped when enrolment moved from a dialog to a route. These cases pin the rule so
 * it survives the next refactor.</p>
 */
class RecoveryCodesIssuedOnEnrolmentSpec extends Specification {

  // ---------------------------------------------------------------- the decision

  @Unroll
  def "with #remaining unused codes left, issuing a sheet is #expected"() {
    expect:
    TwoFactorSettingsCopy.recoverySheetNeededOnEnrolment(remaining) == expected

    where:
    remaining | expected
    0         | true      // first enrolment, or every code has been used
    1         | false     // one is still a way back in
    5         | false
    10        | false     // a full untouched set
  }

  def "a negative count is treated as none, not as 'codes exist'"() {
    expect: "a defensive read must not invert the safety property"
    TwoFactorSettingsCopy.recoverySheetNeededOnEnrolment(-1)
  }

  // ---------------------------------------------------------------- the copy that explains it

  def "when no sheet is issued, the user is told their existing codes still work"() {
    given:
    def body = TwoFactorSettingsCopy.enrolmentDoneWithoutNewCodesBody(true)

    expect: "the absence of a sheet must not read as 'you have no codes'"
    body.toLowerCase().contains("still work")
  }

  def "the confirmation does not keep telling the user to set up an app they just set up"() {
    expect: "a confirmation is not an instruction; reusing the enrolment title read as unfinished work"
    TwoFactorSettingsCopy.ENROL_DONE_TITLE != TwoFactorSettingsCopy.ENROL_TITLE
    !TwoFactorSettingsCopy.ENROL_DONE_TITLE.toLowerCase().contains("set up")
  }

  def "the confirmation says the app is ready"() {
    expect:
    def title = TwoFactorSettingsCopy.ENROL_DONE_TITLE.toLowerCase()
    title.contains("ready") || title.contains("enabled") || title.contains("added")
  }

  def "the confirmation offers a way back that is not a full-width primary bar"() {
    expect: "a primary button spanning the panel reads as the next required step, and there is none"
    TwoFactorSettingsCopy.ENROL_DONE_ACTION == "Back to two-factor settings"
  }

  def "the message names where the codes are managed"() {
    given:
    def body = TwoFactorSettingsCopy.enrolmentDoneWithoutNewCodesBody(true)

    expect: "and points at the explicit action for a deliberate replacement"
    body.toLowerCase().contains("recovery codes")
  }

  def "an account with no codes is never told its codes still work"() {
    expect: "there is no sentence that would be a lie in that state"
    TwoFactorSettingsCopy.enrolmentDoneWithoutNewCodesBody(false)
            .toLowerCase()
            .contains("no unused recovery codes")
  }
}
