package life.qbic.domain.usermanagement;

import java.util.Optional;
import life.qbic.domain.user.UserDomainService;

/**
 * <b>Domain Registry</b>
 *
 * <p>Provides access to registered domain services.
 *
 * @since 1.0.0
 */
public class DomainRegistry {

  private UserDomainService userDomainService;

  private static DomainRegistry INSTANCE;

  public static DomainRegistry instance() {
    if (INSTANCE == null) {
      INSTANCE = new DomainRegistry();
    }
    return INSTANCE;
  }

  /**
   * Registers a {@link UserDomainService} in the domain registry.
   *
   * <p>Successive calls will overwrite the previous registered service reference.
   *
   * @param aUserDomainService a user domain service
   * @since 1.0.0
   */
  public void registerService(UserDomainService aUserDomainService) {
    userDomainService = aUserDomainService;
  }

  /**
   * Queries for a registered {@link UserDomainService}.
   *
   * @return a registered service, or {@link Optional#empty()} if none is registered.
   * @since 1.0.0
   */
  public Optional<UserDomainService> userDomainService() {
    return Optional.ofNullable(userDomainService);
  }
}
