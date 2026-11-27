package life.qbic.datamanager.views.account

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasText
import spock.lang.Specification

/**
 * <b>Regression: a one-time sheet that is never acknowledged must be reported as lost (MFA-91)</b>
 *
 * <p>The rest of the {@link RecoveryCodesDialog} design is right, and this spec exists because it is:
 * the plaintext codes exist only on that sheet, the only exit is "I have saved my recovery codes", and
 * nothing is persisted. What the QA run found is the gap that leaves: <b>a browser refresh closes the
 * dialog without the acknowledgement, and the codes are gone with no trace.</b> The user is left
 * believing they can look them up later, and the next thing they learn is that a lost phone means an
 * identity check at the helpdesk.</p>
 *
 * <p>The application cannot show the codes again — it only stores hashes (AUTH-R-03) — and it must not
 * quietly pretend the sheet was handled. So the two properties pinned here are:</p>
 *
 * <ol>
 *   <li><b>Unacknowledged is distinguishable from acknowledged.</b> A sheet closed without the
 *       acknowledgement is <em>not</em> a sheet saved, and the page must be able to tell.</li>
 *   <li><b>The consequence is named, with the one action that still exists.</b> The notice says the
 *       codes are gone and points at regeneration, which works only while a factor exists — which it
 *       does, because the sheet is issued right after enrolment.</li>
 * </ol>
 *
 * <p>This is deliberately a statement about the component's text and listeners, not a UI test: the
 * defect was a missing sentence, and a missing sentence is best pinned where it is written.</p>
 */
class RecoveryCodesUnacknowledgedSpec extends Specification {

  // ---------------------------------------------------------------- the missing sentence

  def "the notice names the loss, the consequence and the way out"() {
    given:
    def notice = RecoveryCodesDialog.unacknowledgedCodesNotice()

    expect: "it says the codes are gone and that they cannot be shown again"
    notice.toLowerCase().contains("not saved")
    notice.toLowerCase().contains("cannot be shown again")

    and: "it says what losing them costs, in the terms that matter to the user"
    notice.toLowerCase().contains("lose access to your authenticator app")
    notice.toLowerCase().contains("cannot")
    notice.toLowerCase().contains("sign in by yourself")

    and: "it names the action that still exists"
    notice.toLowerCase().contains("generate a new set")
  }

  def "the notice does not promise a way to view the codes again"() {
    given:
    def notice = RecoveryCodesDialog.unacknowledgedCodesNotice().toLowerCase()

    expect: "no phrasing may suggest the existing codes can be recovered or re-displayed"
    !notice.contains("view them again")
    !notice.contains("show them again")
    !notice.contains("recover the codes")
    !notice.contains("look them up")
  }

  def "the notice does not offer a reset by e-mail"() {
    given:
    def notice = RecoveryCodesDialog.unacknowledgedCodesNotice().toLowerCase()

    expect: "mailbox control must never read as the way past the factor (ADR 0011 B1)"
    !notice.contains("by e-mail")
    !notice.contains("by email")
    !notice.contains("email support")
  }

  // ---------------------------------------------------------------- the acknowledgement

  def "the unacknowledged notice is not part of the sheet the user is meant to keep"() {
    given: "the text that is copied and downloaded"
    def sheet = RecoveryCodesDialog.codesAsText(
        new life.qbic.identity.application.mfa.RecoveryCodeSet("user-1",
            ["ABCD-EFGH-IJKL".replace("-", "")], java.time.Instant.parse("2026-11-19T14:32:00Z")),
        "ada@example.org")

    expect: "a saved sheet carries no warning about a loss that did not happen"
    !sheet.contains(RecoveryCodesDialog.unacknowledgedCodesNotice())
  }
}
