package life.qbic.domain.usermanagement;

/**
 * <b><class short description - 1 Line!></b>
 *
 * <p><More detailed description - When to use, what it solves, etc.></p>
 *
 * @since <version tag>
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

  public void registerService(UserDomainService aUserDomainService) {
    userDomainService = aUserDomainService;
  }

  public UserDomainService userDomainService() {
    return userDomainService;
  }

}
