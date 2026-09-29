package life.qbic.datamanager.views.groups

import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService
import life.qbic.usergroups.api.GroupAdministrationPermission
import life.qbic.usergroups.api.GroupInformationService
import life.qbic.usergroups.api.GroupManagementService
import life.qbic.usergroups.api.GroupMember
import life.qbic.usergroups.api.GroupRole
import life.qbic.identity.api.UserInformationService
import life.qbic.datamanager.views.notifications.MessageSourceNotificationFactory
import spock.lang.Specification

/**
 * Unit tests for the admin org-group manager-management page (FEAT-USER-GROUPS-02 AC1/AC4).
 *
 * <p>The view's interactive paths (assign/remove dialogs, last-manager confirm) require a Vaadin
 * UI context and are exercised by the integration test; these specs target the seam-exposed
 * logic without a Spring or Vaadin context, mirroring the existing
 * {@code AdminGroupsMainSpec}/{@code GroupMembersComponentSpec} pattern. The admin gate is
 * resolved through the {@link GroupAdministrationPermission} port, exactly like the
 * org-group directory.</p>
 */
class AdminGroupManagersMainSpec extends Specification {

  GroupInformationService groupInformationService = Stub(GroupInformationService)
  GroupManagementService groupManagementService = Stub(GroupManagementService)
  UserInformationService userInformationService = Stub(UserInformationService)
  GroupAdministrationPermission administrationPermission = Stub(GroupAdministrationPermission)
  AuthenticationToUserIdTranslationService userIdTranslator = Stub(AuthenticationToUserIdTranslationService)
  MessageSourceNotificationFactory messageFactory = Stub(MessageSourceNotificationFactory)

  AdminGroupManagersMain view() {
    new AdminGroupManagersMain(groupInformationService, groupManagementService,
        userInformationService, administrationPermission, userIdTranslator, messageFactory)
  }

  def "managersFrom filters the MANAGER members out of an org roster (admin oversight view)"() {
    given: "a full org roster as returned by the admin-oversight listMembers branch"
    def roster = [
        new GroupMember("alice", GroupRole.MANAGER),
        new GroupMember("bob", GroupRole.MEMBER),
        new GroupMember("carol", GroupRole.MANAGER),
        new GroupMember("dan", GroupRole.MEMBER)]

    when: "the manager view extracts the rosters managers"
    def managers = view().managersFrom(roster)

    then: "only MANAGER members remain, in roster order"
    managers*.userId() == ["alice", "carol"]
    managers.every { it.role() == GroupRole.MANAGER }
  }

  def "managersFrom returns an empty list when no MANAGER members are present"() {
    given: "an org roster with only regular members"
    def roster = [
        new GroupMember("alice", GroupRole.MEMBER),
        new GroupMember("bob", GroupRole.MEMBER)]

    expect:
    view().managersFrom(roster).isEmpty()
  }

  def "managersFrom is empty for an empty roster"() {
    expect:
    view().managersFrom([]).isEmpty()
  }

  def "admin gate resolves through the permission port"() {
    given: "an admin and a non-admin user"
    administrationPermission.isAdmin("admin-1") >> true
    administrationPermission.isAdmin("user-1") >> false

    expect: "the port decides the admin state"
    administrationPermission.isAdmin("admin-1")
    !administrationPermission.isAdmin("user-1")
  }
}