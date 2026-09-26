package life.qbic.usergroups.application.communication;

/**
 * <b>Email service</b>
 *
 * <p>Port of the user groups context to send notification emails. Decouples the application layer
 * from the concrete mail-sending infrastructure, mirroring the {@code EmailService} ports of the
 * identity and project management contexts.</p>
 *
 * @since 1.20.0
 */
public interface EmailService {

  void send(Subject subject, Recipient recipient, Content content)
      throws CommunicationException;

}