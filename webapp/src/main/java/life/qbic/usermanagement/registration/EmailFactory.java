package life.qbic.usermanagement.registration;

import life.qbic.email.Email;
import life.qbic.email.Recipient;

/**
 * Creates registration messages.
 */
public class EmailFactory {

  private static String formatRegistrationEmailContent(String fullName) {
    return String.format("""
        Dear %s,
                
        Thank you for registering to QBiCs Data Management Portal.
                
        Need help? Contact us for further questions at support@qbic.zendesk.com
                
        Best regards,\s
        The QBiC team""", fullName);
  }


  public static Email registrationEmail(String from, Recipient to) {
    String content = formatRegistrationEmailContent(to.fullName());
    String subject = "Activate your Data Manager Account";
    String mimeType = "text/plain";
    return new Email(content, subject, from, to, mimeType);
  }

}
