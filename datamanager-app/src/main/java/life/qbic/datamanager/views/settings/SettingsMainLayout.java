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
 * This layout itself only provides the two-column settings hub: a standalone account overview
 * row at the top (spanning the hub width), a persistent, non-collapsible aside column with the
 * {@link SettingsNavigationComponent} on the left and the currently selected settings section in
 * the content area on the right. The selected aside tab is kept in sync with the active route via
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
    // grid rows: [account overview] / [aside | content] — the overview is a standalone
    // full-hub-width row above the menu+content, not nested in the aside.
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