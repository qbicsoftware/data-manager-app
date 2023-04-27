package life.qbic.authentication.application.communication;

import life.qbic.authentication.application.user.policy.PasswordResetLinkSupplier;
import life.qbic.authentication.domain.user.concept.EmailAddress;
import life.qbic.authentication.domain.user.concept.FullName;
import life.qbic.authentication.domain.user.concept.UserId;
import life.qbic.domain.concepts.communication.EmailService;
import life.qbic.domain.concepts.communication.Recipient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
@Service
public class UserContactService {

    private static final String NO_REPLY_QBIC_LIFE = "no-reply@qbic.life";
    private final EmailService emailService;
    private final PasswordResetLinkSupplier passwordResetLinkSupplier;

    public UserContactService(@Autowired EmailService emailService,
                              @Autowired PasswordResetLinkSupplier passwordResetLinkSupplier) {
        this.emailService = emailService;
        this.passwordResetLinkSupplier = passwordResetLinkSupplier;
    }

    public void sendResetLink(EmailAddress emailAddress, FullName fullName, UserId userId) {
        try {
            var passwordResetEmail = EmailFactory.passwordResetEmail(NO_REPLY_QBIC_LIFE,
                    new Recipient(emailAddress.get(),
                            fullName.get())
                    , passwordResetLinkSupplier.passwordResetUrl(userId.get()));
            emailService.send(passwordResetEmail);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to send password reset email", e);
        }

    }
}
