package life.qbic.datamanager.views.demo;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import jakarta.annotation.security.PermitAll;
import java.util.Objects;
import life.qbic.datamanager.views.UiHandle;
import org.springframework.beans.factory.annotation.Autowired;
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

  private final InvenioDemoService service;

  private final UiHandle uiHandle =  new UiHandle();

  @Autowired
  public InvenioIntegrationDemo(InvenioDemoService invenioDemoService) {
    this.service = Objects.requireNonNull(invenioDemoService);
    var button = new Button("Integrate Invenio");
    button.addClickListener(e -> {
      String ctx = VaadinService.getCurrentRequest().getContextPath(); // e.g. "" or "/dev"
      UI.getCurrent().getPage().setLocation(ctx + "/oauth2/authorization/invenio");
      button.setEnabled(false); // guard against double-click (can cause code reuse -> invalid_grant)
    });
    add(button);
    uiHandle.bind(UI.getCurrent());
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth.isAuthenticated()) {
      var principal = auth.getPrincipal();
      System.out.println(principal);
      service.listDepositions()
          .subscribe(depositions ->
                  uiHandle.onUiAndPush(() -> add(new Div(depositions.toString()))),
              Throwable::printStackTrace);
    }
  }
}
