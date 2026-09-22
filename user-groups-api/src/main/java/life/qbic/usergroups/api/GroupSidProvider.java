package life.qbic.usergroups.api;

import java.util.List;

/**
 * <b>Group SID provider</b>
 *
 * <p>Provides the Spring Security ACL side {@code GrantedAuthoritySid} strings of a user's group
 * memberships. This is the seam the project-management authorization layer consumes (see the
 * user-groups strategy, section 4.3): a {@code GroupAwareSidRetrievalStrategy} derives the
 * caller's group sids live on every permission check.</p>
 *
 * <p>Sids follow the reserved {@code GROUP_<groupId>} prefix convention keyed on the stable group
 * id (never the display name), so ACEs stay valid across group renames. Only <em>active</em>
 * memberships produce sids — dissolved groups never grant access.</p>
 *
 * @since 1.17.0
 */
public interface GroupSidProvider {

  /**
   * Reserved authority-sid prefix for user group sids.
   *
   * <p>Must never be emitted by {@code AuthorityService} for real {@code ROLE_*} authorities and
   * no system role may be named with this prefix (collision guard, strategy section 4.3).</p>
   */
  String GROUP_SID_PREFIX = "GROUP_";

  /**
   * Returns the group sid strings of all <em>active</em> group memberships of the given user.
   *
   * @param userId the user id to resolve group sids for
   * @return a list of {@code "GROUP_<groupId>"} strings, one per active membership
   * @since 1.17.0
   */
  List<String> listGroupSidsForUser(String userId);
}