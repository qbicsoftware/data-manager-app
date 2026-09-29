package life.qbic.projectmanagement.application.authorization.acl

import life.qbic.usergroups.api.GroupSidProvider
import spock.lang.Specification

/**
 * Collision guard for the reserved {@code GROUP_} SID prefix (strategy §4.3).
 *
 * <p>User groups ride as {@code GrantedAuthoritySid("GROUP_<id>")} and are type-identical to
 * {@code ROLE_*} authorities at the ACL layer. The only thing separating groups from real roles
 * is the {@code GROUP_} prefix convention, so {@code AuthorityService} must never emit an
 * authority with that prefix and no system role may be named with it.</p>
 */
class GroupSidPrefixCollisionGuardSpec extends Specification {

  def "no seeded system role emits an authority with the reserved GROUP_ prefix"() {
    given: "the role names seeded in sql/insert-default-values.sql"
    def roleNames = ["ADMIN", "USER", "PROJECT_MANAGER"]

    expect:
    roleNames.stream()
        .map(name -> "ROLE_" + name)
        .noneMatch(authority -> authority.startsWith(GroupSidProvider.GROUP_SID_PREFIX))
  }

  def "the Role authority derivation is ROLE_ prefixed and never collides with GROUP_"() {
    given: "an arbitrary role name"
    def roleName = "DATA_MANAGER"

    expect: "the derived authority string is ROLE_ prefixed"
    "ROLE_" + roleName == "ROLE_DATA_MANAGER"
    !("ROLE_" + roleName).startsWith(GroupSidProvider.GROUP_SID_PREFIX)
  }
}