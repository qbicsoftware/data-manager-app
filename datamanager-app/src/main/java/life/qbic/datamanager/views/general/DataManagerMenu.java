package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility.IconSize;
import java.util.Objects;
import life.qbic.datamanager.views.account.UserAvatar;
import life.qbic.datamanager.views.account.UserProfileMain;
import life.qbic.projectmanagement.application.authorization.QbicOidcUser;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Data Manager Menu
 * <p>
 * Menubar within the data manager application that hosts the logged-in user's account actions:
 * the user avatar with the account Settings shortcut and logout. Projects | Groups | Settings are
 * also top-level navigation tabs; the avatar menu additionally keeps Settings for users
 * accustomed to reaching account settings from the avatar (GitHub-style), while collaborative
 * surfaces (My Groups) stay in their top-level hub.
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
    // Account settings stay reachable from the avatar for discoverability/familiarity
    // (GitHub keeps Settings in the user menu too), with the top-level Settings tab as the
    // primary entry. Each item shows an icon plus its semantic label.
    userSubMenu.addItem(settingsItem(),
        event -> getUI().ifPresent(ui -> ui.navigate(UserProfileMain.class)));
    userSubMenu.addItem(logoutItem(), event -> authenticationContext.logout());
  }

  private static Span settingsItem() {
    Icon icon = new Icon(VaadinIcon.COG);
    Span label = new Span("Settings");
    return withIcon(icon, label);
  }

  private static Span logoutItem() {
    Icon icon = new Icon(VaadinIcon.SIGN_OUT);
    Span label = new Span("Log Out");
    return withIcon(icon, label);
  }

  private static Span withIcon(Icon icon, Span label) {
    icon.addClassName(IconSize.SMALL);
    Span item = new Span(icon, label);
    item.addClassName("user-menu-item");
    item.getStyle().set("display", "inline-flex");
    item.getStyle().set("align-items", "center");
    item.getStyle().set("gap", "var(--spacing-02)");
    return item;
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
