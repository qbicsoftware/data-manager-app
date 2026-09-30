package life.qbic.datamanager.views.groups

import life.qbic.usergroups.api.GroupRole
import life.qbic.usergroups.api.GroupType
import life.qbic.usergroups.api.MyGroupMembership
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests for the default {@link MyGroupsComponent.ManagementActionPolicy}.
 *
 * <p>Encodes the FEAT-USER-GROUPS-04 role gates: the owner of an ad-hoc group can manage
 * members, appoint managers, rename and dissolve; a manager can manage members and rename; a
 * plain member and any org-group membership sees no management actions.</p>
 */
class ManagementActionPolicySpec extends Specification {

  private final MyGroupsComponent.ManagementActionPolicy policy =
      MyGroupsComponent.ManagementActionPolicy.defaultPolicy()

  @Unroll
  def "an ad-hoc #role row offers #expected"() {
    given: "a membership in an ad-hoc group"
    MyGroupMembership membership = new MyGroupMembership("g-1", "Group", null, GroupType.ADHOC,
        role, 3 as int)

    when:
    def actions = policy.actionsFor(membership)

    then:
    actions*.name() == expected

    where:
    role            | expected
    GroupRole.OWNER | ["MANAGE_MEMBERS", "APPOINT_MANAGER", "RENAME", "DISSOLVE"]
    GroupRole.MANAGER | ["MANAGE_MEMBERS", "RENAME"]
    GroupRole.MEMBER | []
  }

  @Unroll
  def "an org group row offers #expected for role #role (PO model: manager manages like ad-hoc)"() {
    given:
    MyGroupMembership membership = new MyGroupMembership("org-1", "Org Group", null, GroupType.ORG,
        role, 5 as int)

    when:
    def actions = policy.actionsFor(membership)

    then:
    actions*.name() == expected

    where:
    role              | expected
    // Org groups carry no OWNER membership row (owner-equivalent = QBiC admin at the app
    // layer), so the OWNER case is unreachable via a real row; keep it defensive-empty.
    GroupRole.OWNER   | []
    // An org MANAGER manages the group exactly like an ad-hoc MANAGER: manage members + rename.
    GroupRole.MANAGER | ["MANAGE_MEMBERS", "RENAME"]
    GroupRole.MEMBER  | []
  }
}