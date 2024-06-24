package life.qbic.datamanager.views.register;

import static java.util.Objects.requireNonNull;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.Optional;
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.identity.domain.model.User;
import life.qbic.identity.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * TODO!
 * <b>short description</b>
 *
 * <p>detailed description</p>
 *
 * @since <version tag>
 */
@Route("register/orcid")
@PermitAll
public class RegisterORCiD extends AppLayout implements BeforeEnterObserver {

  private final UserRepository userRepository;
  private final TextField username;
  private final TextField fullName;
  private final TextField email;

  public RegisterORCiD(
      @Autowired UserRepository userRepository
  ) {
    Div content = new Div();
    this.userRepository = requireNonNull(userRepository, "userRepository must not be null");
    fullName = new TextField("Full Name");
    fullName.setRequired(true);
    username = new TextField("Username");
    username.setRequired(true);
    email = new TextField("Email");
    email.setRequired(true);
    Button submit = new Button("Submit");
    submit.addClickListener(
        clickedEvent -> createUser(fullName.getValue(), username.getValue(), email.getValue()));
    content.add(new FormLayout(fullName, username, email, submit));
    setContent(content);
  }

  private void createUser(String fullName, String username, String email) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
      User user = User.createOidc(fullName, email, username,
          oidcUser.getIssuer().toString(), oidcUser.getName());
      userRepository.addUser(user);
    }
    UI.getCurrent().navigate(Projects.PROJECTS);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
      String familyName = oidcUser.getAttribute("family_name");
      String givenName = oidcUser.getAttribute("given_name");
      String email = oidcUser.getEmail();
      this.fullName.setValue(givenName + " " + familyName);
      this.username.setValue(Optional.ofNullable(oidcUser.getPreferredUsername())
          .orElse(this.username.getEmptyValue()));
      this.email.setValue(Optional.ofNullable(email).orElse(this.email.getEmptyValue()));

    }
  }
}
