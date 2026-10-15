package life.qbic.datamanager.views.general.utils;

import com.vaadin.flow.component.Component;
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

  /**
   * Makes a plain element operable by pointer and by Enter/Space, so every clickable surface in
   * the app is keyboard-operable the same way. The caller owns the cursor and hover styling.
   *
   * @param element the element to make interactive
   * @param role    the ARIA role to expose, e.g. {@code "button"} for an action and {@code "link"}
   *                for a navigation target
   * @param action  what activation does
   */
  public static void activateOnClickAndKeys(Component element, String role, Runnable action) {
    element.getElement().setAttribute("role", role);
    element.getElement().setAttribute("tabindex", "0");
    element.getElement().addEventListener("click", event -> action.run());
    element.getElement().addEventListener("keydown", event -> {
      String key = event.getEventData().path("event.key").asText("");
      if ("Enter".equals(key) || " ".equals(key)) {
        action.run();
      }
    }).addEventData("event.key");
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
