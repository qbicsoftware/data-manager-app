package life.qbic.usergroups.domain.model

import java.time.Instant

import spock.lang.Specification

/**
 * Tests for {@link GroupMembership} identity and {@link GroupMembershipId}.
 */
class GroupMembershipSpec extends Specification {

  def "Two membership ids with the same group and user are equal and have the same hash"() {
    given:
    GroupMembershipId id1 = new GroupMembershipId("group-1", "user-1")
    GroupMembershipId id2 = new GroupMembershipId("group-1", "user-1")

    expect:
    id1 == id2
    id1.hashCode() == id2.hashCode()
  }

  def "Membership ids differ when either the group or the user differs"() {
    given:
    GroupMembershipId original = new GroupMembershipId("group-1", "user-1")

    expect:
    original != new GroupMembershipId("group-2", "user-1")
    original != new GroupMembershipId("group-1", "user-2")
  }

  def "A blank group or user id is rejected"() {
    when:
    new GroupMembershipId(groupId, userId)

    then:
    thrown(IllegalArgumentException)

    where:
    groupId   | userId
    null      | "user-1"
    "group-1" | null
    ""        | "user-1"
    "group-1" | "  "
  }

  def "GroupMembership equality is based on the composite (groupId, userId) identity"() {
    given:
    GroupId groupId = GroupId.from("11111111-1111-1111-1111-111111111111")
    GroupMembership m1 = GroupMembership.create(groupId, "user-1", GroupRole.OWNER,
        Instant.parse("2026-09-01T10:00:00Z"))
    GroupMembership m2 = GroupMembership.create(groupId, "user-1", GroupRole.MANAGER,
        Instant.parse("2026-09-02T10:00:00Z"))
    GroupMembership m3 = GroupMembership.create(groupId, "user-2", GroupRole.OWNER,
        Instant.parse("2026-09-01T10:00:00Z"))

    expect:
    m1 == m2          // same (groupId, userId), different role -> equal identity
    m1 != m3          // different userId
    m1.hashCode() == m2.hashCode()
  }

  def "GroupMembership exposes its inputs"() {
    given:
    GroupId groupId = GroupId.from("11111111-1111-1111-1111-111111111111")
    Instant joinedAt = Instant.parse("2026-09-01T10:00:00Z")

    when:
    GroupMembership membership = GroupMembership.create(groupId, "user-1", GroupRole.OWNER,
        joinedAt)

    then:
    membership.userId() == "user-1"
    membership.role() == GroupRole.OWNER
    membership.joinedAt() == joinedAt
    membership.id().groupId() == groupId.get()
  }
}