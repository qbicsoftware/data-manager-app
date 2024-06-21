package life.qbic.datamanager.views.general;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import java.util.Objects;
import life.qbic.datamanager.security.LogoutService;
import life.qbic.datamanager.views.account.PersonalAccessTokenMain;
import life.qbic.datamanager.views.account.UserAvatar;
import life.qbic.datamanager.views.account.UserProfileMain;
import life.qbic.datamanager.views.projects.overview.ProjectOverviewMain;
import life.qbic.identity.api.UserInformationService;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Data Manager Menu
 * <p>
 * Menubar within the data manager application with which the user can route to the
 * {@link ProjectOverviewMain}, logout and access his personal access tokens in the
 * {@link PersonalAccessTokenMain}
 */
public class DataManagerMenu extends Div {

  private final transient LogoutService logoutService;
  private final transient UserInformationService userInformationService;
  MenuBar projectMenu = new MenuBar();
  UserAvatar userAvatar = new UserAvatar();

  public DataManagerMenu(@Autowired LogoutService logoutService,
      @Autowired UserInformationService userInformationService) {
    this.logoutService = Objects.requireNonNull(logoutService);
    this.userInformationService = Objects.requireNonNull(userInformationService);
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
    userSubMenu.addItem("Personal Access Tokens (PAT)", event -> routeTo(
        PersonalAccessTokenMain.class));
    userSubMenu.addItem("User Profile", event -> routeTo(UserProfileMain.class));
    userSubMenu.addItem("Log Out", event -> logoutService.logout());
  }

  private void initializeAvatar() {
    var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    var userId = "";
    if (principal instanceof QbicUserDetails qbicUserDetails) {
      userId = qbicUserDetails.getUserId();
    }
    if (principal instanceof OAuth2User oAuth2User) {
      userId = oAuth2User.getName();
    }
    /*Since users can change their detailsInformation, the variable information in the user session may not be up to date,
      which is why a we retrieve the current state from the database */
//    var userInfo = userInformationService.findById(userId).orElseThrow();
    userAvatar.setUserId(userId);
  }

  private <T extends Component> void routeTo(Class<T> mainComponent) {
    getUI().orElseThrow().navigate(mainComponent);
  }
}
