package life.qbic.datamanager.views.settings;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import life.qbic.datamanager.views.account.UserAvatar;
import life.qbic.identity.api.UserInfo;

/**
 * Account Overview Header
 * <p>
 * Displays the identity of the currently logged-in user above the settings navigation: the user's
 * avatar, full name, platform user name and a short hint that the shown settings belong to the
 * user's account.
 */
public class AccountOverviewHeader extends Div {

  public AccountOverviewHeader(UserInfo userInfo) {
    requireNonNull(userInfo, "userInfo must not be null");
    addClassName("settings-account-overview");

    UserAvatar userAvatar = new UserAvatar();
    userAvatar.setUserId(userInfo.id());
    userAvatar.setName(userInfo.platformUserName());
    userAvatar.addClassName("settings-account-overview__avatar");

    Span fullName = new Span(userInfo.fullName());
    fullName.addClassName("font-bold");
    Span userName = new Span(userInfo.platformUserName());
    userName.addClassNames("text-s", "text-secondary");
    Span hint = new Span("Your account settings");
    hint.addClassNames("text-s", "text-tertiary");

    Div nameBlock = new Div(fullName, userName, hint);
    nameBlock.addClassName("settings-account-overview__names");

    add(userAvatar, nameBlock);
  }
}