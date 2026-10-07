package life.qbic.datamanager.views.settings;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import life.qbic.datamanager.views.account.PersonalAccessTokenMain;

/**
 * Forwards the legacy {@code /personal-access-token} URL to the canonical settings route
 * {@code /settings/api-tokens}.
 */
@Route("personal-access-token")
@PermitAll
public class LegacyPersonalAccessTokenRedirect extends ForwardingView {

  @Override
  protected Class<? extends Component> navigationTarget() {
    return PersonalAccessTokenMain.class;
  }
}