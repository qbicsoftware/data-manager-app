package life.qbic.datamanager.profilepicture;

/**
 * Port answering whether a user may manage a group's profile picture.
 *
 * <p>Implemented at the composition root by delegating to the user-groups context
 * ({@code GroupService#canManageProfilePicture}), so this package never depends on the group
 * domain directly. Keeping it a functional port makes {@link ProfilePictureService} testable
 * without the group module.</p>
 *
 * @since 1.19.0
 */
@FunctionalInterface
public interface GroupPictureAuthorization {

  /**
   * @param groupId      the group whose picture is being managed
   * @param actingUserId the user attempting the change
   * @return {@code true} if the user may manage the group's picture
   */
  boolean canManage(String groupId, String actingUserId);
}
