package life.qbic.datamanager.views.account

import com.vaadin.flow.router.Route
import jakarta.annotation.security.PermitAll
import life.qbic.datamanager.views.settings.SettingsMainLayout
import spock.lang.Specification

/**
 * <b>Enrolment is a page, not a modal (reported: "the modal dialog again, and it gets worse")</b>
 *
 * <p>The enrolment dialog grew until its footer fell below the fold at 1440x900, and no amount of
 * re-tuning fixed the class of problem: a QR code, a setup key and two inputs are more content than a
 * fixed-height modal body should carry. Reported twice — first the hierarchy (the optional name led),
 * then the cutoff (Confirm and Cancel unreachable).</p>
 *
 * <h3>Why a route rather than a smaller dialog</h3>
 * <p>It is what this codebase already does for multi-step creation: {@code NewGroupMain} and
 * {@code AdminGroupCreationMain} are routes under {@code GroupsMainLayout}, the password-reset flow is
 * a route, and — decisively — <b>mandatory enrolment is already a route</b>
 * ({@code RequiredEnrolmentView}). The settings enrolment was the only multi-step flow in a modal, so
 * it was both inconsistent and the one place that hits the height limit.</p>
 *
 * <p>A page scrolls. That also retires MFA-93 for this surface as a side effect, rather than
 * re-tuning a modal until it just fits at one particular viewport.</p>
 */
class EnrolmentRouteSpec extends Specification {

  // ---------------------------------------------------------------- the route contract

  def "enrolment is reachable as its own settings route"() {
    given:
    def route = TwoFactorEnrolmentMain.getAnnotation(Route)

    expect:
    route != null
    route.value() == "settings/two-factor/enrol"
  }

  def "it renders inside the settings shell, so the section navigation stays visible"() {
    expect: "a user mid-flow can still see where they are and leave"
    TwoFactorEnrolmentMain.getAnnotation(Route).layout() == SettingsMainLayout
  }

  def "it is permitted for signed-in users, like every other settings section"() {
    expect:
    TwoFactorEnrolmentMain.getAnnotation(PermitAll) != null
  }

  // ---------------------------------------------------------------- the step order survives the move

  def "the page keeps the task order: scan, setup key, code, name"() {
    expect: "the hierarchy fix must not be lost in the conversion"
    TwoFactorEnrolmentMain.stepOrder() == ["qr", "setup-key", "code", "name"]
  }

  def "the optional name is still last and still marked as optional"() {
    expect:
    TwoFactorEnrolmentMain.stepOrder().last() == "name"
    TwoFactorEnrolmentMain.NAME_LABEL.toLowerCase().contains("optional")
  }

  def "the primary action still names the verification"() {
    expect:
    TwoFactorEnrolmentMain.CONFIRM_LABEL == "Verify and enable"
  }

  // ---------------------------------------------------------------- the recovery sheet still reports

  def "the recovery-code acknowledgement is tracked for both entry points"() {
    given: "enrolment can now be started from two routes: the settings page and this one"
    def attribute = RecoveryCodeAcknowledgement.UNACKNOWLEDGED_ATTRIBUTE

    expect: "one shared marker, so a refresh loses the sheet loudly wherever it was opened"
    attribute != null
    attribute.contains("unacknowledged")
  }
}
