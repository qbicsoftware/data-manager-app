package life.qbic.datamanager.views.login

import spock.lang.Specification

/**
 * <b>A rejection message must be seen, not scrolled past (MFA-94)</b>
 *
 * <p>Reported: the "That code is not valid…" message "is sometimes hidden behind a scrolling action".
 * The challenge card is taller than the viewport on a laptop, so a message rendered at the bottom of
 * the form can sit below the fold — the user presses Verify, nothing appears to happen, and they
 * conclude the button is broken.</p>
 *
 * <p><b>The fix is placement, not CSS.</b> The message belongs immediately under the field and action
 * it describes: that region is on screen whenever the user is pressing the button, because they just
 * scrolled to it to press the button. Anything lower competes with the support note and the footer
 * link for space that is not always there.</p>
 *
 * <p><b>Not a toast either.</b> A notification that disappears on a timer is wrong for a message the
 * user needs while they retype a code. This user's failure mode is that their authenticator clock has
 * drifted, which can take two attempts and a trip to the phone's settings.</p>
 */
class SecondFactorErrorPlacementSpec extends Specification {

  def "the rejection message is rendered directly under the code field"() {
    expect: "the code step groups field, action and message, in that order"
    SecondFactorView.errorFollowsTheCodeStep()
  }

  def "the error is inside the same block as the field, above the page's links"() {
    expect: "so it cannot fall below the fold while the user is looking at the button"
    SecondFactorView.errorIsInsideCodeSection()
  }

  def "the message is not a toast"() {
    expect: "a timer would hide a message the user needs while retyping"
    !SecondFactorView.errorIsTransient()
  }
}
