package life.qbic.datamanager.views.settings;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import life.qbic.datamanager.views.account.UserAvatar;
import life.qbic.identity.api.UserInfo;

/**
 * Account Overview Header
 * <p>
 * Displays the identity of the currently logged-in user at the top of the settings hub: the
 * user's avatar, full name, platform user name and a short hint that the shown settings belong to
 * the user's account. It heads the whole hub in its own full-width row, above the navigation and
 * the routed section, mirroring GitHub's settings hub - the identity is what the settings below
 * act on. The full shell width means a long name and a user name of up to 20 characters fit beside
 * the avatar without wrapping or truncation.
 */
public class AccountOverviewHeader extends Div {

  public AccountOverviewHeader(UserInfo userInfo) {
    requireNonNull(userInfo, "userInfo must not be null");
    addClassName("settings-account-overview");
    // One labelled unit for assistive technology, so the identity and its hint are announced
    // together and the non-heading "Your account settings" line stays out of the heading outline.
    getElement().setAttribute("role", "group");
    getElement().setAttribute("aria-label", "Account");

    UserAvatar userAvatar = new UserAvatar();
    userAvatar.setUserId(userInfo.id());
    userAvatar.setName(userInfo.platformUserName());
    userAvatar.addClassName("settings-account-overview__avatar");

    Span fullName = new Span(userInfo.fullName());
    fullName.addClassName("font-bold");
    Span userName = new Span("(" + userInfo.platformUserName() + ")");
    userName.addClassNames("text-s", "text-secondary");
    // Full name and user name are separate items on one wrapping line: the identity row spans the
    // full shell width, so both fit side by side, and the user name moves as a whole onto the next
    // line (at the gap between them) if they ever do not, instead of splitting mid-token.
    Span nameLine = new Span(fullName, userName);
    nameLine.addClassName("settings-account-overview__name-line");
    Span hint = new Span("Your account settings");
    // The old --tertiary-text-color measured 2.95:1 on white, below WCAG AA for small text. This
    // hint is what states that the page holds the user's own settings, so it has to stay legible:
    // --body-text-color clears 12:1 while still reading as secondary next to the bold name.
    hint.addClassNames("text-s", "settings-account-overview__hint");

    Div nameBlock = new Div(nameLine, hint);
    nameBlock.addClassName("settings-account-overview__names");

    add(userAvatar, nameBlock);
  }

  /**
   * The wrapping line holding the full name and the user name, for unit tests.
   */
  Span nameLineForTest() {
    return (Span) getChildren()
        .flatMap(child -> child.getChildren())
        .filter(child -> child.getClassNames().contains("settings-account-overview__name-line"))
        .findFirst().orElseThrow();
  }

  /**
   * The avatar element of the identity block, for unit tests.
   */
  Component avatarForTest() {
    return getChildren()
        .filter(child -> child.getClassNames().contains("settings-account-overview__avatar"))
        .findFirst().orElse(null);
  }
}
