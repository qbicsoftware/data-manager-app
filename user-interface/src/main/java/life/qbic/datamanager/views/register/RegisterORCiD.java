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
import life.qbic.datamanager.views.AppRoutes.Projects;
import life.qbic.identity.domain.model.EmailAddress;
import life.qbic.identity.domain.model.EncryptedPassword;
import life.qbic.identity.domain.model.FullName;
import life.qbic.identity.domain.model.User;
import life.qbic.identity.domain.model.UserId;
import life.qbic.identity.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
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
@Route("register/orcid")
@PermitAll
public class RegisterORCiD extends AppLayout implements BeforeEnterObserver {

  private final UserRepository userRepository;
  private final TextField username;
  private final TextField fullName;

  public RegisterORCiD(
      @Autowired UserRepository userRepository
  ) {
    Div content = new Div();
    this.userRepository = requireNonNull(userRepository, "userRepository must not be null");
    fullName = new TextField("Full Name");
    fullName.setRequired(true);
    username = new TextField("Username");
    username.setRequired(true);
    Button submit = new Button("Submit");
    submit.addClickListener(clickedEvent -> createUser(fullName.getValue(), username.getValue()));
    content.add(new FormLayout(fullName, username, submit));
    setContent(content);
  }

  private void createUser(String fullName, String username) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
      EmailAddress emailAddress = EmailAddress.from("test@qbic.uni-tuebingen.de");
      User user = User.create(FullName.from(fullName), emailAddress, username,
          EncryptedPassword.fromEncrypted(""));
      user.setId(UserId.from(oAuth2User.getName()));
      userRepository.addUser(user);
    }
    UI.getCurrent().navigate(Projects.PROJECTS);
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
      String familyName = oAuth2User.getAttribute("family_name");
      String givenName = oAuth2User.getAttribute("given_name");
      this.fullName.setValue(givenName + " " + familyName);
    }
  }
}
