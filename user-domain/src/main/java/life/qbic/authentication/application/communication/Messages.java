package life.qbic.authentication.application.communication;

/**
 * Creates registration messages.
 */
public class Messages {

  private Messages() {
  }

  public static String formatRegistrationEmailContent(String fullName, String confirmationLink) {
    return String.format("""
        Dear %s,
                
        Thank you for registering to QBiCs Data Management Portal.
                
        Please click the link below to confirm your registration:
                
        %s
                
        Need help? Contact us for further questions at support@qbic.zendesk.com
        """, fullName, confirmationLink);
  }

  public static String formatPasswordResetEmailContent(String fullName, String passwordResetLink) {
    return String.format("""
        Dear %s,
                
        we have received a password reset request. If you did not issue the reset, 
        please contact us asap at support@qbic.zendesk.com.
                
        Please click the link below to set your new password:
                
        %s
                
        Need help? Contact us for further questions at support@qbic.zendesk.com
        """, fullName, passwordResetLink);
  }

}
