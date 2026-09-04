package life.qbic.infrastructure.email

import jakarta.mail.Multipart
import jakarta.mail.Session
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import org.springframework.mail.javamail.JavaMailSender
import spock.lang.Specification

/**
 * Verifies that {@link EmailServiceProvider} builds and submits e-mail messages
 * with the same observable behaviour after switching from a custom
 * {@code Transport}-based {@code MailServerConfiguration} to Spring's
 * {@link JavaMailSender}.
 *
 * <p>The mock {@link JavaMailSender} simulates the real sender's behaviour of
 * finalising the message ({@code saveChanges()}) so the resulting
 * {@link MimeMessage} can be inspected the same way it would be after an
 * actual submission.</p>
 */
class EmailServiceProviderSpec extends Specification {

    def "sends a plain text message with the expected subject, recipient, sender and content"() {
        given:
        def mailSender = Mock(JavaMailSender)
        MimeMessage created = new MimeMessage((Session) null)
        mailSender.createMimeMessage() >> created
        mailSender.send(_) >> { MimeMessage m -> m.saveChanges() }

        def provider = new EmailServiceProvider(mailSender)

        when:
        provider.send(
                new Subject("Test subject"),
                new Recipient("Jane Doe", "jane.doe@example.com"),
                new Content("Hello there"))

        then:
        1 * mailSender.send(created)
        created.subject == "Test subject"
        created.getRecipients(MimeMessage.RecipientType.TO).size() == 1
        created.getRecipients(MimeMessage.RecipientType.TO)[0].address == "jane.doe@example.com"
        created.from[0].address == "no-reply@qbic.uni-tuebingen.de"
        created.contentType.startsWith("text/plain")
        def body = created.content as String
        body.contains("Hello there")
        body.contains("With kind regards")
        body.contains("Your QBiC team")
    }

    def "sends a message with an attachment as a multipart with the expected parts"() {
        given:
        def mailSender = Mock(JavaMailSender)
        MimeMessage created = new MimeMessage((Session) null)
        mailSender.createMimeMessage() >> created
        mailSender.send(_) >> { MimeMessage m -> m.saveChanges() }

        def provider = new EmailServiceProvider(mailSender)

        when:
        provider.send(
                new Subject("Test subject"),
                new Recipient("Jane Doe", "jane.doe@example.com"),
                new Content("Hello there"),
                new Attachment("report.txt", "report payload"))

        then:
        1 * mailSender.send(created)
        created.content instanceof Multipart

        def multipart = created.content as Multipart
        multipart.count == 2
        def textPart = multipart.getBodyPart(0)
        def attachmentPart = multipart.getBodyPart(1)

        def textBody = textPart.content as String
        textBody.contains("Hello there")
        textBody.contains("With kind regards")
        textBody.contains("Your QBiC team")
        attachmentPart.fileName == "report.txt"
        attachmentPart.dataHandler.contentType == "application/octet-stream"
        attachmentContent(attachmentPart) == "report payload"
    }

    private static String attachmentContent(MimeBodyPart part) {
        def content = part.getDataHandler().getContent()
        if (content instanceof byte[]) {
            return new String(content as byte[], "UTF-8")
        }
        return content.getText("UTF-8")
    }
}