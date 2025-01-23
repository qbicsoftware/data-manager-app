package life.qbic.datamanager.views.demo;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Profile("test-ui") // This view will only be available when the "test-ui" profile is active
@Route("login2")
@PermitAll
@UIScope
@Component
public class AuthTest extends Div implements BeforeEnterObserver {

  @Autowired
  private OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

  @Autowired
  public AuthTest(ApplicationContext app) {
    Button button = new Button("Authorize Zenodo");
    button.addClickListener(e -> {
      HttpServletRequest request = ((VaadinServletRequest) VaadinRequest.getCurrent()).getHttpServletRequest();
      saveOriginalRoute(request); //
      UI.getCurrent().getPage().setLocation("/dev/oauth2/authorization/zenodo");
    });
    add(button);
  }

  private void saveOriginalRoute(HttpServletRequest request) {
    String currentRoute = UI.getCurrent().getInternals().getActiveViewLocation().getPathWithQueryParameters();
    request.getSession().setAttribute("datamanager.originalRoute", currentRoute);
  }


  @Override
  public void beforeEnter(BeforeEnterEvent event) {

    var auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null || !auth.isAuthenticated()) {
      throw new IllegalStateException("Authentication required");
    }

    if (auth instanceof Jwt jwt) {
      add(new Div("JWT available: " + jwt.getTokenValue()));
    }
  }
}
