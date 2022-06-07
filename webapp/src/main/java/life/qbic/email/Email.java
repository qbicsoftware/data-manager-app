package life.qbic.email;

/**
 * A simple representation of an EmailAddress
 *
 * @since <version tag>
 */
public record Email (
  String content,
  String subject,
  String from,
  Recipient to,
  String mimeType) {
}
