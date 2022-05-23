package life.qbic.usermanagement.registration;

/**
 * Creates registration messages.
 */
public class RegistrationMessageFactory {

  public static String registrationMessage(String fullName) {
    return String.format("""
        Dear %s,
                
        Thank you for registering to QBiCs Data Management Portal.
                
        Need help? Contact us for further questions at support@qbic.zendesk.com
                
        Best regards,\s
        The QBiC team""", fullName);
  }

}
