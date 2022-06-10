package life.qbic.email;

/**
 * A recipient of an email. Provides an value and a full name.
 */
public record Recipient(
    String address,
    String fullName) {
}
