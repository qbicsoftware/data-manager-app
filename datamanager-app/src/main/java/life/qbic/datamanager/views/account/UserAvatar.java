package life.qbic.datamanager.views.account;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
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

  public void setUserId(String userId) {
    setImageHandler(getDownloadHandler(userId));
  }

  private static DownloadHandler getDownloadHandler(String userid) {
    return DownloadHandler.fromInputStream(event ->
    {
      byte[] imageBytes = IdenticonGenerator.generateIdenticonSVG(userid).getBytes(
          StandardCharsets.UTF_8);
      return new DownloadResponse(
          new ByteArrayInputStream(imageBytes),
          "user-identicon.svg", null, imageBytes.length);
    });
  }

  public static class UserAvatarGroupItem extends AvatarGroup.AvatarGroupItem {

    public UserAvatarGroupItem(String userName, String userId) {
      //new logic -> first more important; image then name -> image; name then image -> name is shown
      setImageHandler(UserAvatar.getDownloadHandler(userId));
      super.setName(userName);
    }
  }
}
