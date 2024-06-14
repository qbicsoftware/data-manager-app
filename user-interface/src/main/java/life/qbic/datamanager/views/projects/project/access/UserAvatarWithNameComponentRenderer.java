package life.qbic.datamanager.views.projects.project.access;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import life.qbic.datamanager.views.account.UserAvatar;

public class UserAvatarWithNameComponentRenderer {

  private UserAvatarWithNameComponentRenderer() {

  }

  public static Component render(UserAvatar userAvatar, String userName) {
    Div container = new Div();
    container.addClassName("avatar-with-name");
    Span userNameSpan = new Span(userName);
    userNameSpan.addClassName("username");
    container.add(userAvatar, userNameSpan);
    return container;
  }

}
