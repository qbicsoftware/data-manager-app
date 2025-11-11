package life.qbic.datamanager.views.general.utils;

import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import java.util.Optional;
import life.qbic.datamanager.views.general.contact.Contact;
import life.qbic.projectmanagement.application.authorization.QbicOidcUser;
import life.qbic.projectmanagement.application.authorization.QbicUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * <b>Utility - Tools for the app</b>
 *
 * <p>Some tools that can be reused in the frontend.</p>
 *
 * @since 1.6.0
 */
public class Utility {

  public static void addConsumedLengthHelper(TextField textField) {
    int maxLength = textField.getMaxLength();
    int consumedLength = textField.getValue().length();
    textField.setHelperText(consumedLength + "/" + maxLength);
  }

  public static void addConsumedLengthHelper(TextArea textArea) {
    int maxLength = textArea.getMaxLength();
    int consumedLength = textArea.getValue().length();
    textArea.setHelperText(consumedLength + "/" + maxLength);
  }

  public static Optional<Contact> tryToLoadFromPrincipal() {
    var principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    String fullName;
    String emailAddress;
    String oidc;
    String oidcIssuer;
    if (principal instanceof QbicUserDetails qbicUserDetails) {
      fullName = qbicUserDetails.fullName();
      emailAddress = qbicUserDetails.getEmailAddress();
      oidc = qbicUserDetails.oidc();
      oidcIssuer = qbicUserDetails.oidcIssuer();
    } else if (principal instanceof QbicOidcUser qbicOidcUser) {
      fullName = qbicOidcUser.getFullName();
      emailAddress = qbicOidcUser.getEmail();
      oidc = qbicOidcUser.getOidcId();
      oidcIssuer = qbicOidcUser.getOidcIssuer();
    } else {
      return Optional.empty();
    }
    return Optional.of(new Contact(fullName, emailAddress, oidc, oidcIssuer));
  }
}
