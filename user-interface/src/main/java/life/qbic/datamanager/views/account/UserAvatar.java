package life.qbic.datamanager.views.account;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.server.StreamResource;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import life.qbic.datamanager.views.identicon.IdenticonGenerator;

/**
 * A custom implementation of the Avatar class. The image shown is computed on the name of the
 * avatar.
 */
public class UserAvatar extends Avatar {


  public UserAvatar() {
    addClassName("user-avatar");
  }

  @Override
  public void setName(String name) {
    setImageResource(getImageResource(name));
  }

  private static StreamResource getImageResource(String name) {
    return new StreamResource("user-identicon.svg",
        () -> new ByteArrayInputStream(IdenticonGenerator.generateIdenticon(name).getBytes(
            StandardCharsets.UTF_8)));
  }

  public static class UserAvatarGroupItem extends AvatarGroup.AvatarGroupItem {

    public UserAvatarGroupItem(String userName, String userId) {
      super.setName(userName);
      setImageResource(UserAvatar.getImageResource(userId));
    }
  }
}
