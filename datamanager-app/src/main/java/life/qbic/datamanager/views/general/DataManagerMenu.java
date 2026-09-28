package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.spring.security.AuthenticationContext;
import java.util.Objects;
import life.qbic.datamanager.views.account.UserAvatar;
import life.qbic.projectmanagement.application.authorization.QbicOidcUser;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Data Manager Menu
 * <p>
 * Menubar within the data manager application that hosts the logged-in user's account actions:
 * the user avatar with logout. Global navigation (Projects | Groups | Settings tabs, back to the
 * project overview) is provided by the top-level navigation and homepage link in the layouts;
 * the avatar is now reserved for account-scoped actions only.
 */
public class DataManagerMenu extends Div {

  MenuBar projectMenu = new MenuBar();
  UserAvatar userAvatar = new UserAvatar();

  public DataManagerMenu(AuthenticationContext authenticationContext) {
    initializeUserSubMenuItems(Objects.requireNonNull(authenticationContext));
    add(projectMenu);
    projectMenu.addClassName("menubar");
    addClassName("data-manager-menu");
    projectMenu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
  }

  private void initializeUserSubMenuItems(AuthenticationContext authenticationContext) {
    initializeAvatar();
    MenuItem userMenuItem = projectMenu.addItem(userAvatar);
    SubMenu userSubMenu = userMenuItem.getSubMenu();
    userSubMenu.addItem("Log Out", event -> authenticationContext.logout());
  }

  private void initializeAvatar() {
    var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    var userId = "";
    if (principal instanceof QbicUserDetails qbicUserDetails) {
      userId = qbicUserDetails.getUserId();
    }
    if (principal instanceof QbicOidcUser qbicOidcUser) {
      userId = qbicOidcUser.getQbicUserId();
    }
    userAvatar.setUserId(userId);
  }
}
