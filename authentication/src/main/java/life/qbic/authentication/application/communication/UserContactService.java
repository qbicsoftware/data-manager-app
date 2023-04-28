package life.qbic.authentication.application.communication;

import life.qbic.authentication.application.user.policy.EmailConfirmationLinkSupplier;
import life.qbic.authentication.application.user.policy.PasswordResetLinkSupplier;
import life.qbic.authentication.domain.user.concept.EmailAddress;
import life.qbic.authentication.domain.user.concept.FullName;
import life.qbic.authentication.domain.user.concept.User;
import life.qbic.authentication.domain.user.concept.UserId;
import life.qbic.authentication.domain.user.repository.UserRepository;
import life.qbic.domain.concepts.communication.EmailService;
import life.qbic.domain.concepts.communication.Recipient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

  private final UserRepository userRepository;
  private final EmailConfirmationLinkSupplier emalConfirmationLinkSupplier;

  public UserContactService(@Autowired EmailService emailService,
                            @Autowired PasswordResetLinkSupplier passwordResetLinkSupplier,
                            @Autowired EmailConfirmationLinkSupplier emailConfirmationLinkSupplier,
                            @Autowired UserRepository userRepository) {
    this.emailService = emailService;
    this.passwordResetLinkSupplier = passwordResetLinkSupplier;
    this.userRepository = userRepository;
    this.emalConfirmationLinkSupplier = emailConfirmationLinkSupplier;
  }

  public void sendResetLink(EmailAddress emailAddress, FullName fullName, UserId userId) {
    var passwordResetEmail = EmailFactory.passwordResetEmail(NO_REPLY_QBIC_LIFE,
        new Recipient(emailAddress.get(),
            fullName.get())
        , passwordResetLinkSupplier.passwordResetUrl(userId.get()));

    emailService.send(passwordResetEmail);
  }

  public void sendEmailConfirmation(String userId) {
    Optional<User> userSearchResult = userRepository.findById(UserId.from(userId));
    User user = userSearchResult.orElseThrow(() -> new RuntimeException("Cannot send email confirmation. Unknown user with id " + userId));
    var emailAddressConfirmationEmail = EmailFactory.registrationEmail(NO_REPLY_QBIC_LIFE,
        new Recipient(user.emailAddress().get(), user.fullName().get()),
        emalConfirmationLinkSupplier.emailConfirmationUrl(userId));

    emailService.send(emailAddressConfirmationEmail);

  }
}
