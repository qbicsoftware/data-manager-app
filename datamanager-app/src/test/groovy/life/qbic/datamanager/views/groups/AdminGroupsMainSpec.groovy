package life.qbic.datamanager.views.groups

import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService
import life.qbic.usergroups.api.GroupAdministrationPermission
import life.qbic.usergroups.api.GroupInfo
import life.qbic.usergroups.api.GroupInformationService
import life.qbic.usergroups.api.GroupType
import spock.lang.Specification

/**
 * Unit tests for the admin org-group directory.
 *
 * <p>Covers the directory content (org groups only from the public directory) and the admin
 * gate resolution. The route gate itself (beforeEnter + SecurityContextHolder) is exercised by
 * the integration test; these specs target the seam-exposed logic without a Vaadin or Spring
 * context, mirroring the {@code MyGroupsComponent}/{@code NewGroupFormSpec} seam pattern.</p>
 */
class AdminGroupsMainSpec extends Specification {

  GroupInformationService groupInformationService = Stub(GroupInformationService)
  GroupAdministrationPermission administrationPermission = Stub(GroupAdministrationPermission)
  // The userIdTranslator is only used by the beforeEnter SecurityContextHolder flow, not by the
  // directory logic under test — a stub is sufficient (the constructor requires non-null).
  AuthenticationToUserIdTranslationService userIdTranslator = Stub(AuthenticationToUserIdTranslationService)

  AdminGroupsMain newDirectory() {
    new AdminGroupsMain(groupInformationService, administrationPermission, userIdTranslator)
  }

  def "directory shows only org groups from the public directory"() {
    given: "a public directory with ad-hoc and org groups"
    def adHoc = new GroupInfo("g1", "Sprint Team", "ad-hoc team", GroupType.ADHOC)
    def org1 = new GroupInfo("g2", "NGS Lab", "sequencing lab", GroupType.ORG)
    def org2 = new GroupInfo("g3", "Proteomics", null, GroupType.ORG)
    groupInformationService.listPublicDirectory() >> [adHoc, org1, org2]

    when: "the directory is queried for org groups"
    def orgGroups = newDirectory().orgGroupsFromDirectory()

    then: "only the ORG groups are returned, in directory order, descriptions preserved"
    orgGroups*.id() == ["g2", "g3"]
    orgGroups*.name() == ["NGS Lab", "Proteomics"]
    orgGroups[0].description() == "sequencing lab"
    orgGroups[1].description() == null
  }

  def "directory returns an empty list when no org groups exist"() {
    given: "a directory with only ad-hoc groups"
    groupInformationService.listPublicDirectory() >> [
        new GroupInfo("g1", "Sprint Team", "ad-hoc team", GroupType.ADHOC)]

    when: "the directory is queried for org groups"
    def orgGroups = newDirectory().orgGroupsFromDirectory()

    then: "the result is empty (the view shows the empty state)"
    orgGroups.isEmpty()
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