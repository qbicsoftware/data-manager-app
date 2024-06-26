package life.qbic.datamanager.views.register;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static life.qbic.logging.service.LoggerFactory.logger;

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
import life.qbic.datamanager.views.MainPage;
import life.qbic.identity.domain.model.User;
import life.qbic.identity.domain.repository.UserRepository;
import life.qbic.logging.api.Logger;
import life.qbic.projectmanagement.application.authorization.QbicOidcUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@Route("register/oidc")
@PermitAll
public class RegisterOpenIdConnect extends AppLayout implements BeforeEnterObserver {

  private final UserRepository userRepository;
  private final TextField username;
  private final TextField fullName;
  private final TextField email;

  private static final Logger log = logger(RegisterOpenIdConnect.class);

  public RegisterOpenIdConnect(
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
    if (authentication.getPrincipal() instanceof QbicOidcUser) {
      return; //nothing to do
    }
    if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
      User user = User.createOidc(fullName, email, username,
          oidcUser.getIssuer().toString(), oidcUser.getName());
      userRepository.addUser(user);
      //TODO move user creation to registration service
      UI.getCurrent().navigate(PleaseConfirmEmailPage.class);
      return;
    }
    UI.getCurrent().navigate(
        Projects.PROJECTS); // no user id loaded as authentication principal is not overwritten.
  }

  @Override
  public void beforeEnter(BeforeEnterEvent event) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication.getPrincipal() instanceof QbicOidcUser qbicOidcUser) {
      log.warn("User %s tried to enter registration although exists.".formatted(
          qbicOidcUser.getQbicUserId()));
      event.rerouteTo(MainPage.class);
      return;
    }
    if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
      //note: for ORCiD, only given name and family name are supported https://orcid.org/.well-known/openid-configuration.
      this.fullName.setValue(buildFullName(oidcUser.getGivenName(), oidcUser.getMiddleName(),
          oidcUser.getFamilyName()));
      Optional.ofNullable(oidcUser.getEmail()).ifPresent(this.email::setValue);
      Optional.ofNullable(oidcUser.getPreferredUsername()).ifPresent(this.username::setValue);
    }
  }

  private static String buildFullName(String givenName, String middleName, String familyName) {
    return "%s%s%s".formatted(
        isNull(givenName) ? "" : givenName,
        isNull(middleName) ? "" : " " + middleName,
        isNull(familyName) ? "" : " " + familyName
    );
  }

}
