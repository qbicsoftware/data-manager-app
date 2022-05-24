package life.qbic.usermanagement.registration;

import life.qbic.email.Email;

/**
 * Creates registration messages.
 */
public class EmailFactory {

  private static class RegistrationEmail implements Email {

    private final String content;
    private final String from;
    private final Recipient to;

    private RegistrationEmail(String from, Recipient recipient) {
      this.from = from;
      this.to = recipient;
      this.content = formatContent(recipient.fullName());
    }

    public static String formatContent(String fullName) {
      return String.format("""
          Dear %s,
                  
          Thank you for registering to QBiCs Data Management Portal.
                  
          Need help? Contact us for further questions at support@qbic.zendesk.com
                  
          Best regards,\s
          The QBiC team""", fullName);
    }

    @Override
    public String content() {
      return content;
    }

    @Override
    public String subject() {
      return "Activate your Data Manager Account";
    }

    @Override
    public String from() {
      return from;
    }

    @Override
    public Recipient to() {
      return to;
    }

    @Override
    public String mimeType() {
      return "text/plain";
    }
  }


  public static Email registrationEmail(String from, Recipient to) {
    return new RegistrationEmail(from, to);
  }

}
