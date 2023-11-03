package life.qbic.projectmanagement.application.communication;

/**
 * <b>MailAttachment</b>
 *
 * <p>Encapsulates information about an attachment, e.g. a file added to an email</p>
 *
 * @since 1.0.0
 */
public record Attachment(String content, String name) {

}
