package life.qbic.authentication.application.communication;

import java.util.Optional;
import life.qbic.authentication.application.user.policy.EmailConfirmationLinkSupplier;
import life.qbic.authentication.application.user.policy.PasswordResetLinkSupplier;
import life.qbic.authentication.domain.user.concept.User;
import life.qbic.authentication.domain.user.concept.UserId;
import life.qbic.authentication.domain.user.repository.UserRepository;
import life.qbic.domain.concepts.communication.EmailService;
import life.qbic.domain.concepts.communication.Recipient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service that provides actions to get in contact with users.
 *
 * @since 1.0.0
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

  /**
   * Notifies the user with an action on how to reset the password.
   *
   * @param userId the user's id
   * @since 1.0.0
   */
  public void sendResetLink(String userId) {
    Optional<User> userSearchResult = userRepository.findById(UserId.from(userId));
    User user = userSearchResult.orElseThrow(() -> new RuntimeException(
        "Cannot send email confirmation. Unknown user with id " + userId));
    var passwordResetEmail = EmailFactory.passwordResetEmail(NO_REPLY_QBIC_LIFE,
        new Recipient(user.emailAddress().get(),
            user.fullName().get())
        , passwordResetLinkSupplier.passwordResetUrl(userId));

    emailService.send(passwordResetEmail);
  }

  /**
   * Notifies an inactive user with instructions to confirm the email address for the account.
   *
   * @param userId the user's id
   * @since 1.0.0
   */
  public void sendEmailConfirmation(String userId) {
    Optional<User> userSearchResult = userRepository.findById(UserId.from(userId));
    User user = userSearchResult.orElseThrow(() -> new RuntimeException(
        "Cannot send email confirmation. Unknown user with id " + userId));
    var emailAddressConfirmationEmail = EmailFactory.registrationEmail(NO_REPLY_QBIC_LIFE,
        new Recipient(user.emailAddress().get(), user.fullName().get()),
        emalConfirmationLinkSupplier.emailConfirmationUrl(userId));

    emailService.send(emailAddressConfirmationEmail);

  }
}
