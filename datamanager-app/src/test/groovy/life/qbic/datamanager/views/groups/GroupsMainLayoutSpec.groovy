package life.qbic.datamanager.views.groups

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.Location
import com.vaadin.flow.router.NavigationTrigger
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.Router
import com.vaadin.flow.router.RouterLink
import com.vaadin.flow.server.RouteRegistry
import com.vaadin.flow.server.VaadinService
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService
import life.qbic.usergroups.api.GroupAdministrationPermission
import life.qbic.usergroups.api.GroupInformationService
import life.qbic.usergroups.api.GroupRole
import life.qbic.usergroups.api.GroupType
import life.qbic.usergroups.api.MyGroupMembership
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification

/**
 * Unit tests for the Groups hub layout.
 *
 * <p>Covers the overview header rendering (live total/owned counts), the refresh on navigation and
 * the admin-gated navigation entries. The layout's {@link GroupsNavigationComponent} renders
 * {@link RouterLink}s, which resolve their href from the {@link VaadinService} router registry at
 * construction time, so a minimal service/registry stub is installed (mirroring
 * {@code TopLevelNavigationComponentSpec}). Nothing here needs a Spring or UI context.</p>
 */
class GroupsMainLayoutSpec extends Specification {

  GroupAdministrationPermission permission = Stub(GroupAdministrationPermission)
  AuthenticationToUserIdTranslationService userIdTranslator =
      Stub(AuthenticationToUserIdTranslationService)
  GroupInformationService groupInformationService = Stub(GroupInformationService)
  Router router

  def setup() {
    RouteRegistry registry = Stub(RouteRegistry)
    registry.getTargetUrl(MyGroupsMain, _ as RouteParameters) >> Optional.of("groups")
    registry.getTargetUrl(AdminGroupsMain, _ as RouteParameters) >> Optional.of("groups/admin")
    registry.getTargetUrl(AdminProfilePictureAuditMain, _ as RouteParameters) >>
        Optional.of("groups/admin/pictures")
    router = new Router(registry)
    VaadinService service = Stub(VaadinService)
    service.getRouter() >> router
    service.getContext() >> null
    VaadinService.setCurrent(service)

    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken("principal", "n/a"))
    userIdTranslator.translateToUserId(_) >> Optional.of("u1")
  }

  def cleanup() {
    SecurityContextHolder.clearContext()
    VaadinService.setCurrent(null)
  }

  def "renders the overview header with the live total and owned counts"() {
    given: "two owned groups, one ad-hoc membership and one org membership"
    groupInformationService.listMyGroups("u1") >> [
        membership("g1", GroupRole.OWNER),
        membership("g2", GroupRole.OWNER),
        membership("g3", GroupRole.MEMBER),
        membership("g4", GroupType.ORG, GroupRole.MANAGER),
    ]

    when: "the hub layout is built"
    def layout = new GroupsMainLayout(permission, userIdTranslator, groupInformationService)

    then: "the header shows the totals"
    overviewHeaderOf(layout).countForTest().text == "4 groups \u00b7 2 owned"
  }

  def "refreshes the counts on every navigation into the hub"() {
    given: "a single membership at first"
    def memberships = [membership("g1", GroupRole.MEMBER)]
    groupInformationService.listMyGroups("u1") >> { String id -> memberships }
    def layout = new GroupsMainLayout(permission, userIdTranslator, groupInformationService)
    assert overviewHeaderOf(layout).countForTest().text == "1 group"

    when: "the membership is gone before the next navigation"
    memberships = []
    def event = new BeforeEnterEvent(router, NavigationTrigger.PROGRAMMATIC, new Location("groups"),
        MyGroupsMain, Mock(UI), List.of())
    layout.beforeEnter(event)

    then: "the header reflects the fresh count"
    overviewHeaderOf(layout).countForTest().text == "No groups"
  }

  def "shows the admin-only navigation entries for a QBiC administrator"() {
    given:
    permission.isAdmin("u1") >> true
    groupInformationService.listMyGroups("u1") >> []

    when:
    def layout = new GroupsMainLayout(permission, userIdTranslator, groupInformationService)

    then:
    navTexts(layout).containsAll(["My Groups", "Organisational groups", "Profile pictures"])
  }

  def "hides the admin navigation entries for a non-administrator"() {
    given:
    permission.isAdmin("u1") >> false
    groupInformationService.listMyGroups("u1") >> []

    when:
    def layout = new GroupsMainLayout(permission, userIdTranslator, groupInformationService)

    then:
    navTexts(layout) == ["My Groups"]
  }

  private static MyGroupMembership membership(String id, GroupRole role) {
    membership(id, GroupType.ADHOC, role)
  }

  private static MyGroupMembership membership(String id, GroupType type, GroupRole role) {
    new MyGroupMembership(id, "Group " + id, null, type, role, 1)
  }

  private static GroupsOverviewHeader overviewHeaderOf(Component root) {
    return descendants(root).find { it instanceof GroupsOverviewHeader } as GroupsOverviewHeader
  }

  private static List<String> navTexts(Component root) {
    List<RouterLink> links = []
    collectLinks(root, links)
    // element.text already carries the concatenated text of the link's children (the label Text);
    // do not recurse or the label is counted twice.
    return links.collect { it.element.text }
  }

  private static void collectLinks(Component component, List<RouterLink> links) {
    if (component instanceof RouterLink link) {
      links << link
    }
    component.children.forEach { child -> collectLinks(child, links) }
  }

  private static List<Component> descendants(Component root) {
    List<Component> result = []
    root.children.forEach { child ->
      result << child
      result.addAll(descendants(child))
    }
    return result
  }

}