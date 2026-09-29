package life.qbic.datamanager.views.settings;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import life.qbic.datamanager.views.groups.NewGroupMain;

/**
 * Forwards the legacy {@code /settings/groups/new} URL to the canonical top-level Groups route
 * {@code /groups/new}.
 */
@Route("settings/groups/new")
@PermitAll
public class LegacyNewGroupRedirect extends ForwardingView {

  @Override
  protected Class<? extends Component> navigationTarget() {
    return NewGroupMain.class;
  }
}