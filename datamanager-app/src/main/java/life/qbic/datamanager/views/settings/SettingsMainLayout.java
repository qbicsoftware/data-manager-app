package life.qbic.datamanager.views.settings;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RouterLayout;
import jakarta.annotation.security.PermitAll;
import life.qbic.datamanager.views.UserMainLayout;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.projectmanagement.application.AuthenticationToUserIdTranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * <b> Settings Main Layout </b>
 * <p>
 * The layout hosting all user-specific settings sections. It is nested under
 * {@link UserMainLayout} via {@link ParentLayout}, so the settings routes render inside the
 * single outer shell that provides the global navbar (brand title + top-level
 * "Projects | Groups | Settings" navigation + account menu), the announcement banner and the
 * footer.
 * <p>
 * This layout itself only provides the settings hub: an identity row across the top, and below it
 * the two-column master-detail area - a persistent, non-collapsible aside column on the left and
 * the currently selected settings section in the content area on the right. The identity row is
 * headed by the {@link AccountOverviewHeader} (avatar, name, user name and the "your account
 * settings" hint) and closely mirrors GitHub's settings hub: the identity heads the whole hub
 * rather than sitting inside the navigation column, so a long name has the full shell width. The
 * selected aside tab is kept in sync with the active route via
 * {@link #beforeEnter(BeforeEnterEvent)}.
 */
@PermitAll
@ParentLayout(UserMainLayout.class)
public class SettingsMainLayout extends Div implements RouterLayout, BeforeEnterObserver {

  private final SettingsNavigationComponent settingsNavigationComponent = new SettingsNavigationComponent();
  private final Div contentSlot = new Div();
  private final transient AuthenticationToUserIdTranslationService userIdTranslator;
  private final transient UserInformationService userInformationService;
  private final Div accountOverviewArea = new Div();

  public SettingsMainLayout(@Autowired UserInformationService userInformationService,
      @Autowired AuthenticationToUserIdTranslationService userIdTranslator) {
    this.userInformationService = requireNonNull(userInformationService,
        "userInformationService must not be null");
    this.userIdTranslator = requireNonNull(userIdTranslator,
        "userIdTranslator must not be null");
    addClassName("settings-main-layout");
    accountOverviewArea.addClassName("settings-account-overview-area");
    Div asideArea = new Div(settingsNavigationComponent);
    asideArea.addClassName("settings-aside-area");
    contentSlot.addClassName("settings-content-area");
    // The account overview is its own row above the menu and the content, matching GitHub's
    // settings hub: the identity heads the whole hub instead of being squeezed into the narrow
    // navigation column. The full shell width means a long full name and a user name of up to 20
    // characters fit beside the avatar without wrapping or truncation.
    add(accountOverviewArea, asideArea, contentSlot);
    renderAccountOverview();
  }

  private void renderAccountOverview() {
    accountOverviewArea.removeAll();
    accountOverviewArea.add(new AccountOverviewHeader(loadCurrentUser()));
  }

  /**
   * Reloads the current user's information and re-renders the account overview header in place.
   * Call this after account data shown in the header (e.g. the username) has changed, so the
   * header stays consistent without a full page reload.
   */
  public void refreshAccountOverview() {
    renderAccountOverview();
  }

  private UserInfo loadCurrentUser() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    var userId = userIdTranslator.translateToUserId(authentication).orElseThrow();
    return userInformationService.findById(userId).orElseThrow();
  }

  @Override
  public void showRouterLayoutContent(HasElement content) {
    contentSlot.removeAll();
    contentSlot.getElement().appendChild(content.getElement());
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    settingsNavigationComponent.selectTabFor(event.getNavigationTarget());
  }
}