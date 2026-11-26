package life.qbic.datamanager.views.account

import spock.lang.Specification

/**
 * <b>The enrolment dialog must put the steps in the order the user has to do them</b>
 *
 * <p>Reported from a live build: the dialog led with the QR code, then the setup key, then a
 * full-width, blue-labelled <em>"Name this app (optional)"</em> field, and only then
 * <em>"Code from the app"</em>. Read top to bottom, the page asked for a nickname before it asked for
 * the code — the one thing that actually has to happen for enrolment to succeed.</p>
 *
 * <p>The visual weight was inverted three ways at once:</p>
 * <ol>
 *   <li><b>Order.</b> The name sat above the code, so the eye reached the optional step first.</li>
 *   <li><b>Colour.</b> A label rendered in the primary colour reads as a call to action. "Optional"
 *       in accent blue is a contradiction.</li>
 *   <li><b>Width.</b> Three full-width inputs (key, name, code) stacked vertically look equally
 *       important; the code is the one that gates the submit button.</li>
 * </ol>
 *
 * <p>So the hierarchy this pins is: <b>scan → (or type the key) → enter the code</b>, with the name
 * demoted below the code as a quiet secondary row. Nothing here changes behaviour — the name is still
 * optional, still prefills the default, and clearing it still means "use the default".</p>
 */
class EnrolmentDialogHierarchySpec extends Specification {

  // ---------------------------------------------------------------- the step order

  def "the verification code comes before the optional name"() {
    given:
    def steps = TotpEnrolmentDialog.stepOrder()

    expect: "the two things the user must do lead; the optional one follows"
    steps.indexOf("code") < steps.indexOf("name")
  }

  def "the QR code leads, then the setup key, then the code"() {
    given:
    def steps = TotpEnrolmentDialog.stepOrder()

    expect: "the order a user actually performs the task in"
    steps == ["qr", "setup-key", "code", "name"]
  }

  def "the optional step is last"() {
    expect:
    TotpEnrolmentDialog.stepOrder().last() == "name"
  }

  // ---------------------------------------------------------------- the name is demoted

  def "the name label does not read as a required step"() {
    expect: "the word 'optional' stays, and the label no longer shouts"
    TotpEnrolmentDialog.NAME_LABEL.toLowerCase().contains("optional")
  }

  def "the name is a quiet secondary control, not a full-width field"() {
    expect: "it is marked so the stylesheet can render it smaller and narrower"
    TotpEnrolmentDialog.NAME_CSS_CLASS == "mfa-enrol-name"
  }

  def "the primary action is still the code, and the name never gates it"() {
    expect: "the submit button names the verification, not the naming"
    TotpEnrolmentDialog.CONFIRM_LABEL == "Verify and enable"
  }

  // ---------------------------------------------------------------- what must not regress

  def "an empty name still means the default rather than an error"() {
    expect: "the domain refuses a blank label, so the dialog must never submit one"
    TwoFactorSettingsCopy.factorName("") == TotpEnrolmentDialog.DEFAULT_DISPLAY_NAME
    TwoFactorSettingsCopy.factorName("   ") == TotpEnrolmentDialog.DEFAULT_DISPLAY_NAME
  }

  def "a typed name is still honoured, trimmed"() {
    expect:
    TwoFactorSettingsCopy.factorName("  iPhone 13 Pro  ") == "iPhone 13 Pro"
  }
}
