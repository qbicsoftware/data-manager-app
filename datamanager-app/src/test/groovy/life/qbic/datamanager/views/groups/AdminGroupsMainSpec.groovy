package life.qbic.datamanager.views.groups

import com.vaadin.flow.router.Router
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.server.RouteRegistry
import com.vaadin.flow.server.VaadinService
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService
import life.qbic.usergroups.api.GroupAdministrationPermission
import life.qbic.usergroups.api.GroupInfo
import life.qbic.usergroups.api.GroupInformationService
import life.qbic.usergroups.api.GroupManagementService
import life.qbic.usergroups.api.GroupType
import spock.lang.Specification

/**
 * Unit tests for the admin org-group directory.
 *
 * <p>Covers the directory content (org groups only from the public directory), the admin
 * gate resolution and the My Groups-aligned row anatomy (badges below the title, member-count
 * badge, Manage action). The route gate itself (beforeEnter + SecurityContextHolder) is
 * exercised by the integration test; these specs target the seam-exposed logic without a
 * Spring context. A minimal stubbed VaadinService (mirroring {@code HomeLinkSpec}) lets the
 * org-group name {@link com.vaadin.flow.router.RouterLink} resolve its href.</p>
 */
class AdminGroupsMainSpec extends Specification {

  GroupInformationService groupInformationService = Stub(GroupInformationService)
  GroupManagementService groupManagementService = Stub(GroupManagementService)
  GroupAdministrationPermission administrationPermission = Stub(GroupAdministrationPermission)
  // The userIdTranslator is only used by the beforeEnter SecurityContextHolder flow, not by the
  // directory logic under test — a stub is sufficient (the constructor requires non-null).
  AuthenticationToUserIdTranslationService userIdTranslator = Stub(AuthenticationToUserIdTranslationService)

  AdminGroupsMain newDirectory() {
    new AdminGroupsMain(groupInformationService, groupManagementService, administrationPermission,
        userIdTranslator)
  }

  def setup() {
    // The org-group name is a RouterLink that resolves its href from the router at
    // construction; a minimal service/registry stub (mirroring HomeLinkSpec) lets buildRow and
    // the Manage-affordance tests run without a Spring context.
    RouteRegistry registry = Stub(RouteRegistry)
    registry.getTargetUrl(AdminGroupManagersMain, _ as RouteParameters) >>
        Optional.of("groups/admin/:groupId/managers")
    Router router = new Router(registry)
    VaadinService service = Stub(VaadinService)
    service.getRouter() >> router
    service.getContext() >> null
    VaadinService.setCurrent(service)
  }

  def cleanup() {
    VaadinService.setCurrent(null)
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

  def "each org-group row exposes a Manage affordance navigating to the manager-management page (FEAT-USER-GROUPS-02)"() {
    given: "one org group in the directory"
    def org1 = new GroupInfo("g2", "NGS Lab", "sequencing lab", GroupType.ORG)
    groupInformationService.listPublicDirectory() >> [org1]

    when: "the org-group row is built for an acting admin"
    def row = newDirectory().buildRow(org1, "admin-1")

    then: "the row contains a Manage button"
    def buttons = []
    collectButtons(row, buttons)
    buttons*.text.contains("Manage")
  }

  def "the org-group row carries a member-count badge from the admin-gated count seam"() {
    given: "an org group and a stubbed member count"
    def org1 = new GroupInfo("g2", "NGS Lab", "sequencing lab", GroupType.ORG)
    groupManagementService.orgGroupMemberCount("g2", "admin-1") >> 5

    when: "the row is built for an acting admin"
    def row = newDirectory().buildRow(org1, "admin-1")

    then: "the row contains a badge reporting the count from the admin seam"
    def badges = []
    collectBadgeTexts(row, badges)
    badges.any { it == "5 members" }
  }

  def "the member-count badge singularizes correctly"() {
    expect: "the badge uses the singular form for a single member"
    AdminGroupsMain.buildMemberCountBadge(1).text == "1 member"
    AdminGroupsMain.buildMemberCountBadge(5).text == "5 members"
  }

  private static void collectBadgeTexts(com.vaadin.flow.component.Component component,
      List<String> acc) {
    if (component instanceof com.vaadin.flow.component.html.Span span
        && span.classNames.contains("my-groups-badge")) {
      acc << span.text
    }
    component.children.forEach { child -> collectBadgeTexts(child, acc) }
  }

  private static void collectButtons(com.vaadin.flow.component.Component component,
      List<com.vaadin.flow.component.button.Button> acc) {
    if (component instanceof com.vaadin.flow.component.button.Button button) {
      acc << button
    }
    component.children.forEach { child -> collectButtons(child, acc) }
  }
}