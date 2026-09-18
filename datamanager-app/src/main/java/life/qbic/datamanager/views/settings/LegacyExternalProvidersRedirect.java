package life.qbic.datamanager.views.settings;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import life.qbic.datamanager.views.account.ExternalProvidersMain;

/**
 * Forwards the legacy {@code /external-providers} URL to the canonical settings route
 * {@code /settings/external-providers}.
 */
@Route("external-providers")
@PermitAll
public class LegacyExternalProvidersRedirect extends ForwardingView {

  @Override
  protected Class<? extends Component> navigationTarget() {
    return ExternalProvidersMain.class;
  }
}