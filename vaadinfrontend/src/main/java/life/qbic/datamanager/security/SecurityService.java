package life.qbic.datamanager.security;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinServletRequest;
import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;
import life.qbic.authentication.domain.user.concept.EmailAddress;
import life.qbic.authentication.domain.user.concept.User;
import life.qbic.authentication.domain.user.repository.UserRepository;
import life.qbic.datamanager.views.AppRoutes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class SecurityService implements Serializable {

  @Serial
  private static final long serialVersionUID = 5199220688136926750L;

  private final UserRepository userRepository;

  @Autowired
  public SecurityService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  private Optional<Authentication> getAuthentication() {
    SecurityContext context = SecurityContextHolder.getContext();
    return Optional.ofNullable(context.getAuthentication())
        .filter(authentication -> !(authentication instanceof AnonymousAuthenticationToken));
  }

  public Optional<User> get() {
    return getAuthentication()
        .flatMap(authentication -> userRepository.findByEmail(
            EmailAddress.from(authentication.getName())));
  }

  public void logout() {
    SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
    logoutHandler.logout(VaadinServletRequest.getCurrent().getHttpServletRequest(), null, null);
  }
}
