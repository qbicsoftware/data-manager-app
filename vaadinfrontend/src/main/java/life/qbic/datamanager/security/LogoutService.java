package life.qbic.datamanager.security;

import com.vaadin.flow.server.VaadinServletRequest;
import java.io.Serial;
import java.io.Serializable;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class LogoutService implements Serializable {

  @Serial
  private static final long serialVersionUID = 5199220688136926750L;

  public void logout() {
    SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
    logoutHandler.logout(VaadinServletRequest.getCurrent().getHttpServletRequest(), null, null);
  }
}
