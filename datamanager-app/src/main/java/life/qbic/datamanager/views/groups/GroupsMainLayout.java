package life.qbic.datamanager.views.groups;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RouterLayout;
import jakarta.annotation.security.PermitAll;
import java.util.Objects;
import life.qbic.datamanager.views.UserMainLayout;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import life.qbic.usergroups.api.GroupAdministrationPermission;
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
 * This layout itself only provides the two-column groups hub: a persistent, non-collapsible
 * aside column with the {@link GroupsNavigationComponent} on the left and the currently selected
 * group route in the content area on the right. The selected aside tab is kept in sync with the
 * active route via {@link #beforeEnter(BeforeEnterEvent)}.
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

  public GroupsMainLayout(
      @Autowired GroupAdministrationPermission groupAdministrationPermission,
      @Autowired AuthenticationToUserIdTranslationService userIdTranslator) {
    addClassName("groups-main-layout");
    this.groupsNavigationComponent = new GroupsNavigationComponent(
        isCurrentUserAdmin(groupAdministrationPermission, userIdTranslator));
    Div asideArea = new Div(groupsNavigationComponent);
    asideArea.addClassName("groups-aside-area");
    contentSlot.addClassName("groups-content-area");
    add(asideArea, contentSlot);
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
  }
}