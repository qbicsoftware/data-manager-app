package life.qbic.domain.concepts.communication;

public interface EmailService {

  void send(Email email);

  void send(String recipient, String recipientFullName, String subject, String message);
}
