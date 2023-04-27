package life.qbic.authentication.application.communication;

import life.qbic.domain.concepts.communication.Email;
import life.qbic.domain.concepts.communication.Recipient;

/**
 * Creates registration messages.
 */
public class EmailFactory {

  private EmailFactory() {
  }

  private static String formatRegistrationEmailContent(String fullName, String confirmationLink) {
    return String.format("""
        Dear %s,
                
        Thank you for registering to QBiCs Data Management Portal.
        
        Please click the link below to confirm your registration:
        
        %s
                
        Need help? Contact us for further questions at support@qbic.zendesk.com
                
        Best regards,\s
        The QBiC team""", fullName, confirmationLink);
  }

  private static String formatPasswordResetEmailContent(String fullName, String passwordResetLink) {
    return String.format("""
        Dear %s,
                
        we have received a password reset request. If you did not issue the reset, 
        please contact us asap at support@qbic.zendesk.com.
        
        Please click the link below to set your new password:
        
        %s
                
        Need help? Contact us for further questions at support@qbic.zendesk.com
                
        Best regards,\s
        The QBiC team""", fullName, passwordResetLink);
  }

  public static Email registrationEmail(String from, Recipient to, String confirmationLink) {
    String content = formatRegistrationEmailContent(to.fullName(), confirmationLink);
    String subject = "Activate your Data Manager account";
    String mimeType = "text/plain";
    return new Email(content, subject, from, to, mimeType);
  }

  public static Email passwordResetEmail(String from, Recipient to, String passwordResetLink) {
    var content = formatPasswordResetEmailContent(to.fullName(), passwordResetLink);
    var subject = "Reset your Data Manager account password";
    var mimeType = "text/plain";
    return new Email(content, subject, from, to, mimeType);
  }

}
