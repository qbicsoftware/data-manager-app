package life.qbic.usergroups.domain.model

import java.time.Instant

import spock.lang.Specification

/**
 * Tests for the {@link UserGroup} aggregate.
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

  def "The roster of a newly created group is never empty"() {
    expect:
    createAdHoc().memberships().size() == 1
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

  def "Removing the last member of an ad-hoc group dissolves it and purges the roster"() {
    given:
    UserGroup group = createAdHoc(GroupId.create(), "only-member")

    when:
    boolean dissolved = group.removeMembership("only-member")

    then:
    dissolved
    group.status() == GroupStatus.DISSOLVED
    group.memberships().isEmpty()
    !group.isActive()
    // soft-dissolve: the group row concept is kept (the aggregate instance still exists)
  }

  def "Removing one member of a non-empty ad-hoc group keeps it ACTIVE"() {
    given:
    UserGroup group = createAdHoc()
    group.addMember("creator-user", "member-2", NOW)

    when:
    boolean dissolved = group.removeMembership("member-2")

    then:
    !dissolved
    group.status() == GroupStatus.ACTIVE
    group.memberships().size() == 1
    group.memberships().get(0).userId() == "creator-user"
  }

  def "Removing a non-member does nothing and reports no dissolution"() {
    given:
    UserGroup group = createAdHoc()

    when:
    boolean dissolved = group.removeMembership("i-am-not-a-member")

    then:
    !dissolved
    group.status() == GroupStatus.ACTIVE
    group.memberships().size() == 1
  }

  def "Explicit dissolve purges all memberships and is idempotent"() {
    given:
    UserGroup group = createAdHoc()
    group.addMember("creator-user", "member-2", NOW)

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

  def "Adding a duplicate member is rejected"() {
    given:
    UserGroup group = createAdHoc()

    when: "adding an existing member"
    group.addMember("creator-user", "creator-user", NOW)

    then:
    thrown(IllegalArgumentException)
  }

  def "Adding a member to a dissolved group is rejected"() {
    given:
    UserGroup group = createAdHoc()
    group.dissolve()

    when: "adding a member to a dissolved group"
    group.addMember("creator-user", "new-user", NOW)

    then:
    thrown(IllegalStateException)
  }

  def "Two groups with the same id are equal regardless of other attributes"() {
    given:
    GroupId id = GroupId.create()

    expect:
    createAdHoc(id) == createAdHoc(id)
  }

  // ── appointOrgManager / removeOrgManager (FEAT-USER-GROUPS-02) ────────────

  private static UserGroup createOrg(GroupId id = GroupId.create(),
      String admin = "admin-user") {
    return UserGroup.createOrg(id, GroupName.from("NGS Lab"),
        GroupDescription.from("QBiC NGS lab"), admin, NOW)
  }

  def "appointOrgManager directly appoints a non-member as a MANAGER without creating an OWNER row"() {
    given: "an org group with an empty roster"
    UserGroup group = createOrg()

    when: "the admin appoints a non-member as manager"
    boolean changed = group.appointOrgManager("admin-user", "lab-lead", NOW)

    then: "the user becomes a MANAGER membership and no OWNER row exists"
    changed
    group.memberships().size() == 1
    group.memberships().get(0).userId() == "lab-lead"
    group.memberships().get(0).role() == GroupRole.MANAGER
    group.memberships().every { it.role() != GroupRole.OWNER }
    group.type() == GroupType.ORG
  }

  def "appointOrgManager promotes an existing MEMBER to MANAGER"() {
    given: "an org group with a manager who added a plain member"
    UserGroup group = createOrg()
    group.appointOrgManager("admin-user", "manager-1", NOW)
    group.addMember("manager-1", "alice", NOW)

    when: "the admin promotes the member"
    boolean changed = group.appointOrgManager("admin-user", "alice", NOW)

    then: "the member becomes a MANAGER"
    changed
    group.memberships().size() == 2
    group.memberships().every { it.role() != GroupRole.OWNER }
    group.memberships().find { it.userId() == "alice" }.role() == GroupRole.MANAGER
    group.memberships().find { it.userId() == "manager-1" }.role() == GroupRole.MANAGER
  }

  def "appointOrgManager is a no-op when the user is already a MANAGER"() {
    given: "an org group where the user is already a manager"
    UserGroup group = createOrg()
    group.appointOrgManager("admin-user", "lab-lead", NOW)

    when:
    boolean changed = group.appointOrgManager("admin-user", "lab-lead", NOW)

    then:
    !changed
    group.memberships().size() == 1
    group.memberships().get(0).role() == GroupRole.MANAGER
  }

  def "appointOrgManager rejects a dissolved group"() {
    given: "a dissolved org group"
    UserGroup group = createOrg()
    group.dissolve()

    when:
    group.appointOrgManager("admin-user", "lab-lead", NOW)

    then:
    thrown(IllegalStateException)
  }

  def "appointOrgManager rejects an ad-hoc group"() {
    given: "an ad-hoc group"
    UserGroup group = createAdHoc()

    when:
    group.appointOrgManager("creator-user", "other-user", NOW)

    then:
    thrown(IllegalStateException)
  }

  def "appointOrgManager rejects blank ids"() {
    given: "an org group"
    UserGroup group = createOrg()

    when:
    group.appointOrgManager("  ", "lab-lead", NOW)

    then:
    thrown(IllegalArgumentException)

    when:
    group.appointOrgManager("admin-user", "  ", NOW)

    then:
    thrown(IllegalArgumentException)
  }

  def "removeOrgManager removes a manager's membership and reports true"() {
    given: "an org group with two managers"
    UserGroup group = createOrg()
    group.appointOrgManager("admin-user", "alice", NOW)
    group.appointOrgManager("admin-user", "bob", NOW)

    when: "the admin removes alice"
    boolean removed = group.removeOrgManager("admin-user", "alice")

    then:
    removed
    group.memberships().size() == 1
    group.memberships().get(0).userId() == "bob"
    group.memberships().get(0).role() == GroupRole.MANAGER
  }

  def "removeOrgManager allows removing the last manager and never dissolves an org group"() {
    given: "an org group with a single manager"
    UserGroup group = createOrg()
    group.appointOrgManager("admin-user", "solo", NOW)

    when: "the admin removes the last manager"
    boolean removed = group.removeOrgManager("admin-user", "solo")

    then: "the group stays ACTIVE, governed by the QBiC admin, with an empty roster (no OWNER)"
    removed
    group.memberships().isEmpty()
    group.status() == GroupStatus.ACTIVE
    group.type() == GroupType.ORG
  }

  def "removeOrgManager returns false for a non-member"() {
    given: "an org group with one manager"
    UserGroup group = createOrg()
    group.appointOrgManager("admin-user", "alice", NOW)

    when: "removing a non-member"
    boolean removedUnknown = group.removeOrgManager("admin-user", "ghost")

    then:
    !removedUnknown
    group.memberships().size() == 1
  }

  def "removeOrgManager removes a regular member's membership"() {
    given: "an org group with a manager and a regular member"
    UserGroup group = createOrg()
    group.appointOrgManager("admin-user", "manager-1", NOW)
    group.addMember("manager-1", "regular-1", NOW)

    when: "the admin removes the regular member"
    boolean removed = group.removeOrgManager("admin-user", "regular-1")

    then:
    removed
    group.memberships().size() == 1
    group.memberships().get(0).userId() == "manager-1"
  }

  def "removeOrgManager rejects a dissolved or ad-hoc group, and blank ids"() {
    given: "a dissolved org group"
    UserGroup dissolved = createOrg()
    dissolved.dissolve()

    when:
    dissolved.removeOrgManager("admin-user", "alice")

    then:
    thrown(IllegalStateException)
  }

  def "removeOrgManager rejects an ad-hoc group"() {
    given: "an ad-hoc group"
    UserGroup adHoc = createAdHoc()

    when:
    adHoc.removeOrgManager("creator-user", "alice")

    then:
    thrown(IllegalStateException)
  }

  def "removeOrgManager rejects a blank admin id"() {
    given: "an org group and a blank admin id"
    UserGroup orgGroup = createOrg()

    when:
    orgGroup.removeOrgManager("  ", "alice")

    then:
    thrown(IllegalArgumentException)
  }
}