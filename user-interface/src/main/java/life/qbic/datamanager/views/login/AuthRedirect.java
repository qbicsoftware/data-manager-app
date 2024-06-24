package life.qbic.datamanager.views.login;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.datamanager.views.register.RegisterORCiD;
import life.qbic.identity.domain.model.UserId;
import life.qbic.identity.domain.repository.UserRepository;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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

  private final UserRepository userRepository;

  public AuthRedirect(@Autowired UserRepository userRepository) {
    this.userRepository = requireNonNull(userRepository, "userRepository must not be null");
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    var attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
    var session = attributes.getRequest().getSession();
    if (principal instanceof QbicUserDetails qbicUserDetails) {
      event.forwardTo(Projects.PROJECTS);
    } else if (principal instanceof OAuth2User oAuth2User) {
      userRepository.findById(UserId.from(oAuth2User.getName()))
          .ifPresentOrElse(
              found -> event.forwardTo(Projects.PROJECTS),
              () -> event.forwardTo(RegisterORCiD.class));
    } else {
      event.rerouteToError(NotFoundException.class);
    }
  }
}
