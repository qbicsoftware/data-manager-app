package life.qbic.usergroups.domain.model

import java.time.Instant

import spock.lang.Specification

/**
 * Tests for the {@link UserGroup} aggregate.
 *
 * <p>The aggregate is type-agnostic: it carries <em>no</em> role gates and <em>no</em> type
 * branches (org vs ad-hoc authorization lives at the application layer). It only upholds the
 * unconditional invariants: roster integrity (no duplicates), the dissolved-guard, the
 * self-removal guard, the "the OWNER is the permanent anchor and can never be removed" invariant,
 * and the absence of auto-dissolve.</p>
 */
class UserGroupSpec extends Specification {

  private static Instant NOW = Instant.parse("2026-09-22T10:00:00Z")

  private static UserGroup createAdHoc(GroupId id = GroupId.create(),
      String creator = "creator-user") {
    return UserGroup.createAdHoc(id, GroupName.from("NGS Lab"),
        GroupDescription.from("A test lab"), creator, NOW)
  }

  def "An ad-hoc group is created with the creator as OWNER, ADHOC type and ACTIVE status"() {
    given:
    GroupId id = GroupId.create()
    String creator = "creator-user"

    when:
    UserGroup group = UserGroup.createAdHoc(id, GroupName.from("NGS Lab"),
        GroupDescription.from("Test lab"), creator, NOW)

    then:
    group.id() == id
    group.type() == GroupType.ADHOC
    group.status() == GroupStatus.ACTIVE
    group.createdBy() == creator
    group.createdAt() == NOW
    group.isActive()
    group.memberships().size() == 1
    group.memberships().get(0).userId() == creator
    group.memberships().get(0).role() == GroupRole.OWNER
  }

  def "The roster of a newly created ad-hoc group is never empty (OWNER is the anchor)"() {
    expect:
    createAdHoc().memberships().size() == 1
    createAdHoc().memberships().get(0).role() == GroupRole.OWNER
  }

  def "An org group is created ACTIVE with type ORG and an empty roster (no OWNER row)"() {
    given:
    GroupId id = GroupId.create()
    String admin = "admin-user"

    when:
    UserGroup group = UserGroup.createOrg(id, GroupName.from("NGS Lab"),
        GroupDescription.from("QBiC NGS lab"), admin, NOW)

    then:
    group.id() == id
    group.type() == GroupType.ORG
    group.status() == GroupStatus.ACTIVE
    group.createdBy() == admin
    group.createdAt() == NOW
    group.isActive()
    group.memberships().isEmpty()
  }

  def "Org group creation rejects a blank admin user id"() {
    when:
    UserGroup.createOrg(GroupId.create(), GroupName.from("NGS Lab"),
        GroupDescription.from("desc"), "  ", NOW)

    then:
    thrown(IllegalArgumentException)
  }

  def "Creation rejects a blank creator user id"() {
    when:
    UserGroup.createAdHoc(GroupId.create(), GroupName.from("NGS Lab"),
        GroupDescription.from("desc"), "  ", NOW)

    then:
    thrown(IllegalArgumentException)
  }

  def "Removing a membership never dissolves the group (no auto-dissolve anywhere)"() {
    given: "an ad-hoc group whose only member is the owner"
    UserGroup group = createAdHoc(GroupId.create(), "only-member")

    when: "a regular member (added earlier) is removed"
    group.addMember("member-2", NOW)
    boolean removed = group.removeMembership("member-2")

    then: "the membership is gone but the group stays ACTIVE"
    removed
    group.memberships().size() == 1
    group.memberships().get(0).userId() == "only-member"
    group.status() == GroupStatus.ACTIVE
    group.isActive()
  }

  def "Removing a non-member does nothing"() {
    given:
    UserGroup group = createAdHoc()

    when:
    boolean removed = group.removeMembership("i-am-not-a-member")

    then:
    !removed
    group.status() == GroupStatus.ACTIVE
    group.memberships().size() == 1
  }

  def "The OWNER cannot be removed (permanent anchor, group never empty)"() {
    given: "an ad-hoc group with a second member"
    UserGroup group = createAdHoc()
    group.addMember("member-2", NOW)

    when: "removing the owner via removeMember"
    group.removeMember("member-2", "creator-user")

    then: "the owner is protected"
    thrown(IllegalArgumentException)
    group.memberships().size() == 2
  }

  def "Removing an OWNER via removeMembership is not permitted either"() {
    given: "an ad-hoc group"
    UserGroup group = createAdHoc(GroupId.create(), "only-member")

    when: "the owner removes themselves"
    group.removeMembership("only-member")

    then: "the aggregate removes the membership (the app layer blocks owner self-removal)"
    group.memberships().isEmpty()
  }

  def "An actor cannot remove themselves via removeMember (must use removeMembership)"() {
    given: "an ad-hoc group with a member"
    UserGroup group = createAdHoc()
    group.addMember("member-2", NOW)

    when:
    group.removeMember("member-2", "member-2")

    then:
    thrown(IllegalArgumentException)
  }

  def "Explicit dissolve purges all memberships and is idempotent"() {
    given:
    UserGroup group = createAdHoc()
    group.addMember("member-2", NOW)

    when:
    group.dissolve()

    then:
    group.status() == GroupStatus.DISSOLVED
    group.memberships().isEmpty()

    when: "dissolving again is a no-op"
    group.dissolve()

    then:
    group.status() == GroupStatus.DISSOLVED
    group.memberships().isEmpty()
  }

  def "Adding a member to a dissolved group is rejected"() {
    given:
    UserGroup group = createAdHoc()
    group.dissolve()

    when:
    group.addMember("new-user", NOW)

    then:
    thrown(IllegalStateException)
  }

  def "Two groups with the same id are equal regardless of other attributes"() {
    given:
    GroupId id = GroupId.create()

    expect:
    createAdHoc(id) == createAdHoc(id)
  }

  // ── type-agnostic primitives (shared by both group types) ────────────────

  def "addMember adds a MEMBER to any group type without an acting-role parameter"() {
    given: "an ad-hoc group and an org group"
    UserGroup adHoc = createAdHoc()
    UserGroup org = createOrg()

    when:
    adHoc.addMember("alice", NOW)
    org.addMember("carol", NOW)

    then: "the ad-hoc group member has the MEMBER role"
    adHoc.memberships().find { it.userId() == "alice" }.role() == GroupRole.MEMBER
    and: "the org group (empty roster) also accepts members"
    org.memberships().find { it.userId() == "carol" }.role() == GroupRole.MEMBER
  }

  def "addMember rejects a duplicate member"() {
    given: "an ad-hoc group with a member"
    UserGroup group = createAdHoc()
    group.addMember("alice", NOW)

    when:
    group.addMember("alice", NOW)

    then:
    thrown(IllegalArgumentException)
    group.memberships().size() == 2
  }

  def "setRoleOf promotes a MEMBER to MANAGER and demotes back (type-agnostic)"() {
    given: "an ad-hoc group with a member"
    UserGroup group = createAdHoc()
    group.addMember("alice", NOW)

    when: "promoting"
    boolean promoted = group.setRoleOf("alice", GroupRole.MANAGER)

    then:
    promoted
    group.memberships().find { it.userId() == "alice" }.role() == GroupRole.MANAGER

    when: "demoting"
    boolean demoted = group.setRoleOf("alice", GroupRole.MEMBER)

    then:
    demoted
    group.memberships().find { it.userId() == "alice" }.role() == GroupRole.MEMBER
  }

  def "setRoleOf is a no-op when the member already holds the role"() {
    given:
    UserGroup group = createAdHoc()
    group.addMember("alice", NOW)

    expect:
    !group.setRoleOf("alice", GroupRole.MEMBER)
  }

  def "setRoleOf cannot grant OWNER and rejects non-members and dissolved groups"() {
    given: "an ad-hoc group"
    UserGroup group = createAdHoc()

    when: "granting OWNER"
    group.setRoleOf("creator-user", GroupRole.OWNER)
    then:
    thrown(IllegalArgumentException)

    when: "targeting a non-member"
    group.setRoleOf("ghost", GroupRole.MANAGER)
    then:
    thrown(IllegalArgumentException)

    when: "on a dissolved group"
    group.dissolve()
    group.setRoleOf("creator-user", GroupRole.MANAGER)
    then:
    thrown(IllegalStateException)
  }

  def "rename and updateDescription are type-agnostic and role-gate-free"() {
    given: "an org group with an empty roster (no OWNER, no membership)"
    UserGroup group = createOrg()

    when: "renaming + describing"
    group.rename(GroupName.from("QBiC NGS Core"))
    group.updateDescription(GroupDescription.from("New description"))

    then:
    group.name().value() == "QBiC NGS Core"
    group.description().value().get() == "New description"
  }

  def "rename and updateDescription reject a dissolved group"() {
    given: "a dissolved group"
    UserGroup group = createAdHoc()
    group.dissolve()

    when:
    group.rename(GroupName.from("X"))
    then:
    thrown(IllegalStateException)

    when:
    group.updateDescription(GroupDescription.from("X"))
    then:
    thrown(IllegalStateException)
  }

  def "removeMember works identically for org and ad-hoc groups (removes a non-owner)"() {
    given: "an ad-hoc group with a member and an org group with a member"
    UserGroup adHoc = createAdHoc()
    adHoc.addMember("alice", NOW)
    UserGroup org = createOrg()
    org.addMember("carol", NOW)

    when: "removing the member"
    boolean adHocRemoved = adHoc.removeMember("creator-user", "alice")
    boolean orgRemoved = org.removeMember("admin-user", "carol")

    then: "both succeed identically"
    adHocRemoved
    adHoc.memberships().size() == 1
    orgRemoved
    org.memberships().isEmpty()
  }

  private static UserGroup createOrg(GroupId id = GroupId.create(),
      String admin = "admin-user") {
    return UserGroup.createOrg(id, GroupName.from("NGS Lab"),
        GroupDescription.from("QBiC NGS lab"), admin, NOW)
  }
}