package life.qbic.usergroups.domain.registry;

import java.util.Optional;
import life.qbic.usergroups.domain.service.GroupDomainService;

/**
 * <b>Domain Registry</b>
 *
 * <p>Provides access to registered domain services of the user groups context. Mirrors the
 * identity context's {@code DomainRegistry}.</p>
 *
 * @since 1.0.0
 */
public class DomainRegistry {

  private GroupDomainService groupDomainService;

  private static DomainRegistry INSTANCE;

  private DomainRegistry() {
    // private constructor to enforce singleton usage
  }

  public static DomainRegistry instance() {
    if (INSTANCE == null) {
      INSTANCE = new DomainRegistry();
    }
    return INSTANCE;
  }

  /**
   * Registers a {@link GroupDomainService} in the domain registry.
   *
   * <p>Successive calls will overwrite the previously registered service reference.</p>
   *
   * @param aGroupDomainService a group domain service
   * @since 1.0.0
   */
  public void registerService(GroupDomainService aGroupDomainService) {
    groupDomainService = aGroupDomainService;
  }

  /**
   * Queries for a registered {@link GroupDomainService}.
   *
   * @return a registered service, or {@link Optional#empty()} if none is registered.
   * @since 1.0.0
   */
  public Optional<GroupDomainService> groupDomainService() {
    return Optional.ofNullable(groupDomainService);
  }
}