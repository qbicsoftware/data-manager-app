package life.qbic.datamanager.views.settings;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import life.qbic.datamanager.views.account.UserAvatar;
import life.qbic.identity.api.UserInfo;

/**
 * Account Overview Header
 * <p>
 * Displays the identity of the currently logged-in user at the top of the settings aside,
 * directly above the settings navigation: the user's avatar, full name, platform user name and a
 * short hint that the shown settings belong to the user's account. The block is compact (avatar
 * beside the name) because it accompanies the settings menu rather than heading a page.
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
    Span nameLine = new Span(fullName, new Text(" "), userName);
    Span hint = new Span("Your account settings");
    // The old --tertiary-text-color measured 2.95:1 on white, below WCAG AA for small text. This
    // hint is what states that the page holds the user's own settings, so it has to stay legible:
    // --body-text-color clears 12:1 while still reading as secondary next to the bold name.
    hint.addClassNames("text-s", "settings-account-overview__hint");

    Div nameBlock = new Div(nameLine, hint);
    nameBlock.addClassName("settings-account-overview__names");

    add(userAvatar, nameBlock);
  }
}
