package life.qbic.datamanager.views.settings

import life.qbic.identity.api.UserInfo
import spock.lang.Specification

/**
 * Unit tests for the {@link AccountOverviewHeader} identity block.
 *
 * <p>The header renders plain DOM (no Vaadin service is required), so the structure can be
 * asserted directly. The full name and the user name are separate items on one wrapping line, so
 * the user name moves as a whole if it ever does not fit instead of being split mid-token, and it
 * is always rendered in full - the earlier design truncated it with an ellipsis, which silently
 * hid part of a user's own handle.</p>
 */
class AccountOverviewHeaderSpec extends Specification {

  private static UserInfo user(String fullName, String userName) {
    return new UserInfo("4a6b1c2d-0000-0000-0000-000000000000", fullName, "user@example.com",
        userName, true, null, null)
  }

  def "the user name is rendered verbatim on the same line as the full name"() {
    given:
    def header = new AccountOverviewHeader(user("Sven Admin Istrator", "sven1102dasdsadsadsadsa"))

    when: "the name line is inspected"
    def nameLine = header.nameLineForTest()

    then: "the line holds exactly the full name and the user name"
    nameLine.getClassNames().contains("settings-account-overview__name-line")
    nameLine.getChildren().count() == 2

    and: "the user name is rendered in parentheses, directly after the name"
    def userName = nameLine.getChildren()
        .filter { it.getText() == "(sven1102dasdsadsadsadsa)" }
        .findFirst().orElseThrow()
    userName.getText() == "(sven1102dasdsadsadsadsa)"
  }

  def "a user name at the length cap is rendered in full and never truncated"() {
    given: "a user name at the 20-character cap"
    def userName = "a" * 20
    def header = new AccountOverviewHeader(user("Sven", userName))

    when:
    def userSpan = header.nameLineForTest().getChildren()
        .filter { it.getText() == "(" + userName + ")" }
        .findFirst().orElseThrow()

    then: "the whole user name is present in the DOM — no ellipsis, no clipping"
    userSpan.getText() == "(" + userName + ")"
    userSpan.getText().length() == 22  // 20 characters plus the two parentheses
  }

  def "the identity block exposes the avatar and the account hint"() {
    given:
    def header = new AccountOverviewHeader(user("Sven Admin Istrator", "sven1103"))

    expect: "the block is announced as one labelled group"
    header.getElement().getAttribute("role") == "group"
    header.getElement().getAttribute("aria-label") == "Account"

    and: "the avatar and hint are present"
    header.avatarForTest() != null
    header.getChildren()
        .flatMap { it.getChildren() }
        .any { it.getClassNames().contains("settings-account-overview__hint") }
  }
}
