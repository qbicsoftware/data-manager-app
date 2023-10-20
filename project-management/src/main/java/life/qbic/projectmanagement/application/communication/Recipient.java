package life.qbic.projectmanagement.application.communication;

/**
 * A recipient of an mail. Provides an mail address and a full name.
 */
public record Recipient(
    String address,
    String fullName) {

}
