package life.qbic.email;

/**
 * A recipient of an email. Provides an email address and a full name.
 */
public record Recipient(
    String address,
    String fullName) {
}
