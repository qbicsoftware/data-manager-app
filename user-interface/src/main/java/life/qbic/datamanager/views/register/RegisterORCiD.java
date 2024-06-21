package life.qbic.datamanager.views.register;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.router.Route;
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
@Route("/register/orcid")
public class RegisterORCiD extends AppLayout {

  public RegisterORCiD(
      @Autowired UserRepository userRepository
  ) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication.getPrincipal() instanceof OAuth2User oAuth2User) {
      FullName fullName = FullName.from("fullName");
      EmailAddress emailAddress = EmailAddress.from("test@qbic.uni-tuebingen.de");
      String s = "userName";
      User user = User.create(fullName, emailAddress, s, EncryptedPassword.fromEncrypted(""));
      user.setId(UserId.from(oAuth2User.getName()));
      userRepository.addUser(user);

    }
  }
}
