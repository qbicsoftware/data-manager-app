package life.qbic.datamanager.views.demo;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.annotation.UIScope;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Profile("test-ui") // This view will only be available when the "test-ui" profile is active
@Route("login2")
@AnonymousAllowed
@UIScope
@Component
public class AuthTest extends Div implements BeforeEnterObserver {

  @Autowired
  private OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

  public AuthTest(@Autowired ApplicationContext app) {
    Button button = new Button("Authorize Zenodo");
    button.addClickListener(e -> {
      String authorizationUrl = ServletUriComponentsBuilder
          .fromCurrentContextPath()
          .path("/oauth2/authorization/zenodo")
          .toUriString();
      UI.getCurrent().getPage().setLocation(authorizationUrl);
    });

    add(button);
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
    if (auth instanceof OAuth2AuthenticationToken token) {
      add(new Div("Found OAuth2AuthenticationToken: " + token));
      OAuth2AuthorizeRequest authRequest = OAuth2AuthorizeRequest
          .withClientRegistrationId(token.getAuthorizedClientRegistrationId())
          .principal(token)
          .build();
      OAuth2AuthorizedClient client = oAuth2AuthorizedClientManager.authorize(authRequest);
      OAuth2AccessToken accessToken = client.getAccessToken();
    }
    else {
      add(new Div("Not a JWT"));
    }
  }
}
