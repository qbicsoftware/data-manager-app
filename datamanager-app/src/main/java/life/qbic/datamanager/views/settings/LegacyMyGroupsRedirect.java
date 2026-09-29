package life.qbic.datamanager.views.settings;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import life.qbic.datamanager.views.groups.MyGroupsMain;

/**
 * Forwards the legacy {@code /settings/groups} URL to the canonical top-level Groups route
 * {@code /groups}.
 */
@Route("settings/groups")
@PermitAll
public class LegacyMyGroupsRedirect extends ForwardingView {

  @Override
  protected Class<? extends Component> navigationTarget() {
    return MyGroupsMain.class;
  }
}