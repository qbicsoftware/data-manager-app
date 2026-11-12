package life.qbic.identity.application.communication;

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
    // The second paragraph must keep working for BOTH ways this message is sent:
    //
    //  1. the visitor used "forgot your password" on the login page;
    //  2. somebody submitted the registration form with an address that already has an account
    //     (AUTH-NFR-03 / FEAT-AUTH-FLOWS-03). That case must be indistinguishable from a successful
    //     registration, so the owner — not the requester — is told, by this message.
    //
    // Case 2 is the reason "you can safely ignore this email" is stated explicitly: a person who never
    // used this portal may receive it because a stranger typed their address, and they must not be sent
    // hunting for a password they do not have. It does not weaken case 1, where ignoring it is also the
    // correct action.
    return String.format("""
        Dear %s,

        we have received a request to reset the password for this email address. If you did not
        make this request, you can safely ignore this email — no change will be made. You may also
        contact us at support@qbic.zendesk.com if you are unsure.

        Please click the link below to set your new password:

        %s

        Need help? Contact us for further questions at support@qbic.zendesk.com
        """, fullName, passwordResetLink);
  }

}
