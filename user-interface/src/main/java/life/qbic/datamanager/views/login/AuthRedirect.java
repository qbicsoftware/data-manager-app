package life.qbic.datamanager.views.login;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Route("auth")
@PermitAll
public class AuthRedirect extends Div implements BeforeEnterObserver {


  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal instanceof QbicUserDetails qbicUserDetails) {
      event.forwardTo(Projects.PROJECTS);
    } else if (principal instanceof OAuth2User oAuth2User) {
      event.forwardTo("/register/orcid");
    } else {
      event.rerouteToError(NotFoundException.class);
    }
  }
}
