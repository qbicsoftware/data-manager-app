package life.qbic.datamanager.views.demo;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Profile("development") // This view will only be available when the "development" profile is active
@Route("test-integration")
@PermitAll
public class InvenioIntegrationDemo extends Div implements BeforeEnterObserver {

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth.isAuthenticated()) {
      var principal = auth.getPrincipal();
      System.out.println(principal);
      return;
    }
    Div div = new Div("Not authenticated yet!");
    add(div);
  }

  public InvenioIntegrationDemo() {
    var button = new Button("Integrate Invenio");
    button.addClickListener(e -> {
      UI.getCurrent().getPage().setLocation("/dev/oauth2/authorization/invenio");
    });
    add(button);
  }

}
