package life.qbic.datamanager.views.settings;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import life.qbic.datamanager.views.groups.GroupDetailMain;

/**
 * Forwards the legacy {@code /settings/groups/:groupId} URL to the canonical top-level Groups
 * route {@code /groups/:groupId}, preserving the {@code groupId} path parameter so bookmarked
 * group-detail links keep working.
 */
@Route("settings/groups/:groupId")
@PermitAll
public class LegacyGroupDetailRedirect extends Div implements BeforeEnterObserver {

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    event.forwardTo(GroupDetailMain.class, event.getRouteParameters());
  }
}