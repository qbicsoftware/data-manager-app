package life.qbic.datamanager.views.settings;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import life.qbic.datamanager.views.account.UserProfileMain;

/**
 * Forwards the legacy {@code /profile} URL to the canonical settings profile route
 * {@code /settings/profile}.
 */
@Route("profile")
@PermitAll
public class LegacyProfileRedirect extends ForwardingView {

  @Override
  protected Class<? extends Component> navigationTarget() {
    return UserProfileMain.class;
  }
}