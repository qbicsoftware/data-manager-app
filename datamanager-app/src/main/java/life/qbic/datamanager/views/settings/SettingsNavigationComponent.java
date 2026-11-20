package life.qbic.datamanager.views.settings;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import java.util.HashMap;
import java.util.Map;
import life.qbic.datamanager.views.account.ExternalProvidersMain;
import life.qbic.datamanager.views.account.PersonalAccessTokenMain;
import life.qbic.datamanager.views.account.TwoFactorAuthenticationMain;
import life.qbic.datamanager.views.account.UserProfileMain;
import life.qbic.identity.application.mfa.MfaPolicy;
import life.qbic.identity.domain.model.mfa.MfaPolicyMode;
import life.qbic.identity.domain.model.mfa.SecondFactorRemoval;

/**
 * Settings Navigation Component
 * <p>
 * Vertical navigation menu for the settings hub. Each entry is a real {@link RouterLink} to a
 * settings section, so sections remain deep-linkable and the current section can be highlighted
 * based on the active route. Additional settings categories can be added by registering further
 * tabs via {@link #addTab(String, VaadinIcon, Class)}.
 * <p>
 * <b>The two-factor entry is conditional.</b> Under {@link MfaPolicyMode#OFF} nothing is asked at
 * sign-in, so an account could enrol an app and never be challenged for it: the entry is not rendered
 * at all, and {@link TwoFactorAuthenticationMain} refuses the route for the same reason. An entry that
 * leads to "you cannot use this" is worse than no entry.
 */
public class SettingsNavigationComponent extends Div {

  private final Tabs tabs = new Tabs();
  private final Map<Class<?>, Tab> tabsByNavigationTarget = new HashMap<>();

  /**
   * @param mfaPolicy the deployment posture, which decides whether the two-factor entry exists
   */
  public SettingsNavigationComponent(MfaPolicy mfaPolicy) {
    addClassName("settings-navigation-component");
    tabs.addClassName("settings-navigation-tabs");
    tabs.getElement().setAttribute("orientation", "vertical");
    addTab("Profile", VaadinIcon.USER, UserProfileMain.class);
    if (SecondFactorRemoval.twoFactorCanBeEnabled(mfaPolicy.mode())) {
      addTab("Two-factor", VaadinIcon.SHIELD, TwoFactorAuthenticationMain.class);
    }
    addTab("API Tokens", VaadinIcon.KEY, PersonalAccessTokenMain.class);
    addTab("External Providers", VaadinIcon.DATABASE, ExternalProvidersMain.class);
    add(tabs);
  }

  private void addTab(String label, VaadinIcon icon, Class<? extends Component> navigationTarget) {
    RouterLink link = new RouterLink();
    link.add(new Icon(icon), new Text(label));
    link.setRoute(navigationTarget);
    Tab tab = new Tab(link);
    tab.addClassName("settings-nav-tab");
    tabs.add(tab);
    tabsByNavigationTarget.put(navigationTarget, tab);
  }

  /**
   * Marks the tab matching the given navigation target as the selected one.
   *
   * @param navigationTarget the class of the currently displayed route target
   */
  public void selectTabFor(Class<?> navigationTarget) {
    Tab tab = tabsByNavigationTarget.get(navigationTarget);
    if (tab != null) {
      tabs.setSelectedTab(tab);
    }
  }
}