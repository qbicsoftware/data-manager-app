package life.qbic.datamanager.views.account;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarGroup;
import life.qbic.datamanager.profilepicture.ProfilePictureOwnerType;
import life.qbic.datamanager.profilepicture.ProfilePictureUrlResolver;

/**
 * A custom implementation of the Avatar class.
 *
 * <p>The avatar points at the stable profile-picture URL for its owner. That endpoint serves the
 * stored picture when one exists (redirecting to its immutable, content-hashed URL) and otherwise
 * serves the identicon derived from the owner id. The same component serves both user and group
 * owners.</p>
 */
public class UserAvatar extends Avatar {

  public UserAvatar() {
    addClassName("user-avatar");
  }

  /**
   * Renders the given user's avatar (stored picture or identicon).
   *
   * @param userId the user id
   */
  public void setUserId(String userId) {
    setOwner(ProfilePictureOwnerType.USER, userId);
  }

  /**
   * Renders the given group's avatar (stored picture or identicon).
   *
   * @param groupId the group id
   */
  public void setGroupId(String groupId) {
    setOwner(ProfilePictureOwnerType.GROUP, groupId);
  }

  /**
   * Points the avatar at the stable URL for the given owner.
   *
   * @param ownerType the owner kind
   * @param ownerId   the user or group id
   */
  public void setOwner(ProfilePictureOwnerType ownerType, String ownerId) {
    setImage(ProfilePictureUrlResolver.stablePath(ownerType, ownerId));
  }

  /**
   * Re-points the avatar at the stable URL with a cache-busting parameter so a just-changed picture
   * is re-fetched by the browser.
   *
   * @param ownerType the owner kind
   * @param ownerId   the user or group id
   */
  public void refresh(ProfilePictureOwnerType ownerType, String ownerId) {
    setImage(ProfilePictureUrlResolver.stablePath(ownerType, ownerId) + "?v="
        + System.currentTimeMillis());
  }

  public static class UserAvatarGroupItem extends AvatarGroup.AvatarGroupItem {

    public UserAvatarGroupItem(String userName, String userId) {
      this(userName, ProfilePictureOwnerType.USER, userId);
    }

    /**
     * Creates a group item for a user or a group owner.
     *
     * @param userName  the display name
     * @param ownerType the owner kind
     * @param ownerId   the user or group id
     */
    public UserAvatarGroupItem(String userName, ProfilePictureOwnerType ownerType, String ownerId) {
      setImage(ProfilePictureUrlResolver.stablePath(ownerType, ownerId));
      super.setName(userName);
    }

    public static UserAvatarGroupItem forGroup(String groupName, String groupId) {
      return new UserAvatarGroupItem(groupName, ProfilePictureOwnerType.GROUP, groupId);
    }
  }
}
