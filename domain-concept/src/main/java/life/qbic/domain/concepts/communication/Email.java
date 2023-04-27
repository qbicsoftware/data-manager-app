package life.qbic.domain.concepts.communication;

/**
 * A simple representation of an Email
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
