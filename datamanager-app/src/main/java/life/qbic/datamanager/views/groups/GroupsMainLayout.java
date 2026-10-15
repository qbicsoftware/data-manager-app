package life.qbic.datamanager.views.groups;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RouterLayout;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.Objects;
import life.qbic.datamanager.views.UserMainLayout;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import life.qbic.usergroups.api.GroupAdministrationPermission;
import life.qbic.usergroups.api.GroupInformationService;
import life.qbic.usergroups.api.GroupRole;
import life.qbic.usergroups.api.MyGroupMembership;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * <b> Groups Main Layout </b>
 * <p>
 * The layout hosting the top-level Groups area. It is nested under {@link UserMainLayout} via
 * {@link ParentLayout}, so the group routes render inside the single outer shell that provides
 * the global navbar (brand title + top-level "Projects | Groups" navigation + account menu),
 * the announcement banner and the footer.
 * <p>
 * This layout provides the groups hub: a full-width {@link GroupsOverviewHeader} identity row at
 * the top (mirroring the settings hub's account overview), and below it the two-column area - a
 * persistent, non-collapsible aside column with the {@link GroupsNavigationComponent} on the left
 * and the currently selected group route in the content area on the right. The header carries a
 * live membership count that is refreshed on every navigation into the hub. The selected aside tab
 * is kept in sync with the active route via {@link #beforeEnter(BeforeEnterEvent)}.
 * <p>
 * QBiC administrators additionally see the admin-only "Admin" tab leading to the org-group
 * creation page ({@link AdminGroupCreationMain}); the admin gate is resolved through the
 * {@link GroupAdministrationPermission} port when the layout is built.
 */
@PermitAll
@ParentLayout(UserMainLayout.class)
public class GroupsMainLayout extends Div implements RouterLayout, BeforeEnterObserver {

  private final GroupsNavigationComponent groupsNavigationComponent;
  private final Div contentSlot = new Div();
  private final Div overviewArea = new Div();
  private final transient AuthenticationToUserIdTranslationService userIdTranslator;
  private final transient GroupInformationService groupInformationService;

  public GroupsMainLayout(
      @Autowired GroupAdministrationPermission groupAdministrationPermission,
      @Autowired AuthenticationToUserIdTranslationService userIdTranslator,
      @Autowired GroupInformationService groupInformationService) {
    addClassName("groups-main-layout");
    this.userIdTranslator = userIdTranslator;
    this.groupInformationService = groupInformationService;
    this.groupsNavigationComponent = new GroupsNavigationComponent(
        isCurrentUserAdmin(groupAdministrationPermission, userIdTranslator));
    overviewArea.addClassName("groups-overview-area");
    Div asideArea = new Div(groupsNavigationComponent);
    asideArea.addClassName("groups-aside-area");
    contentSlot.addClassName("groups-content-area");
    add(overviewArea, asideArea, contentSlot);
    renderOverview();
  }

  /**
   * Recomputes the caller's membership counts and renders the overview header. Called on layout
   * construction and on every navigation into or within the Groups hub, so the live count stays
   * in sync after a group is created, joined or left.
   */
  private void renderOverview() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    List<MyGroupMembership> memberships =
        userIdTranslator.translateToUserId(authentication)
            .map(groupInformationService::listMyGroups)
            .orElseGet(List::of);
    int totalGroups = memberships.size();
    int ownedGroups = (int) memberships.stream()
        .filter(membership -> membership.myRole() == GroupRole.OWNER)
        .count();
    overviewArea.removeAll();
    overviewArea.add(new GroupsOverviewHeader(totalGroups, ownedGroups));
  }

  private static boolean isCurrentUserAdmin(GroupAdministrationPermission permission,
      AuthenticationToUserIdTranslationService userIdTranslator) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return userIdTranslator.translateToUserId(authentication)
        .map(permission::isAdmin)
        .orElse(false);
  }

  @Override
  public void showRouterLayoutContent(HasElement content) {
    contentSlot.removeAll();
    contentSlot.getElement().appendChild(content.getElement());
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    groupsNavigationComponent.selectTabFor(event.getNavigationTarget());
    // Navigation into (or within) the hub is the reliable signal that membership counts may have
    // changed (create/leave/dissolve all navigate back to a groups route).
    renderOverview();
  }
}