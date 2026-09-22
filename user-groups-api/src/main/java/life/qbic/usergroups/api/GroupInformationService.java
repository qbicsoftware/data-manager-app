package life.qbic.usergroups.api;

import java.util.List;
import java.util.Optional;

/**
 * <b>Group information service</b>
 *
 * <p>Access facade for the user groups context. Offers clients from other QBiC domains and the
 * application layer (UI) to query group information without exposing any internal domain model
 * details outside the context boundary — mirrors the {@code UserInformationService} pattern of
 * the identity context.</p>
 *
 * <p><b>Visibility policy:</b> group names and descriptions are public (discoverability);
 * membership information is never exposed here — {@link #listMyGroups(String)} only ever returns
 * the <em>caller's own</em> memberships.</p>
 *
 * @since 1.17.0
 */
public interface GroupInformationService {

  /**
   * Looks up a group by its stable group id.
   *
   * @param groupId the group id to look up
   * @return the group information, or an empty {@link Optional} if no group with the given id
   * exists or the group is no longer active
   * @since 1.17.0
   */
  Optional<GroupInfo> findGroupById(String groupId);

  /**
   * Lists all currently active groups for the public directory.
   *
   * <p>Dissolved groups never appear. Exposes identity, name, description and type only — never
   * membership information.</p>
   *
   * @return a list of active {@link GroupInfo} entries
   * @since 1.17.0
   */
  List<GroupInfo> listPublicDirectory();

  /**
   * Lists the active group memberships of the given user ("my groups").
   *
   * <p>Membership data is private: this method only returns memberships of the provided user
   * themselves and never any other user's membership.</p>
   *
   * @param userId the user id to list memberships for
   * @return the caller's active group memberships including their role inside each group
   * @since 1.17.0
   */
  List<MyGroupMembership> listMyGroups(String userId);

  /**
   * Queries whether a desired group name is still available.
   *
   * <p>The comparison is case-insensitive: names that differ only in letter case from an existing
   * group are <em>not</em> available.</p>
   *
   * @param name the desired group name
   * @return {@code true} if the name is still available, {@code false} if it is in use by another
   * group
   * @since 1.17.0
   */
  boolean isGroupNameAvailable(String name);
}