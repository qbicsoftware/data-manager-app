package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import life.qbic.datamanager.security.LogoutService;
import life.qbic.datamanager.views.account.PersonalAccessTokenMain;
import life.qbic.datamanager.views.account.ProfileSettingsMain;
import life.qbic.datamanager.views.projects.overview.ProjectOverviewMain;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Data Manager Menu
 * <p>
 * Menubar within the data manager application with which the user can route to the
 * {@link ProjectOverviewMain}, logout and access his personal access tokens in the
 * {@link PersonalAccessTokenMain}
 */
public class DataManagerMenu extends Div {

  MenuBar projectMenu = new MenuBar();
  Avatar userAvatar = new Avatar();
  private final UserInformationService userInformationService;
  private final LogoutService logoutService;

  public DataManagerMenu(@Autowired UserInformationService userInformationService,
      @Autowired LogoutService logoutService) {
    this.userInformationService = userInformationService;
    this.logoutService = logoutService;
    initializeHomeMenuItem();
    initializeUserSubMenuItems();
    add(projectMenu);
    projectMenu.addClassName("menubar");
    addClassName("data-manager-menu");
    projectMenu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
  }

  private void initializeHomeMenuItem() {
    projectMenu.addItem(new Button("Home"), event -> routeTo(ProjectOverviewMain.class));
  }

  private void initializeUserSubMenuItems() {
    initializeAvatar();
    MenuItem userMenuItem = projectMenu.addItem(userAvatar);
    SubMenu userSubMenu = userMenuItem.getSubMenu();
    userSubMenu.addItem("Your Profile", event -> routeTo(ProfileSettingsMain.class));
    userSubMenu.add(new Hr());
    userSubMenu.addItem("Personal Access Tokens (PAT)", event -> routeTo(
        PersonalAccessTokenMain.class));
    userSubMenu.addItem("Log Out", event -> logoutService.logout());
  }

  private Avatar initializeAvatar() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Avatar avatar = new Avatar();
    avatar.addClassName("user-avatar");
    if (authentication.getPrincipal() instanceof QbicUserDetails qbicUserDetails) {
      UserInfo userInfo = userInformationService.findById(qbicUserDetails.getUserId())
          .orElseThrow();
      avatar.setName(userInfo.fullName());
    }
    //for images -> https://barro.github.io/2018/02/avatars-identicons-and-hash-visualization/#github-identicon
    return avatar;
  }

  private <T extends Component> void routeTo(Class<T> mainComponent) {
    getUI().orElseThrow().navigate(mainComponent);
  }
}
