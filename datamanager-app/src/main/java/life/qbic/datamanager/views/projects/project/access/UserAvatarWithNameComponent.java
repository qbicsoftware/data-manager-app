package life.qbic.datamanager.views.projects.project.access;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import life.qbic.datamanager.views.account.UserAvatar;

/**
 * A component displaying a user avatar and user name
 */
public class UserAvatarWithNameComponent extends Div {

  private final Span userNameSpan;
  private final UserAvatar userAvatar;

  public UserAvatarWithNameComponent(UserAvatar userAvatar, String userName) {
    addClassName("avatar-with-name");
    userNameSpan = new Span(userName);
    userNameSpan.addClassName("username");
    this.userAvatar = userAvatar;
    add(this.userAvatar, userNameSpan);
  }

  public Component getUserNameComponent() {
    return userNameSpan;
  }

  public Component getUserAvatarComponent() {
    return userAvatar;
  }
}
