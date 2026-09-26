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
}