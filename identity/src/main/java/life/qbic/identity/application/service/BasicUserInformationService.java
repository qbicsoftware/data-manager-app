package life.qbic.identity.application.service;

import static life.qbic.logging.service.LoggerFactory.logger;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import life.qbic.application.commons.OffsetBasedRequest;
import life.qbic.application.commons.SortOrder;
import life.qbic.identity.api.UserInfo;
import life.qbic.identity.api.UserInformationService;
import life.qbic.identity.domain.model.EmailAddress;
import life.qbic.identity.domain.model.EmailAddress.EmailValidationException;
import life.qbic.identity.domain.model.User;
import life.qbic.identity.domain.model.UserId;
import life.qbic.identity.domain.repository.UserRepository;
import life.qbic.logging.api.Logger;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

/**
 * <b>Basic user information service</b>
 *
 * <p>Implementation of the {@link UserInformationService}, provides a OHS via a Java interface to
 * query user information</p>
 *
 * @since 1.0.0
 */
public class BasicUserInformationService implements UserInformationService {

  private static final Logger log = logger(BasicUserInformationService.class);

  private final UserRepository userRepository;

  public BasicUserInformationService(UserRepository userRepository) {
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

  @Override
  public boolean userNameAvailable(String userName) {
    return userRepository.findByUserName(userName).isEmpty();
  }

  @Override
  public List<UserInfo> findAllActive(String filter, int offset, int limit,
      List<SortOrder> sortOrders) {
    List<Order> orders = sortOrders.stream().map(it -> {
      Order order;
      if (it.isDescending()) {
        order = Order.desc(it.propertyName());
      } else {
        order = Order.asc(it.propertyName());
      }
      return order;
    }).toList();
    return userRepository.findByUserNameContainingIgnoreCaseAndActiveTrue(
            filter, new OffsetBasedRequest(offset, limit, Sort.by(orders)))
        .stream()
        .map(user -> new UserInfo(user.id().get(), user.fullName().get(), user.emailAddress().get(),
            user.userName(), user.getEncryptedPassword().get(), user.isActive()))
        .toList();
  }

  private UserInfo convert(User user) {
    return new UserInfo(user.id().get(), user.fullName().get(), user.emailAddress().get(),
        user.userName(),
        user.getEncryptedPassword().get(),
        user.isActive());
  }
}
