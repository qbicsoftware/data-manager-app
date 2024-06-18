package life.qbic.datamanager.views.projects.project.access;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import life.qbic.datamanager.views.account.UserAvatar;

/**
 * A component displaying a user avatar and user name
 */
public class UserAvatarWithNameComponent extends Div {

  public UserAvatarWithNameComponent(UserAvatar userAvatar, String userName) {
    addClassName("avatar-with-name");
    Span userNameSpan = new Span(userName);
    userNameSpan.addClassName("username");
    add(userAvatar, userNameSpan);
  }

}
