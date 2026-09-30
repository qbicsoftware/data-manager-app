package life.qbic.usergroups.application.communication;

/**
 * <b>Group notification messages</b>
 *
 * <p>Plain-text email templates for the group membership notifications of the user groups context.
 * The messages are formatted after the existing {@code Messages} pattern of the identity and
 * project management contexts.</p>
 *
 * @since 1.20.0
 */
public final class Messages {

  private Messages() {
    // utility class
  }

  /**
   * Informs a user that they have been added to a group and therefore may have gained access to
   * the projects the group is shared with.
   *
   * @param fullName the recipient's full name for the salutation
   * @param groupName the name of the group the user was added to
   * @return the formatted message body
   * @since 1.20.0
   */
  public static String addedToGroup(String fullName, String groupName) {
    return String.format("""
        Dear %s,

        you have been added to the user group "%s".

        As a member of this group you may now access the projects and data the group is shared
        with. If this was not intended, please contact the group owner or our support.

        With kind regards,

        Your QBiC team
        """, fullName, groupName);
  }

  /**
   * Informs a user that they have been removed from a group.
   *
   * @param fullName the recipient's full name for the salutation
   * @param groupName the name of the group the user was removed from
   * @return the formatted message body
   * @since 1.20.0
   */
  public static String removedFromGroup(String fullName, String groupName) {
    return String.format("""
        Dear %s,

        you have been removed from the user group "%s".

        As a result you may no longer access the projects and data the group was shared with.
        If this was not intended, please contact the group owner or our support.

        With kind regards,

        Your QBiC team
        """, fullName, groupName);
  }
}