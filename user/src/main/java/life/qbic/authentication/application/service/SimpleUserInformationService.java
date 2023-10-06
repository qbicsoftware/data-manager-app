package life.qbic.authentication.application.service;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.Objects;
import java.util.Optional;
import life.qbic.authentication.domain.user.concept.EmailAddress;
import life.qbic.authentication.domain.user.concept.EmailAddress.EmailValidationException;
import life.qbic.authentication.domain.user.concept.User;
import life.qbic.authentication.domain.user.concept.UserId;
import life.qbic.authentication.domain.user.repository.UserRepository;
import life.qbic.logging.api.Logger;
import life.qbic.user.api.UserInfo;
import life.qbic.user.api.UserInformationService;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
 */
public class SimpleUserInformationService implements UserInformationService {

  private static final Logger log = logger(SimpleUserInformationService.class);

  private final UserRepository userRepository;

  public SimpleUserInformationService(UserRepository userRepository) {
    this.userRepository = Objects.requireNonNull(userRepository);
  }

  @Override
  public Optional<UserInfo> findByEmail(String emailAddress) {
    try {
      var email = EmailAddress.from(emailAddress);
      return userRepository.findByEmail(email).map(this::convert);
    } catch (EmailValidationException e) {
      log.error("Client provided invalid email address %s".formatted(emailAddress), e);
      return Optional.empty();
    }
  }

  @Override
  public Optional<UserInfo> findById(String userId) {
    try {
      var id = UserId.from(userId);
      return userRepository.findById(id).map(this::convert);
    } catch (IllegalArgumentException e) {
      log.error("Invalid user id %s".formatted(userId), e);
      return Optional.empty();
    }
  }

  private UserInfo convert(User user) {
    return new UserInfo(user.id().get(), user.fullName().get(), user.emailAddress().get(),
        user.getEncryptedPassword().get(),
        user.isActive());
  }
}
