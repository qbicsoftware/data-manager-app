package life.qbic.controlling.application.authorization.authorities;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

/**
 * Provides granted authorities given a user
 */
@Service
public class QbicUserAuthorityService implements UserAuthorityProvider {

  private final UserRoleRepository userRoleRepository;

  public QbicUserAuthorityService(
      @Autowired UserRoleRepository userRoleRepository
  ) {
    this.userRoleRepository = userRoleRepository;
  }

  @Override
  public List<GrantedAuthority> getAuthoritiesByUserId(String userId) {
    List<UserRole> roles = userRoleRepository.findAllByUserId(userId);
    return roles.stream().flatMap(this::getAuthoritiesForRole).toList();
  }

  private Stream<GrantedAuthority> getAuthoritiesForRole(UserRole userRole) {
    List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
    Role role = userRole.role();
    grantedAuthorities.add(role);
    grantedAuthorities.addAll(role.permissions());
    return grantedAuthorities.stream();
  }
}
