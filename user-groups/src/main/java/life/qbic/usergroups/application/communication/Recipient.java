package life.qbic.usergroups.application.communication;

/**
 * <b>Recipient</b>
 *
 * <p>A recipient of an email sent by the user groups context, providing the mail address and a
 * full name for the salutation.</p>
 *
 * @since 1.20.0
 */
public record Recipient(String address, String fullName) {

}