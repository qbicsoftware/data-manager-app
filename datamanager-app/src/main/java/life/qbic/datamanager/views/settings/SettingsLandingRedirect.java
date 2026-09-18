package life.qbic.datamanager.views.settings;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import life.qbic.datamanager.views.account.UserProfileMain;

/**
 * Entry point of the settings hub. Forwards to the default settings section (Profile), so that
 * {@code /settings} behaves like {@code /settings/profile} while keeping one canonical URL per
 * section.
 */
@Route("settings")
@PermitAll
public class SettingsLandingRedirect extends ForwardingView {

  @Override
  protected Class<? extends Component> navigationTarget() {
    return UserProfileMain.class;
  }
}