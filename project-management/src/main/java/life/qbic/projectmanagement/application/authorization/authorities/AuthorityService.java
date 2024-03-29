package life.qbic.projectmanagement.application.authorization.authorities;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import life.qbic.projectmanagement.application.authorization.SidDataStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Provides granted authorities given a user
 */
@Service
public class AuthorityService implements UserAuthorityProvider {

  private final UserRoleRepository userRoleRepository;

  private final SidDataStorage sidDataStorage;

  public AuthorityService(
      @Autowired UserRoleRepository userRoleRepository,
      @Autowired SidDataStorage sidDataStorage
  ) {
    this.userRoleRepository = userRoleRepository;
    this.sidDataStorage = sidDataStorage;
  }

  @Override
  public List<GrantedAuthority> getAuthoritiesByUserId(String userId) {
    List<UserRole> roles = userRoleRepository.findAllByUserId(userId);
    return roles.stream().flatMap(this::getAuthoritiesForRole).toList();
  }

  /**
   * Whenever a new user has been registered, a new authorization entry needs to be created for the
   * user identified by their unique user id.
   * <p>
   * This method is idempotent, meaning the entry is only created once per provided user id.
   *
   * @param userId the user's id, uniquely identifying them
   * @since 1.0.0
   */
  public void createNewAuthEntry(String userId) {
    sidDataStorage.addSid(userId, true);
  }

  private Stream<GrantedAuthority> getAuthoritiesForRole(UserRole userRole) {
    List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
    Role role = userRole.role();
    grantedAuthorities.add(role);
    grantedAuthorities.addAll(role.permissions());
    return grantedAuthorities.stream();
  }
}
